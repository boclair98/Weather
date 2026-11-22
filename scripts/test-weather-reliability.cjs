// Regression tests for the actual template functions; no network, browser, DB or email.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const root = path.join(__dirname, '..');
const html = fs.readFileSync(path.join(root, 'src/main/resources/templates/index.html'), 'utf8');
const workspace = fs.readFileSync(path.join(root, 'src/main/resources/static/weather-workspace.js'), 'utf8');
const names = [
    'readPreferences', 'savePreferences', 'toggleTemperatureUnit',
    'readLocalValue', 'writeLocalValue', 'readLocations', 'writeLocations',
    'migrateLegacyLocations', 'readFavoriteLocations', 'readSavedLocations',
    'rememberLocation', 'isSavedLocation', 'updateFavoriteButton', 'selectLocation',
    'searchLocations', 'useCurrentLocation',
    'snapshotLocationKey', 'saveWeatherSnapshot', 'readWeatherSnapshot'
];
const functions = names.map(name => {
    const match = html.match(new RegExp(`    (?:async )?function ${name}\\([^]*?(?=\\r?\\n    (?:async )?function )`));
    assert.ok(match, `Production function ${name} is present`);
    return match[0];
}).join('\n');

function element() {
    return {value: '', textContent: '', innerHTML: '', disabled: false, children: [],
        attributes: {}, classList: {toggle() {}},
        setAttribute(name, value) { this.attributes[name] = value; }};
}

function harness({blocked = false, now = '2026-10-25T01:00:00Z'} = {}) {
    const values = new Map(), nodes = new Map(), gps = [], requests = [];
    const calls = {loads: [], results: [], rendered: [], aborted: [], units: 0, rerenders: 0, searchControls: 0};
    let clock = Date.parse(now);
    class ClockDate extends Date {
        constructor(...args) { super(...(args.length ? args : [clock])); }
        static now() { return clock; }
    }
    const ctx = {
        Date: ClockDate, Intl, URLSearchParams, Map, Set,
        recentLocationsKey: 'recent', savedLocationsKey: 'saved', legacyLocationsKey: 'legacy',
        preferencesKey: 'preferences', lastLocationKey: 'last', snapshotKey: 'snapshot',
        temperatureUnit: 'C', themeMode: 'light', locationSearchVersion: 0, selectedLocation: null,
        requestControllers: new Map(),
        localStorage: {
            getItem(key) { if (blocked) throw new Error('SecurityError'); return values.get(key) ?? null; },
            setItem(key, value) { if (blocked) throw new Error('QuotaExceededError'); values.set(key, value); }
        },
        navigator: {geolocation: {getCurrentPosition(success, failure) { gps.push({success, failure}); }}},
        document: {getElementById(id) {
            if (!nodes.has(id)) nodes.set(id, element());
            return nodes.get(id);
        }},
        showResult(message, ok) { calls.results.push({message, ok}); },
        renderFavoriteLocations() {},
        renderLocationResults(locations) { calls.rendered.push(locations); },
        syncLocationSearchControls() { calls.searchControls += 1; },
        loadWeatherPreview(location) { calls.loads.push(location); },
        updateUnitToggle() { calls.units += 1; },
        rerenderWeatherData() { calls.rerenders += 1; },
        getNetworkError(error, fallback) { return error.message || fallback; },
        requestJson(url, options) {
            let resolve, reject;
            const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
            requests.push({url, key: options.key, resolve, reject});
            // Keep aborted requests resolvable: queued/cached responses can still finish.
            ctx.requestControllers.set(options.key, {abort() { calls.aborted.push(options.key); }});
            return promise;
        }
    };
    for (const name of ['locationQuery', 'locationResults', 'locationMeta', 'searchButton', 'currentLocationButton', 'favoriteLocationButton']) ctx[name] = element();
    vm.createContext(ctx);
    vm.runInContext(workspace + '\n' + functions, ctx);
    function take(key) {
        const index = requests.findIndex(request => request.key === key);
        assert.notEqual(index, -1, `Pending ${key} request exists`);
        return requests.splice(index, 1)[0];
    }
    return {ctx, values, calls, gps, requests, take, setNow(value) { clock = Date.parse(value); }};
}

const first = {locationName: '중앙역', regionName: '서울', nx: 60, ny: 127};
const second = {locationName: '중앙역', regionName: '부산', nx: 97, ny: 74};
const coords = {coords: {latitude: 37.5, longitude: 127}};
const plain = value => JSON.parse(JSON.stringify(value));
let cases = 0;
async function check(name, body) { await body(); cases += 1; console.log(`PASS ${name}`); }

async function run() {
    await check('blocked storage preserves initialization, preference reads/writes and unit rerender', () => {
        const h = harness({blocked: true});
        assert.doesNotThrow(() => h.ctx.migrateLegacyLocations());
        assert.equal(h.ctx.readPreferences().temperatureUnit, 'C');
        assert.doesNotThrow(() => h.ctx.savePreferences());
        assert.equal(h.ctx.readLocalValue('anything', 'fallback'), 'fallback');
        assert.equal(h.ctx.writeLocations('saved', [first]), false);
        assert.deepEqual(plain(h.ctx.readLocations('saved')), []);
        h.ctx.toggleTemperatureUnit();
        assert.equal(h.ctx.temperatureUnit, 'F');
        assert.equal(h.calls.units, 1);
        assert.equal(h.calls.rerenders, 1);
    });
    await check('a blocked recent-location write cannot prevent selecting and loading weather', () => {
        const h = harness({blocked: true});
        assert.doesNotThrow(() => h.ctx.selectLocation(first, -1));
        assert.equal(h.ctx.selectedLocation.locationName, first.locationName);
        assert.equal(h.calls.loads.length, 1);
        assert.equal(h.calls.loads[0].nx, first.nx);
        assert.equal(h.ctx.currentLocationButton.disabled, false);
    });
    await check('saved locations reject corrupt entries and preserve equal names in distinct grids', () => {
        const h = harness();
        h.values.set('saved', JSON.stringify([null, {}, first, first, second]));
        const found = h.ctx.readSavedLocations();
        assert.equal(found.length, 2);
        assert.equal(h.ctx.writeLocations('saved', found), true);
        assert.equal(JSON.parse(h.values.get('saved')).length, 2);
        h.values.set('legacy', h.values.get('saved'));
        h.ctx.migrateLegacyLocations();
        assert.equal(h.ctx.readFavoriteLocations().length, 2);
        h.values.set('saved', '{broken');
        assert.deepEqual(plain(h.ctx.readSavedLocations()), []);
        const before = h.calls.loads.length;
        h.ctx.selectLocation({locationName: '잘못된 위치', nx: 0, ny: 127}, -1);
        assert.equal(h.calls.loads.length, before);
    });
    await check('GPS callback arriving after manual selection makes no reverse-geocoding request', async () => {
        const h = harness();
        h.ctx.useCurrentLocation();
        h.ctx.selectLocation(second, -1);
        await h.gps[0].success(coords);
        assert.equal(h.requests.length, 0);
        assert.equal(h.ctx.selectedLocation.nx, second.nx);
        assert.equal(h.calls.loads.length, 1);
        assert.equal(h.calls.rendered.length, 0);
    });
    await check('reverse-geocoding success cannot replace a newer search or release its loading state', async () => {
        const h = harness();
        h.ctx.useCurrentLocation();
        const old = h.gps[0].success(coords);
        const coordinatesRequest = h.take('coordinates');
        h.ctx.locationQuery.value = '부산 중앙역';
        const search = h.ctx.searchLocations();
        coordinatesRequest.resolve(first);
        await old;
        assert.equal(h.ctx.locationQuery.value, '부산 중앙역');
        assert.equal(h.ctx.searchButton.disabled, true);
        assert.equal(h.calls.rendered.length, 0);
        assert.ok(h.calls.aborted.includes('coordinates'));
        h.take('locations').resolve([second]);
        await search;
        assert.equal(h.ctx.selectedLocation.nx, second.nx);
        assert.equal(h.calls.loads.length, 1);
    });
    await check('reverse-geocoding failure cannot overwrite a newer selected-location message', async () => {
        const h = harness();
        h.ctx.useCurrentLocation();
        const old = h.gps[0].success(coords);
        const request = h.take('coordinates');
        h.ctx.selectLocation(second, -1);
        const count = h.calls.results.length;
        request.reject(new Error('stale reverse-geocoding failure'));
        await old;
        assert.equal(h.calls.results.length, count);
        assert.equal(h.ctx.selectedLocation.nx, second.nx);
        assert.equal(h.ctx.currentLocationButton.disabled, false);
    });
    await check('late GPS permission errors do not override a newer GPS request', async () => {
        const h = harness();
        h.ctx.useCurrentLocation();
        h.ctx.useCurrentLocation();
        h.gps[0].failure({code: 1, PERMISSION_DENIED: 1});
        assert.equal(h.calls.results.length, 0);
        assert.equal(h.ctx.currentLocationButton.disabled, true);
        const latest = h.gps[1].success(coords);
        h.take('coordinates').resolve(second);
        await latest;
        assert.equal(h.ctx.selectedLocation.nx, second.nx);
        assert.equal(h.ctx.currentLocationButton.disabled, false);
        assert.equal(h.calls.loads.length, 1);
    });
    await check('snapshot save/read preserves same-name places in distinct grids', () => {
        const h = harness();
        h.ctx.saveWeatherSnapshot(first, 0, {forecastDate: '2026-10-25', id: 'seoul'});
        assert.equal(h.ctx.readWeatherSnapshot(first, 0).forecast.id, 'seoul');
        assert.equal(h.ctx.readWeatherSnapshot(second, 0), null);
        h.ctx.saveWeatherSnapshot(second, 0, {forecastDate: '20261025', id: 'busan'});
        assert.equal(h.ctx.readWeatherSnapshot(second, 0).forecast.id, 'busan');
        assert.equal(h.ctx.readWeatherSnapshot(first, 0).forecast.id, 'seoul');
    });
    await check('snapshot save/read expires after three hours including the exact valid boundary', () => {
        const h = harness({now: '2026-10-25T00:00:00Z'});
        h.ctx.saveWeatherSnapshot(first, 0, {forecastDate: '2026-10-25'});
        h.setNow('2026-10-25T03:00:00Z');
        assert.ok(h.ctx.readWeatherSnapshot(first, 0));
        h.setNow('2026-10-25T03:00:00.001Z');
        assert.equal(h.ctx.readWeatherSnapshot(first, 0), null);
    });
    await check('a snapshot from before Korean midnight is not displayed as the new today', () => {
        const h = harness({now: '2026-10-24T14:59:00Z'});
        h.ctx.saveWeatherSnapshot(first, 0, {forecastDate: '2026-10-24'});
        const old = h.ctx.readWeatherSnapshot(first, 0);
        assert.ok(old);
        h.setNow('2026-10-24T15:01:00Z');
        assert.equal(h.ctx.readWeatherSnapshot(first, 0), null);
        // Even a misplaced/migrated entry under today's key must fail the date check.
        h.values.set('snapshot', JSON.stringify({[h.ctx.snapshotLocationKey(first, 0)]: old}));
        assert.equal(h.ctx.readWeatherSnapshot(first, 0), null);
    });
    await check('snapshot corruption and blocked storage safely return no fallback', () => {
        const h = harness();
        h.values.set('snapshot', '{invalid');
        assert.equal(h.ctx.readWeatherSnapshot(first, 0), null);
        assert.doesNotThrow(() => h.ctx.saveWeatherSnapshot(first, 0, {forecastDate: '2026-10-25'}));
        const blocked = harness({blocked: true});
        assert.doesNotThrow(() => blocked.ctx.saveWeatherSnapshot(first, 0, {forecastDate: '2026-10-25'}));
        assert.equal(blocked.ctx.readWeatherSnapshot(first, 0), null);
    });
    await check('missing observation rainfall is not presented as dry weather', () => {
        const match = html.match(/    function observationPrecipitationLabel\([^]*?(?=\r?\n    (?:async )?function )/);
        const ctx = {WeatherLetter: require('../src/main/resources/static/weather-letter.js')};
        vm.runInNewContext(match[0], ctx);
        assert.equal(ctx.observationPrecipitationLabel({oneHourPrecipitation: '-', precipitationType: '없음'}), '1시간 강수량 정보 없음');
        assert.equal(ctx.observationPrecipitationLabel({oneHourPrecipitation: null, precipitationType: '비'}), '비 관측 · 강수량 정보 없음');
        assert.equal(ctx.observationPrecipitationLabel({oneHourPrecipitation: '0', precipitationType: '없음'}), '1시간 강수 없음');
        assert.equal(ctx.observationPrecipitationLabel({oneHourPrecipitation: '1.5', precipitationType: '비'}), '1시간 비 1.5mm');
    });
    await check('request memory and browser cache URLs turn over at Korean midnight', async () => {
        const h = harness({now: '2026-10-25T14:59:59Z'});
        const requested = [];
        Object.assign(h.ctx, {apiCache: new Map(), AbortController,
            cloneJson: plain, apiCacheTtl: () => 30000,
            fetch: async url => { requested.push(url); return {ok: true, json: async () => ({version: requested.length})}; }});
        const fn = html.match(/    async function requestJson\([^]*?(?=\r?\n    (?:async )?function )/)[0];
        vm.runInContext(fn, h.ctx);
        const url = '/api/weather/hourly?nx=60&ny=127&dayOffset=0';
        await h.ctx.requestJson(url);
        await h.ctx.requestJson(url);
        assert.equal(requested.length, 1);
        h.setNow('2026-10-25T15:00:00Z');
        await h.ctx.requestJson(url);
        assert.equal(requested.length, 2);
        assert.ok(requested[0].endsWith('forecastDate=2026-10-25'));
        assert.ok(requested[1].endsWith('forecastDate=2026-10-26'));
    });
    console.log(`Weather reliability: ${cases} regression cases passed`);
}
run().catch(error => { console.error(error); process.exitCode = 1; });
