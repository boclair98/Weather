const assert = require('node:assert/strict');
const {layout} = require('../src/main/resources/static/weather-chart.js');
const hour = (time, temperature) => ({time, temperature});

const complete = layout([hour('08:00', 0), hour('09:00', -3), hour('10:00', 8)]);
assert.deepEqual(complete.points.map(p => p.value), [0, -3, 8]);
assert.equal(complete.paths.length, 1);
assert.ok(complete.points[1].y > complete.points[0].y && complete.points[0].y > complete.points[2].y);
const missing = layout([hour('08:00', 10), hour('09:00', null), hour('10:00', 12)]);
assert.equal(missing.paths.length, 2);
assert.deepEqual(missing.points.map(p => p.x), [50, 250]);
assert.equal(layout([hour('08:00', 10), hour('10:00', 12)]).paths.length, 2);
assert.equal(layout([hour('08:00', 10), hour('08:00', 12)]).paths.length, 2);
assert.equal(layout([hour('08:00', ''), hour('09:00', '-'), hour('10:00', Infinity)]).points.length, 0);
assert.equal(layout([hour('24:00', 10), hour('no-time', 12)]).points.length, 0);
assert.ok(layout([hour('08:00', 10)]).points.every(p => Number.isFinite(p.y)));
assert.ok(layout([hour('08:00', 10), hour('09:00', 10)]).points.every(p => p.y === 35));
assert.equal(layout([]).paths.length, 0);
// A unit change must also rerender hourly-only partial success, not just daily weather.
const fs = require('node:fs');
const vm = require('node:vm');
const html = fs.readFileSync(require('node:path').join(__dirname, '../src/main/resources/templates/index.html'), 'utf8');
const rerender = html.match(/    function rerenderWeatherData\([^]*?(?=\r?\n    function )/)[0];
const calls = [];
const context = {selectedLocation: {}, dailyForecast: null, currentObservation: null,
    hourlyForecastHours: [hour('09:00', 15)], weatherPlannerData: null, weatherDecisionData: null,
    briefingPreviewDialog: {open: false}, renderWeatherLetter() { calls.push('letter'); },
    renderHourlyForecastItems() { calls.push('hours'); }};
vm.runInNewContext(rerender + '\nrerenderWeatherData();', context);
assert.deepEqual(calls, ['letter', 'hours']);
const kinds = ['weatherIconKind', 'hourlyWeatherIconKind'].map(name => html.match(new RegExp(`    function ${name}\\([^]*?(?=\\r?\\n    function )`))[0]).join('\n');
const icons = {};
vm.runInNewContext(kinds, icons);
assert.equal(icons.hourlyWeatherIconKind({time: '00:00', sky: '맑음'}), 'clear-night');
assert.equal(icons.hourlyWeatherIconKind({time: '23:00', sky: '구름많음'}), 'partly-cloudy-night');
assert.equal(icons.hourlyWeatherIconKind({time: '12:00', sky: '맑음'}), 'sunny');
assert.equal(icons.hourlyWeatherIconKind({time: '00:00', sky: '구름많음', precipitation: '비'}), 'rain');
assert.equal(icons.hourlyWeatherIconKind({time: '00:00'}), 'unknown');
assert.equal(icons.hourlyWeatherIconKind({time: '29:00', sky: '맑음'}), 'sunny');
console.log('Weather chart: 19 assertions passed (gaps, invalid values, real zero, flat ranges, unit refresh and night illustrations)');
