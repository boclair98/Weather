// Pure, deterministic tests. No network, persistence or email.
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const path = require('node:path');
const Workspace = require('../src/main/resources/static/weather-workspace.js');
let cases = 0;
function check(name, body) { body(); cases += 1; console.log(`PASS ${name}`); }
const seoul = {locationName: '서울 중구', regionName: '서울', nx: 60, ny: 127, latitude: 37.56, longitude: 126.99};
const busan = {...seoul, locationName: '부산 중구', nx: 97, ny: 74};
const now = new Date('2026-10-25T01:00:00Z');
const snapshot = (overrides = {}) => ({location: seoul, savedAt: '2026-10-25T00:00:00Z', forecast: {forecastDate: '2026-10-25'}, ...overrides});

check('normalizes names and numeric coordinate strings without retaining extra fields', () => {
    const actual = Workspace.normalizeLocation({...seoul, locationName: '  서울\n 중구  ', nx: '60', ny: '127', extra: 'discarded'});
    assert.deepEqual(actual, seoul);
    assert.equal(Workspace.normalizeLocation({...seoul, regionName: null}).regionName, seoul.locationName);
    assert.equal(Workspace.normalizeLocation({...seoul, locationName: '가'.repeat(101)}).locationName.length, 100);
});
check('rejects nulls, arrays, malformed grids and missing names', () => {
    [null, [], '서울', 1, {}, {...seoul, nx: ''}, {...seoul, nx: false}, {...seoul, nx: []},
        {...seoul, nx: 0}, {...seoul, nx: 150}, {...seoul, ny: 254}, {...seoul, ny: 12.5},
        {...seoul, nx: Infinity}, {...seoul, locationName: '  '}, {...seoul, locationName: {}}]
        .forEach(value => assert.equal(Workspace.normalizeLocation(value), null));
});
check('coordinates preserve real zero and reject non-finite, absent or out-of-range values', () => {
    assert.equal(Workspace.normalizeLocation({...seoul, latitude: 0}).latitude, 0);
    [undefined, null, '', NaN, Infinity, true, [], -91, 91]
        .forEach(value => assert.equal(Workspace.normalizeLocation({...seoul, latitude: value}).latitude, null));
    [-181, 181].forEach(value => assert.equal(Workspace.normalizeLocation({...seoul, longitude: value}).longitude, null));
});
check('same place deduplicates but same name in different grids stays distinct', () => {
    const another = {...seoul, nx: 61};
    assert.equal(Workspace.uniqueLocations([seoul, {...seoul}, another, busan]).length, 3);
    assert.notEqual(Workspace.locationKey(seoul), Workspace.locationKey(another));
    assert.notEqual(Workspace.locationKey(seoul), Workspace.locationKey({...seoul, locationName: '서울 종로구'}));
    assert.equal(Workspace.locationKey(null), '');
});
check('uniqueLocations respects limits after filtering corrupt entries and does not mutate inputs', () => {
    const input = [null, seoul, seoul, busan];
    assert.deepEqual(Workspace.uniqueLocations(input, 1), [seoul]);
    assert.equal(input.length, 4);
    assert.deepEqual(Workspace.uniqueLocations(input, 0), []);
    assert.deepEqual(Workspace.uniqueLocations(input, 1.5), []);
    assert.deepEqual(Workspace.uniqueLocations({}), []);
});
check('Korean dates switch at 15:00 UTC independently of device timezone', () => {
    assert.equal(Workspace.koreaDate(0, '2026-10-24T14:59:59Z'), '2026-10-24');
    assert.equal(Workspace.koreaDate(0, '2026-10-24T15:00:00Z'), '2026-10-25');
    assert.equal(Workspace.koreaDate(1, '2026-12-31T14:59:59Z'), '2027-01-01');
    assert.equal(Workspace.koreaDate(2, '2024-02-28T03:00:00Z'), '2024-03-01');
});
check('Korean date is identical under Korean, American and UTC host zones', () => {
    const modulePath = path.resolve(__dirname, '../src/main/resources/static/weather-workspace.js');
    const script = `process.stdout.write(require(${JSON.stringify(modulePath)}).koreaDate(0, '2026-10-24T15:00:00Z'));`;
    for (const timezone of ['Asia/Seoul', 'America/Los_Angeles', 'UTC']) {
        const result = execFileSync(process.execPath, ['-e', script], {encoding: 'utf8', env: {...process.env, TZ: timezone}});
        assert.equal(result, '2026-10-25');
    }
});
check('invalid dates and fractional offsets do not silently become today', () => {
    assert.equal(Workspace.koreaDate(0, 'bad date'), null);
    assert.equal(Workspace.koreaDate(0, null), null);
    assert.equal(Workspace.koreaDate(0.5, now), null);
    assert.equal(Workspace.koreaDate(Infinity, now), null);
});
check('comparison uses exact valid date and time and preserves missing numeric data', () => {
    const hour = {time: '09:00', temperature: 0, precipitationProbability: null};
    const payload = {forecastDate: '20261025', hours: [hour]};
    assert.deepEqual(Workspace.comparisonHour(payload, '2026-10-25', '09:00'), hour);
    assert.notEqual(Workspace.comparisonHour(payload, '2026-10-25', '09:00'), hour);
    assert.equal(Workspace.comparisonHour(payload, '2026-10-26', '09:00'), null);
    assert.equal(Workspace.comparisonHour(payload, '2026-10-25', '09:30'), null);
});
check('comparison rejects malformed payloads, invalid times and ambiguous duplicates', () => {
    const hour = {time: '09:00', temperature: 15};
    const payload = {forecastDate: '2026-10-25', hours: [null, hour]};
    [null, [], {}, {...payload, hours: {}}, {...payload, hours: [hour, {...hour}]}]
        .forEach(value => assert.equal(Workspace.comparisonHour(value, '2026-10-25', '09:00'), null));
    ['9:00', '24:00', '09:60', ' 09:00', null].forEach(value => assert.equal(Workspace.comparisonHour(payload, '2026-10-25', value), null));
});
check('invalid calendar dates are never considered equivalent comparison dates', () => {
    for (const date of ['2026-02-30', '20260230', '2026-13-01', '2026-1025', '202610-25']) {
        assert.equal(Workspace.comparisonHour({forecastDate: date, hours: [{time: '09:00'}]}, date, '09:00'), null);
    }
});
check('snapshot allows only same location, target date and fresh timestamps', () => {
    assert.equal(Workspace.validSnapshot(snapshot(), seoul, 0, now), true);
    assert.equal(Workspace.validSnapshot(snapshot(), busan, 0, now), false);
    assert.equal(Workspace.validSnapshot(snapshot(), {...seoul, locationName: '다른 동네'}, 0, now), false);
    assert.equal(Workspace.validSnapshot(snapshot(), seoul, 1, now), false);
    assert.equal(Workspace.validSnapshot(snapshot({forecast: {forecastDate: '20261026'}}), seoul, 1, now), true);
});
check('snapshot expiry includes the three-hour boundary and rejects future timestamps', () => {
    assert.equal(Workspace.validSnapshot(snapshot({savedAt: '2026-10-24T22:00:00Z'}), seoul, 0, now), true);
    assert.equal(Workspace.validSnapshot(snapshot({savedAt: '2026-10-24T21:59:59Z'}), seoul, 0, now), false);
    assert.equal(Workspace.validSnapshot(snapshot({savedAt: '2026-10-25T01:00:01Z'}), seoul, 0, now), false);
});
check('a previous today snapshot cannot be reused after Korean midnight', () => {
    const old = snapshot({savedAt: '2026-10-24T14:59:00Z', forecast: {forecastDate: '2026-10-24'}});
    assert.equal(Workspace.validSnapshot(old, seoul, 0, '2026-10-24T14:59:59Z'), true);
    assert.equal(Workspace.validSnapshot(old, seoul, 0, '2026-10-24T15:00:01Z'), false);
});
check('malformed or timezone-ambiguous snapshots fail closed without throwing', () => {
    [null, [], {}, snapshot({location: null}), snapshot({forecast: null}), snapshot({forecast: {}}),
        snapshot({savedAt: ''}), snapshot({savedAt: '2026-10-25T09:00:00'}), snapshot({savedAt: 'invalid'})]
        .forEach(value => assert.equal(Workspace.validSnapshot(value, seoul, 0, now), false));
    assert.equal(Workspace.validSnapshot(snapshot(), seoul, -1, now), false);
    assert.equal(Workspace.validSnapshot(snapshot(), seoul, 3, now), false);
    assert.equal(Workspace.validSnapshot(snapshot(), seoul, 0, new Date(NaN)), false);
});

console.log(`Weather workspace: ${cases} regression cases passed`);
