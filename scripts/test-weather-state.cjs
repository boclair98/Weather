// Deterministic state tests for the actual browser loaders. No network or mail.
// Run from the repository root: node scripts/test-weather-state.cjs
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const WeatherLetter = require('../src/main/resources/static/weather-letter.js');

const html = fs.readFileSync(path.join(__dirname, '../src/main/resources/templates/index.html'), 'utf8');
const loaderNames = ['loadWeatherPreview', 'loadCurrentWeather', 'loadHourlyForecast', 'loadWeatherPlanner', 'loadWeatherDecision'];
const loaders = loaderNames.map((name) => {
    const match = html.match(new RegExp(`    async function ${name}\\([^]*?(?=\\r?\\n    (?:async )?function )`));
    assert.ok(match, `Missing production loader: ${name}`);
    return match[0];
}).join('\n');
const renderLetter = html.match(/    function renderWeatherLetter\([^]*?(?=\r?\n    (?:async )?function )/);
assert.ok(renderLetter, 'Missing production weather-letter renderer');
const advancedToggle = html.match(/    advancedWeatherTools\?\.addEventListener\("toggle", \(\) => \{[^]*?\r?\n    \}\);/);
assert.ok(advancedToggle, 'Missing production advanced-tools toggle listener');

function element() {
    return {
        hidden: false, disabled: false, textContent: '', innerHTML: '', value: 'NONE',
        dataset: {}, attributes: {}, open: false, listeners: new Map(),
        classList: {add() {}, remove() {}},
        setAttribute(name, value) { this.attributes[name] = value; },
        getAttribute(name) { return this.attributes[name] ?? null; },
        removeAttribute(name) { delete this.attributes[name]; },
        addEventListener(name, handler) { this.listeners.set(name, handler); },
        dispatch(name) { this.listeners.get(name)?.(); },
        replaceChildren() { this.innerHTML = ''; },
        close() { this.open = false; }
    };
}

function harness() {
    const nodes = new Map();
    const pending = [];
    const rendered = {daily: [], current: [], hourly: [], planner: [], decision: []};
    const aborted = [];
    const ctx = {
        URLSearchParams, Date, Map, WeatherLetter,
        selectedLocation: null, forecastDayOffset: 0, weatherLoadVersion: 0,
        dailyForecast: null, currentObservation: null, weatherPlannerData: null,
        weatherDecisionData: null, goOutWindowData: null, hourlyForecastHours: [], hourlyForecastExpanded: false,
        requestControllers: new Map(),
        advancedWeatherTools: element(),
        document: {getElementById(id) {
            if (!nodes.has(id)) nodes.set(id, element());
            return nodes.get(id);
        }},
        locationContext: (location) => location.locationName,
        hideSnapshotNote() {}, showSnapshotNote() {}, updateDataFreshness() {},
        saveWeatherSnapshot() {}, readWeatherSnapshot: () => null,
        setWeatherIcon() {}, updateFavoriteButton() {}, showResult() {},
        formatIssuedAt: (value) => value || '시각 없음',
        formatTemperature: (value) => `${value}°C`,
        getNetworkError: (error, fallback) => error.message || fallback,
        requestJson(url, options) {
            const params = new URL(url, 'http://localhost').searchParams;
            let resolve, reject;
            const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
            const request = {key: options.key, location: params.get('locationName'), resolve, reject};
            pending.push(request);
            ctx.requestControllers.set(options.key, {abort() { aborted.push(options.key); }});
            // Deliberately allow an aborted request to finish: a cached/queued
            // response can win the race with cancellation in real browsers.
            return promise;
        },
        renderDailyInsight() { rendered.daily.push(ctx.dailyForecast); ctx.renderWeatherLetter(); },
        renderSelectedPeriod() {
            if (!ctx.currentObservation) ctx.heroWeatherTemperature.textContent = ctx.dailyForecast.morning.tmp;
        },
        renderCurrentObservation(data) {
            rendered.current.push(data);
            ctx.heroWeatherCard.dataset.state = 'ready';
            ctx.heroWeatherTemperature.textContent = `${data.temperature}°`;
        },
        renderHourlyForecast(data) {
            rendered.hourly.push(data);
            ctx.hourlyForecastHours = data.hours || [];
            ctx.renderWeatherLetter();
        },
        renderWeatherPlanner(data) { rendered.planner.push(data); },
        renderWeatherDecision(data) { rendered.decision.push(data); ctx.decisionResult.hidden = false; }
    };
    const nodeNames = `mainContent locationSearchDialog weatherPreview refreshWeatherButton weatherPreviewTitle
        weatherPreviewMain heroWeatherCard heroWeatherLocation heroWeatherStatus heroWeatherTemperature
        heroWeatherCondition heroWeatherFeelsLike heroWeatherPrecipitation heroWeatherWind heroWeatherObservedAt
        heroWeatherIcon heroWeatherAction heroWeatherActionLabel weatherDetail outingScore outingScoreLabel
        weatherDecisionHeadline weatherDecisionDetail personalizationSummary decisionExplanation riskFactorList
        weatherAir weatherAirStation warningBanner uvDisplay uvAdvice pollenDisplay pollenAdvice safetySummary
        styleHeadline styleSummary styleTop styleBottom styleFootwear styleOuter styleColor styleCaution
        actionUmbrella actionMask actionOutdoor dailyInsight goOutWindow goOutWindowTrack dailyPeriods
        dailyPeriodsHeadingWrap hourlyWeather hourlyHeadline hourlyRainSummary hourlyTrack toggleHourlyForecastButton
        hourlySource hourlyError decisionResult decisionError weatherPlanner decisionWindow plannerDays
        addPlannerCalendar addDecisionCalendar plannerHeadline plannerRecommendation plannerPacking plannerQuality
        plannerFreshness plannerError loadDecisionWindowButton decisionDate decisionTime decisionFlex decisionDuration`.trim().split(/\s+/);
    for (const name of nodeNames) ctx[name] = element();
    vm.createContext(ctx);
    vm.runInContext(loaders, ctx);
    vm.runInContext(renderLetter[0], ctx);
    vm.runInContext(advancedToggle[0], ctx);
    function take(key, location) {
        const index = pending.findIndex((item) => item.key === key && item.location === location);
        assert.notEqual(index, -1, `No pending ${key} request for ${location}`);
        return pending.splice(index, 1)[0];
    }
    return {ctx, take, pending, rendered, aborted, nodes};
}

const first = {locationName: '서울 중구', nx: 60, ny: 127};
const second = {locationName: '부산 중구', nx: 97, ny: 74};
const forecast = (id) => ({id, morning: {tmp: '17', dataFetchedAt: '2026-10-23T09:00:00'}});
const flush = () => new Promise(setImmediate);

async function check(name, body) {
    await body();
    console.log(`PASS ${name}`);
}

async function run() {
    await check('late daily/current responses never replace the selected location state', async () => {
        const h = harness();
        h.ctx.selectedLocation = first;
        const oldLoad = h.ctx.loadWeatherPreview(first);
        h.ctx.selectedLocation = second;
        const newLoad = h.ctx.loadWeatherPreview(second);
        const current = {id: 'second-current', temperature: 22};
        h.take('current', second.locationName).resolve(current);
        const latest = forecast('second-daily');
        h.take('daily', second.locationName).resolve(latest);
        await newLoad;
        h.take('daily', first.locationName).resolve(forecast('old-daily'));
        h.take('current', first.locationName).resolve({id: 'old-current', temperature: -9});
        await oldLoad;
        await flush();
        assert.equal(h.ctx.dailyForecast, latest);
        assert.equal(h.ctx.currentObservation, current);
        assert.equal(h.ctx.heroWeatherTemperature.textContent, '22°');
        assert.deepEqual(h.rendered.daily.map((item) => item.id), ['second-daily']);
        assert.deepEqual(h.rendered.current.map((item) => item.id), ['second-current']);
        assert.ok(h.aborted.includes('daily'));
        assert.ok(h.aborted.includes('current'));
        assert.ok(h.aborted.includes('hourly'));
    });

    await check('old hourly failures and finally cannot end the new loading state', async () => {
        const h = harness();
        h.ctx.selectedLocation = first;
        h.ctx.loadWeatherPreview(first);
        h.ctx.selectedLocation = second;
        h.ctx.loadWeatherPreview(second);
        h.take('hourly', first.locationName).reject(new Error('Old request failed'));
        await flush();
        assert.equal(h.ctx.hourlyWeather.attributes['aria-busy'], 'true');
        assert.equal(h.ctx.hourlyError.hidden, true);
        h.take('hourly', second.locationName).resolve({id: 'second-hourly'});
        await flush();
        assert.equal(h.ctx.hourlyWeather.attributes['aria-busy'], 'false');
        assert.deepEqual(h.rendered.hourly.map((item) => item.id), ['second-hourly']);
    });

    await check('changing location clears planner state and ignores a late planner response', async () => {
        const h = harness();
        h.ctx.advancedWeatherTools.open = true;
        h.ctx.selectedLocation = first;
        const oldLoad = h.ctx.loadWeatherPreview(first);
        h.take('daily', first.locationName).resolve(forecast('first-daily'));
        await oldLoad;
        h.ctx.weatherPlannerData = {id: 'first-planner-cached'};
        h.ctx.selectedLocation = second;
        const newLoad = h.ctx.loadWeatherPreview(second);
        assert.equal(h.ctx.weatherPlannerData, null);
        assert.equal(h.ctx.weatherPlanner.hidden, true);
        assert.equal(h.ctx.addPlannerCalendar.disabled, true);
        assert.ok(h.aborted.includes('planner'));
        h.take('daily', second.locationName).resolve(forecast('second-daily'));
        await newLoad;
        h.take('planner', first.locationName).resolve({id: 'old-planner'});
        await flush();
        assert.equal(h.ctx.weatherPlanner.attributes['aria-busy'], 'true');
        assert.equal(h.ctx.weatherPlannerData, null);
        assert.deepEqual(h.rendered.planner, []);
        h.take('planner', second.locationName).resolve({id: 'new-planner'});
        await flush();
        assert.equal(h.ctx.weatherPlannerData.id, 'new-planner');
        assert.equal(h.ctx.weatherPlanner.attributes['aria-busy'], 'false');
    });

    await check('closed advanced tools make no planner request until expanded', async () => {
        const h = harness();
        h.ctx.selectedLocation = first;
        const loading = h.ctx.loadWeatherPreview(first);
        h.take('daily', first.locationName).resolve(forecast('daily'));
        await loading;
        assert.equal(h.pending.some((request) => request.key === 'planner'), false);
        assert.equal(h.ctx.weatherPlanner.hidden, true);
        h.ctx.advancedWeatherTools.open = true;
        h.ctx.advancedWeatherTools.dispatch('toggle');
        const planner = h.take('planner', first.locationName);
        h.ctx.advancedWeatherTools.dispatch('toggle');
        assert.equal(h.pending.some((request) => request.key === 'planner'), false, 'No duplicate request while loading');
        planner.resolve({id: 'lazy-planner'});
        await flush();
        assert.equal(h.ctx.weatherPlannerData.id, 'lazy-planner');
        h.ctx.advancedWeatherTools.dispatch('toggle');
        assert.equal(h.pending.some((request) => request.key === 'planner'), false, 'Reuse the current location planner');
    });

    await check('a late decision response cannot replace the current place or release its controls', async () => {
        const h = harness();
        h.ctx.selectedLocation = first;
        const oldDecision = h.ctx.loadWeatherDecision();
        h.ctx.selectedLocation = second;
        h.ctx.weatherLoadVersion += 1;
        const newDecision = h.ctx.loadWeatherDecision();
        h.take('decision', first.locationName).resolve({id: 'old-decision'});
        await oldDecision;
        assert.equal(h.ctx.weatherDecisionData, null);
        assert.equal(h.ctx.decisionWindow.attributes['aria-busy'], 'true');
        assert.equal(h.ctx.loadDecisionWindowButton.disabled, true);
        h.take('decision', second.locationName).resolve({id: 'new-decision'});
        await newDecision;
        assert.equal(h.ctx.weatherDecisionData.id, 'new-decision');
        assert.equal(h.ctx.loadDecisionWindowButton.disabled, false);
        assert.deepEqual(h.rendered.decision.map((item) => item.id), ['new-decision']);
    });

    await check('a late decision error cannot erase a newer result for the same place', async () => {
        const h = harness();
        h.ctx.selectedLocation = first;
        const oldDecision = h.ctx.loadWeatherDecision();
        const oldRequest = h.take('decision', first.locationName);
        const newDecision = h.ctx.loadWeatherDecision();
        h.take('decision', first.locationName).resolve({id: 'newer-decision'});
        await newDecision;
        oldRequest.reject(new Error('Older request failed'));
        await oldDecision;
        assert.equal(h.ctx.weatherDecisionData.id, 'newer-decision');
        assert.equal(h.ctx.decisionError.hidden, true);
        assert.equal(h.ctx.decisionResult.hidden, false);
    });

    await check('daily failure preserves successful observations and independent hourly data', async () => {
        const h = harness();
        h.ctx.selectedLocation = first;
        const loading = h.ctx.loadWeatherPreview(first);
        h.take('current', first.locationName).resolve({temperature: 19});
        h.take('hourly', first.locationName).resolve({id: 'independent-hourly'});
        await flush();
        h.take('daily', first.locationName).reject(new Error('일별 예보 연결 실패'));
        await loading;
        assert.equal(h.ctx.heroWeatherTemperature.textContent, '19°');
        assert.equal(h.ctx.heroWeatherCard.dataset.state, 'ready');
        assert.equal(h.ctx.dailyForecast, null);
        assert.equal(h.ctx.weatherAir.textContent, '측정 자료 없음');
        assert.deepEqual(h.rendered.hourly.map((item) => item.id), ['independent-hourly']);
        assert.equal(h.ctx.refreshWeatherButton.disabled, false);
    });

    await check('new weather letter values are cleared immediately on location change', async () => {
        const h = harness();
        for (const id of ['weatherLetterHeadline', 'weatherLetterRange', 'weatherLetterRain', 'weatherLetterAir']) {
            h.ctx.document.getElementById(id).textContent = '이전 지역의 자료';
        }
        h.ctx.selectedLocation = second;
        h.ctx.loadWeatherPreview(second);
        assert.equal(h.nodes.get('weatherLetterHeadline').textContent, '예보를 확인하고 있어요');
        for (const id of ['weatherLetterRange', 'weatherLetterRain', 'weatherLetterAir']) {
            assert.equal(h.nodes.get(id).textContent, '확인 중');
        }
    });

    await check('both daily and hourly failures replace loading copy regardless of response order', async () => {
        for (const firstFailure of ['daily', 'hourly']) {
            const h = harness();
            h.ctx.selectedLocation = first;
            const loading = h.ctx.loadWeatherPreview(first);
            const daily = h.take('daily', first.locationName);
            const hourly = h.take('hourly', first.locationName);
            if (firstFailure === 'daily') {
                daily.reject(new Error('Daily unavailable'));
                await loading;
                hourly.reject(new Error('Hourly unavailable'));
            } else {
                hourly.reject(new Error('Hourly unavailable'));
                await flush();
                daily.reject(new Error('Daily unavailable'));
                await loading;
            }
            await flush();
            assert.equal(h.nodes.get('weatherLetterHeadline').textContent, '예보 자료를 불러오지 못했어요');
            assert.equal(h.nodes.get('weatherLetterRange').textContent, '기온 자료 없음');
            assert.equal(h.nodes.get('weatherLetterRain').textContent, '강수 자료 없음');
            assert.equal(h.nodes.get('weatherLetterAir').textContent, '측정 자료 없음');
        }
    });

    await check('hourly failure preserves a successful daily briefing', async () => {
        const h = harness();
        h.ctx.selectedLocation = first;
        const loading = h.ctx.loadWeatherPreview(first);
        const daily = {morning: {tmp: '17', pop: '30', airQualitySummary: '보통'}};
        h.take('daily', first.locationName).resolve(daily);
        await loading;
        h.take('hourly', first.locationName).reject(new Error('Hourly unavailable'));
        await flush();
        assert.equal(h.ctx.dailyForecast, daily);
        assert.match(h.nodes.get('weatherLetterRange').textContent, /17°C/);
        assert.equal(h.nodes.get('weatherLetterRain').textContent, '최대 30%');
        assert.equal(h.nodes.get('weatherLetterAir').textContent, '보통');
    });

    await check('daily failure preserves a successful hourly briefing', async () => {
        const h = harness();
        h.ctx.selectedLocation = first;
        const loading = h.ctx.loadWeatherPreview(first);
        h.take('hourly', first.locationName).resolve({hours: [{temperature: 21, precipitationProbability: 70, time: '18:00'}]});
        await flush();
        h.take('daily', first.locationName).reject(new Error('Daily unavailable'));
        await loading;
        assert.equal(h.ctx.hourlyForecastHours.length, 1);
        assert.match(h.nodes.get('weatherLetterRange').textContent, /21°C/);
        assert.equal(h.nodes.get('weatherLetterRain').textContent, '최대 70%');
        assert.match(h.nodes.get('weatherLetterHeadline').textContent, /18:00/);
    });
}

run().catch((error) => { console.error(error); process.exitCode = 1; });
