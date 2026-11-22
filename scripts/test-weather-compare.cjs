// Executes the production comparison controller against a tiny DOM and deferred requests.
// No dependency, browser, account, persistence or network is used.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const Workspace = require('../src/main/resources/static/weather-workspace.js');
const source = fs.readFileSync(path.join(__dirname, '../src/main/resources/static/weather-compare.js'), 'utf8');
const fixedNow = new Date('2026-10-25T00:30:00Z');
class FixedDate extends Date {
    constructor(...args) { super(...(args.length ? args : [fixedNow.getTime()])); }
    static now() { return fixedNow.getTime(); }
}

class Element {
    constructor(tag = 'div') {
        this.tagName = tag.toLowerCase();
        this.children = [];
        this.listeners = new Map();
        this.attributes = new Map();
        this._value = '';
        this._text = '';
        this.disabled = false;
        this.checked = false;
        this.open = false;
    }
    get value() { return this._value; }
    set value(value) {
        const text = String(value);
        this._value = this.tagName === 'select' && !this.options.some(option => option.value === text) ? '' : text;
    }
    get options() { return this.children.filter(node => node.tagName === 'option'); }
    set textContent(value) { this._text = String(value); this.children = []; }
    get textContent() { return this._text + this.children.map(node => node.textContent).join(' '); }
    appendChild(node) {
        this.children.push(node);
        if (this.tagName === 'select' && this.options.length === 1) this._value = node.value;
        return node;
    }
    append(...nodes) { nodes.forEach(node => this.appendChild(node)); }
    replaceChildren(...nodes) {
        this.children = [];
        this._text = '';
        if (this.tagName === 'select') this._value = '';
        this.append(...nodes);
    }
    querySelectorAll(selector) {
        assert.equal(selector, 'input:checked', 'Only a production-used selector is implemented');
        return descendants(this, node => node.tagName === 'input' && node.checked);
    }
    setAttribute(name, value) { this.attributes.set(name, String(value)); }
    removeAttribute(name) { this.attributes.delete(name); }
    getAttribute(name) { return this.attributes.get(name) ?? null; }
    addEventListener(name, fn) {
        if (!this.listeners.has(name)) this.listeners.set(name, []);
        this.listeners.get(name).push(fn);
    }
    dispatch(name, target = this) {
        const event = {target, defaultPrevented: false, preventDefault() { this.defaultPrevented = true; }};
        return Promise.all((this.listeners.get(name) || []).map(listener => listener(event)));
    }
    showModal() { this.open = true; }
    close() { this.open = false; return this.dispatch('close'); }
}
function descendants(node, predicate) {
    return node.children.flatMap(child => [...(predicate(child) ? [child] : []), ...descendants(child, predicate)]);
}
const places = ['집 근처', '회사 근처', '약속 장소', '여행지'].map((locationName, index) => ({locationName, nx: 60 + index, ny: 127}));
function harness(placeList = places, initialDay = 1) {
    const nodes = new Map();
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, new Element(id === 'compareDay' || id === 'compareTime' ? 'select' : 'div'));
        return nodes.get(id);
    };
    for (const day of ['0', '1', '2']) {
        const option = new Element('option'); option.value = day; node('compareDay').appendChild(option);
    }
    const requests = [], cancelled = [], selected = [];
    let active = 0, peak = 0, searches = 0;
    const context = {URLSearchParams, Intl, Date: FixedDate,
        WeatherWorkspace: {...Workspace, koreaDate: offset => Workspace.koreaDate(offset, fixedNow)}};
    vm.runInNewContext(source, context, {filename: 'weather-compare.js'});
    const controller = context.WeatherCompare.init({
        document: {getElementById: node, createElement: tag => new Element(tag)},
        locations: () => placeList, day: () => initialDay,
        cancel: key => cancelled.push(key),
        request(url, options) {
            let resolve, reject;
            const promise = new Promise((yes, no) => {resolve = yes; reject = no;});
            active += 1; peak = Math.max(peak, active);
            requests.push({url, key: options.key, resolve, reject});
            // cancel is intentionally not connected to rejection. Even a queued/cache
            // response that finishes after AbortController.abort must be ignored.
            return promise.finally(() => {active -= 1;});
        },
        temperature: value => value === null || value === undefined ? '자료 없음' : `${value}°C`,
        icon: (icon, hour) => { icon.textContent = hour.sky || '상태 정보 없음'; },
        issued: value => value || '자료 없음',
        select: (place, day) => selected.push({place, day}), search: () => {searches += 1;}
    });
    const inputs = () => descendants(node('comparePlaceChoices'), item => item.tagName === 'input');
    const payload = (overrides = {}) => ({
        forecastDate: Workspace.koreaDate(Number(node('compareDay').value), fixedNow),
        hours: [{time: node('compareTime').value, temperature: 13, sky: '흐림', precipitation: '없음',
            precipitationProbability: 20, humidity: 70, windSpeed: 2}],
        provenance: {forecastIssuedAt: '2026-10-25T02:00:00+09:00'}, ...overrides
    });
    return {controller, node, inputs, requests, cancelled, selected, payload,
        active: () => active, peak: () => peak, searches: () => searches,
        submit: () => node('weatherCompareForm').dispatch('submit')};
}
let cases = 0;
async function check(name, body) { await body(); cases += 1; console.log(`PASS ${name}`); }

async function run() {
    await check('opening comparison performs no request and enforces a three-place selection cap', async () => {
        const h = harness(); h.controller.open();
        assert.equal(h.requests.length, 0);
        assert.equal(h.inputs().filter(input => input.checked).length, 3);
        h.inputs()[3].checked = true;
        await h.node('comparePlaceChoices').dispatch('change', h.inputs()[3]);
        assert.equal(h.inputs()[3].checked, false);
        assert.match(h.node('compareStatus').textContent, /3곳까지/);
    });
    await check('fewer than two places cannot start requests', async () => {
        const h = harness([places[0]]); h.controller.open();
        assert.equal(h.node('runWeatherCompare').disabled, true);
        await h.submit();
        assert.equal(h.requests.length, 0);
        assert.match(h.node('compareStatus').textContent, /2곳 이상/);
    });
    await check('submit starts at most three keyed requests and renders three exact-time results', async () => {
        const h = harness(); h.controller.open();
        const pending = h.submit();
        assert.equal(h.requests.length, 3);
        assert.equal(h.peak(), 3);
        assert.deepEqual(h.requests.map(request => request.key), ['compare-0', 'compare-1', 'compare-2']);
        assert.equal(h.node('compareResults').getAttribute('aria-busy'), 'true');
        for (const request of h.requests) {
            const query = new URL(request.url, 'http://localhost').searchParams;
            assert.equal(query.get('dayOffset'), '1');
            request.resolve(h.payload());
        }
        await pending;
        assert.equal(h.active(), 0);
        assert.equal(h.node('compareResults').children.length, 3);
        assert.match(h.node('compareStatus').textContent, /3곳 중 3곳/);
        assert.equal(h.node('compareResults').getAttribute('aria-busy'), null);
        assert.equal(h.node('runWeatherCompare').disabled, false);
    });
    await check('one failed region does not erase the other two successful results', async () => {
        const h = harness(); h.controller.open();
        const pending = h.submit();
        h.requests[0].resolve(h.payload());
        h.requests[1].reject(new Error('upstream unavailable'));
        h.requests[2].resolve(h.payload());
        await pending;
        assert.match(h.node('compareResults').children[0].textContent, /13°C/);
        assert.match(h.node('compareResults').children[1].textContent, /불러오지 못했어요/);
        assert.match(h.node('compareResults').children[2].textContent, /13°C/);
        assert.match(h.node('compareStatus').textContent, /3곳 중 2곳/);
    });
    for (const operation of ['close', 'date', 'time', 'selection']) {
        await check(`${operation} change cancels requests and ignores their late success and failure`, async () => {
            const h = harness(); h.controller.open();
            const pending = h.submit();
            const oldPayload = h.payload();
            const cancelCount = h.cancelled.length;
            if (operation === 'close') await h.node('weatherCompareDialog').close();
            if (operation === 'date') { h.node('compareDay').value = '2'; await h.node('compareDay').dispatch('change'); }
            if (operation === 'time') { h.node('compareTime').value = '10:00'; await h.node('compareTime').dispatch('change'); }
            if (operation === 'selection') { h.inputs()[0].checked = false; await h.node('comparePlaceChoices').dispatch('change', h.inputs()[0]); }
            assert.deepEqual(h.cancelled.slice(cancelCount), ['compare-0', 'compare-1', 'compare-2']);
            const expected = {text: h.node('compareResults').textContent, status: h.node('compareStatus').textContent};
            h.requests[0].resolve(oldPayload);
            h.requests[1].reject(new Error('late error'));
            h.requests[2].resolve(oldPayload);
            await pending;
            assert.equal(h.node('compareResults').textContent, expected.text);
            assert.equal(h.node('compareStatus').textContent, expected.status);
            assert.equal(h.node('compareResults').getAttribute('aria-busy'), null);
        });
    }
    await check('a closed and reopened dialog cannot be overwritten by an older comparison', async () => {
        const h = harness(); h.controller.open();
        const oldRun = h.submit(), oldPayload = h.payload();
        await h.node('weatherCompareDialog').close(); h.controller.open();
        const newRun = h.submit();
        for (const request of h.requests.slice(3)) request.resolve(h.payload({hours: [{time: '09:00', temperature: 22, sky: '맑음'}]}));
        await newRun;
        const expected = h.node('compareResults').textContent;
        for (const request of h.requests.slice(0, 3)) request.resolve(oldPayload);
        await oldRun;
        assert.equal(h.node('compareResults').textContent, expected);
        assert.match(expected, /22°C/);
        assert.doesNotMatch(expected, /13°C/);
    });
    await check('missing exact times and wrong dates never fall back to adjacent forecasts', async () => {
        const h = harness(); h.controller.open();
        const pending = h.submit();
        h.requests[0].resolve(h.payload({hours: [{time: '08:00', temperature: 88}, {time: '10:00', temperature: 99}]}));
        h.requests[1].resolve(h.payload({forecastDate: '2026-10-25'}));
        h.requests[2].resolve(h.payload({hours: [{time: '09:00', temperature: 0, sky: '맑음', precipitationProbability: 0}]}));
        await pending;
        const cards = h.node('compareResults').children;
        assert.match(cards[0].textContent, /선택한 날짜·시간의 예보가 없어요/);
        assert.match(cards[1].textContent, /선택한 날짜·시간의 예보가 없어요/);
        assert.doesNotMatch(cards[0].textContent, /88°C|99°C/);
        assert.match(cards[2].textContent, /0°C/);
        assert.match(h.node('compareStatus').textContent, /3곳 중 1곳/);
    });
    await check('invalid and missing measurements are not displayed as favorable zeros', async () => {
        const h = harness(places.slice(0, 2)); h.controller.open();
        const pending = h.submit();
        const payload = h.payload({hours: [{time: '09:00', temperature: null, precipitationProbability: null, humidity: 101, windSpeed: -1}]});
        h.requests.forEach(request => request.resolve(payload)); await pending;
        const card = h.node('compareResults').children[0];
        assert.equal(descendants(card, node => node.tagName === 'dd').every(node => node.textContent === '자료 없음'), true);
        assert.doesNotMatch(card.textContent, /0%|0m\/s/);
    });
    await check('view-details closes the dialog and selects the requested region and day', async () => {
        const h = harness(places.slice(0, 2), 2); h.controller.open();
        const pending = h.submit(); h.requests.forEach(request => request.resolve(h.payload())); await pending;
        const view = descendants(h.node('compareResults').children[1], node => node.className === 'compare-view')[0];
        await view.dispatch('click');
        assert.equal(h.node('weatherCompareDialog').open, false);
        assert.equal(h.selected.length, 1);
        assert.equal(h.selected[0].place.locationName, places[1].locationName);
        assert.equal(h.selected[0].day, 2);
    });
    await check('the close and find-place controls use the same cancellation path', async () => {
        const h = harness(); h.controller.open();
        await h.node('closeWeatherCompare').dispatch('click');
        assert.equal(h.node('weatherCompareDialog').open, false);
        h.controller.open();
        await h.node('compareFindPlace').dispatch('click');
        assert.equal(h.node('weatherCompareDialog').open, false);
        assert.equal(h.searches(), 1);
    });
    console.log(`Weather comparison: ${cases} regression cases passed`);
}
run().catch(error => { console.error(error); process.exitCode = 1; });
