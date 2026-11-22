// Executes the actual template's login listener and draft persistence functions.
// No browser session, authentication, network request or email is involved.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const html = fs.readFileSync(path.join(__dirname, '../src/main/resources/templates/index.html'), 'utf8');
const functions = ['saveSubscriptionDraft', 'restoreSubscriptionDraft', 'normalizeTimeInput'].map(name => {
    const match = html.match(new RegExp(`    function ${name}\\([^]*?(?=\\r?\\n    (?:async )?function )`));
    assert.ok(match, `${name} exists in production`);
    return match[0];
}).join('\n');
const listener = html.match(/    subscriptionIntro\.querySelector\("\.subscription-login-action"\)\.addEventListener[^\r\n]+/);
assert.ok(listener, 'The actual login action preserves the draft');

function harness(storage) {
    const nodes = new Map();
    const node = id => {
        if (!nodes.has(id)) nodes.set(id, {value: '', checked: false});
        return nodes.get(id);
    };
    const calls = {weather: [], panel: [], searchControls: 0};
    let onLogin;
    const context = {
        document: {getElementById: node},
        emailInput: node('email'), locationQuery: node('locationQuery'), locationMeta: {},
        selectedLocation: {locationName: '을지로3가역', nx: 60, ny: 127},
        subscriptionSource: 'direct', subscriptionDraftKey: 'test-draft',
        emailVerificationToken: '', sessionStorage: storage,
        subscriptionIntro: {querySelector: () => ({addEventListener: (event, fn) => {
            assert.equal(event, 'click'); onLogin = fn;
        }})},
        syncLocationSearchControls: () => calls.searchControls += 1,
        loadWeatherPreview: location => calls.weather.push(location),
        openSubscriptionPanel: options => calls.panel.push(options),
        showSubscriptionResult() {}
    };
    node('morningEnabled').checked = true;
    node('morningTime').value = '07:15';
    vm.createContext(context);
    vm.runInContext(functions + '\n' + listener[0], context);
    return {context, calls, node, clickLogin: () => onLogin()};
}

const saved = new Map();
const storage = {setItem: (key, value) => saved.set(key, value), getItem: key => saved.get(key), removeItem: key => saved.delete(key)};
const before = harness(storage);
before.clickLogin();
const after = harness(storage);
after.context.selectedLocation = null;
after.context.restoreSubscriptionDraft();
assert.equal(after.context.selectedLocation.locationName, '을지로3가역');
assert.equal(after.node('morningTime').value, '07:15');
assert.equal(after.calls.weather.length, 1);
assert.equal(after.calls.searchControls, 1);
assert.equal(after.calls.panel[0].focusForm, true);
console.log('PASS login draft restores location, notification time and subscription intent');

saved.set('test-draft', '{invalid');
assert.doesNotThrow(() => after.context.restoreSubscriptionDraft());
assert.equal(saved.has('test-draft'), false);
console.log('PASS invalid draft is safely discarded');

const blocked = harness({getItem() { throw new Error('SecurityError'); }, setItem() { throw new Error('QuotaExceededError'); }, removeItem() { throw new Error('SecurityError'); }});
assert.doesNotThrow(() => blocked.clickLogin());
assert.doesNotThrow(() => blocked.context.restoreSubscriptionDraft());
console.log('PASS unavailable storage does not interrupt login or page startup');
