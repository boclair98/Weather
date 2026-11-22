// Executes the production worker with isolated browser primitives. No network or disk writes.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../src/main/resources/static/service-worker-v17.js'), 'utf8');
const currentCache = source.match(/const CACHE_NAME = "([^"]+)"/)[1];
const origin = 'https://weather.example.test';
const asset = '/weather.css?v=20261013-rollback-tablet-v1';
const tick = () => new Promise(setImmediate);
function deferred() {
    let resolve, reject;
    const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
    return {promise, resolve, reject};
}

function harness({network = () => Promise.resolve(new Response('network')), cacheNames = [],
    onOpen = async () => {}, onPut = async () => {}, onAddAll = async () => {},
    onKeys = async () => {}, onDelete = async () => {}, onMatch = async () => {}} = {}) {
    const handlers = new Map(), stored = new Map();
    const calls = {fetch: [], open: [], put: [], addAll: [], deleted: [], claimed: 0, skipped: 0, match: []};
    const key = input => new URL(typeof input === 'string' ? input : input.url, origin).href;
    const cache = {
        async addAll(paths) { calls.addAll.push([...paths]); await onAddAll(paths); },
        async put(input, response) {
            calls.put.push(key(input));
            await onPut(input, response);
            stored.set(key(input), response);
        }
    };
    const ctx = {
        URL, Response,
        self: {
            location: {origin},
            addEventListener(name, handler) { handlers.set(name, handler); },
            skipWaiting() { calls.skipped += 1; },
            clients: {async claim() { calls.claimed += 1; }}
        },
        caches: {
            async open(name) { calls.open.push(name); await onOpen(name); return cache; },
            async keys() { await onKeys(); return [...cacheNames]; },
            async delete(name) { calls.deleted.push(name); await onDelete(name); return true; },
            async match(input) {
                calls.match.push(key(input));
                await onMatch(input);
                return stored.get(key(input))?.clone();
            }
        },
        fetch(request) { calls.fetch.push(request.url); return network(request); }
    };
    vm.runInNewContext(source, ctx);
    function dispatch(name, request) {
        const pending = [];
        let response;
        handlers.get(name)({request,
            waitUntil(promise) { pending.push(Promise.resolve(promise)); },
            respondWith(promise) { assert.equal(response, undefined); response = Promise.resolve(promise); }
        });
        return {
            get response() { return response; },
            get lifetimeTasks() { return pending.length; },
            async settle() {
                if (response) await response;
                for (let index = 0; index < pending.length; index += 1) await pending[index];
                await tick();
            }
        };
    }
    return {calls, stored, key, dispatch,
        cache(path, body) { stored.set(key(path), new Response(body)); },
        fetch(path, options = {}) {
            return dispatch('fetch', {url: new URL(path, origin).href, method: 'GET', mode: 'cors', ...options});
        }};
}

let cases = 0;
async function check(name, body) { await body(); cases += 1; console.log(`PASS ${name}`); }
async function run() {
    await check('installation precaches a static offline document, not account-specific homepage HTML', async () => {
        const h = harness();
        await h.dispatch('install').settle();
        assert.equal(h.calls.addAll.length, 1);
        assert.ok(h.calls.addAll[0].includes('/offline.html'));
        assert.ok(!h.calls.addAll[0].includes('/'));
        assert.ok(h.calls.addAll[0].every(url => !url.startsWith('/api/') && !url.includes('emailVerified')));
        assert.deepEqual(h.calls.open, [currentCache]);
        assert.equal(h.calls.skipped, 1);
    });
    await check('activation removes only obsolete weather-shell caches and preserves unrelated caches', async () => {
        const h = harness({cacheNames: ['weather-shell-v1', 'weather-shell-v31', currentCache, 'other-app-assets', 'weather-data-private']});
        await h.dispatch('activate').settle();
        assert.deepEqual(h.calls.deleted, ['weather-shell-v1', 'weather-shell-v31']);
        assert.equal(h.calls.claimed, 1);
    });
    await check('successful navigation with an auth-return query is never written to cache', async () => {
        const h = harness({network: async () => new Response('<html>account-specific page</html>', {headers: {'Content-Type': 'text/html'}})});
        const event = h.fetch('/?emailVerified=test-only-placeholder', {mode: 'navigate'});
        assert.equal(await (await event.response).text(), '<html>account-specific page</html>');
        await event.settle();
        assert.equal(h.calls.fetch.length, 1);
        assert.deepEqual(h.calls.put, []);
        assert.deepEqual(h.calls.match, []);
        assert.deepEqual(h.calls.open, []);
    });
    await check('offline navigation returns only the static offline page, never a cached session homepage', async () => {
        const h = harness({network: async () => { throw new Error('offline'); }});
        h.cache('/offline.html', '<html>offline public information</html>');
        h.cache('/', '<html>old signed-in session</html>');
        const event = h.fetch('/private/settings', {mode: 'navigate'});
        assert.equal(await (await event.response).text(), '<html>offline public information</html>');
        await event.settle();
        assert.deepEqual(h.calls.match, [h.key('/offline.html')]);
        assert.deepEqual(h.calls.put, []);
    });
    await check('offline navigation still has a readable 503 when no offline asset is cached', async () => {
        const h = harness({network: async () => { throw new Error('offline'); }});
        const response = await h.fetch('/', {mode: 'navigate'}).response;
        assert.equal(response.status, 503);
        assert.match(response.headers.get('Content-Type'), /text\/plain/);
        assert.match(await response.text(), /네트워크 연결/);
    });
    await check('API GET responses are network-only even if a matching cache entry exists', async () => {
        const h = harness({network: async () => new Response('{"fresh":true}', {headers: {'Content-Type': 'application/json'}})});
        h.cache('/api/users/me', '{"stalePrivate":true}');
        const event = h.fetch('/api/users/me');
        assert.deepEqual(await (await event.response).json(), {fresh: true});
        await event.settle();
        assert.deepEqual(h.calls.match, []);
        assert.deepEqual(h.calls.put, []);
    });
    await check('API network failure produces a Korean OFFLINE JSON 503 without cached private data', async () => {
        const h = harness({network: async () => { throw new Error('offline'); }});
        const response = await h.fetch('/api/users/me').response;
        assert.equal(response.status, 503);
        const payload = await response.json();
        assert.equal(payload.code, 'OFFLINE');
        assert.match(payload.message, /네트워크 연결/);
        assert.deepEqual(h.calls.match, []);
    });
    await check('non-GET, unrelated private assets and cross-origin requests bypass worker caching', async () => {
        const h = harness();
        for (const [url, options] of [
            ['/api/users/subscribe', {method: 'POST'}],
            [asset, {method: 'PUT'}],
            ['/private/account-avatar.png', {}],
            ['/weather.css?v=unrecognized-version', {}],
            ['https://unrelated.example.test/weather.css', {}]
        ]) assert.equal(h.fetch(url, options).response, undefined);
        assert.deepEqual(h.calls.fetch, []);
        assert.deepEqual(h.calls.match, []);
        assert.deepEqual(h.calls.put, []);
    });
    await check('allowlisted cached assets render immediately and refresh in the background', async () => {
        const refresh = deferred();
        const h = harness({network: () => refresh.promise});
        h.cache(asset, 'cached css');
        const event = h.fetch(asset);
        assert.equal(await (await event.response).text(), 'cached css');
        assert.equal(event.lifetimeTasks, 1);
        assert.equal(h.calls.fetch.length, 1);
        refresh.resolve(new Response('updated css'));
        await event.settle();
        assert.deepEqual(h.calls.put, [h.key(asset)]);
        assert.equal(await h.stored.get(h.key(asset)).text(), 'updated css');
    });
    await check('allowlisted uncached assets come from the network and enter only the current shell cache', async () => {
        const h = harness({network: async () => new Response('fresh css')});
        const event = h.fetch(asset);
        assert.equal(await (await event.response).text(), 'fresh css');
        await event.settle();
        assert.deepEqual(h.calls.open, [currentCache]);
        assert.deepEqual(h.calls.put, [h.key(asset)]);
    });
    await check('failed background refresh keeps the cached public asset usable', async () => {
        const h = harness({network: async () => { throw new Error('offline'); }});
        h.cache(asset, 'cached css');
        const event = h.fetch(asset);
        assert.equal(await (await event.response).text(), 'cached css');
        await event.settle();
        assert.deepEqual(h.calls.put, []);
    });
    await check('HTTP errors are returned without poisoning the allowed asset cache', async () => {
        const h = harness({network: async () => new Response('unavailable', {status: 503})});
        const event = h.fetch(asset);
        assert.equal((await event.response).status, 503);
        await event.settle();
        assert.deepEqual(h.calls.put, []);
    });
    await check('cache.open rejection does not suppress a valid network asset response', async () => {
        const h = harness({network: async () => new Response('fresh css'),
            onOpen: async () => { throw new Error('SecurityError'); }});
        const event = h.fetch(asset);
        const response = await event.response;
        assert.equal(response.status, 200);
        assert.equal(await response.text(), 'fresh css');
        await event.settle();
        assert.deepEqual(h.calls.open, [currentCache]);
        assert.deepEqual(h.calls.put, []);
    });
    await check('cache.put rejection does not suppress a valid network asset response', async () => {
        const h = harness({network: async () => new Response('fresh css'),
            onPut: async () => { throw new Error('QuotaExceededError'); }});
        const event = h.fetch(asset);
        const response = await event.response;
        assert.equal(response.status, 200);
        assert.equal(await response.text(), 'fresh css');
        await event.settle();
        assert.deepEqual(h.calls.put, [h.key(asset)]);
        assert.equal(h.stored.has(h.key(asset)), false);
    });
    await check('background cache quota errors leave the existing public asset usable', async () => {
        const h = harness({network: async () => new Response('fresh css'),
            onPut: async () => { throw new Error('QuotaExceededError'); }});
        h.cache(asset, 'cached css');
        const event = h.fetch(asset);
        assert.equal(await (await event.response).text(), 'cached css');
        await event.settle();
        assert.equal(await h.stored.get(h.key(asset)).text(), 'cached css');
        assert.deepEqual(h.calls.put, [h.key(asset)]);
    });
    await check('uncached network response lifetime includes its pending cache.put operation', async () => {
        const write = deferred();
        const h = harness({network: async () => new Response('fresh css'), onPut: () => write.promise});
        const event = h.fetch(asset);
        let delivered = false;
        event.response.then(() => { delivered = true; });
        await tick();
        assert.deepEqual(h.calls.put, [h.key(asset)]);
        assert.equal(delivered, false);
        assert.equal(h.stored.has(h.key(asset)), false);
        write.resolve();
        assert.equal(await (await event.response).text(), 'fresh css');
        await event.settle();
        assert.equal(delivered, true);
        assert.equal(await h.stored.get(h.key(asset)).text(), 'fresh css');
    });
    await check('cached response is immediate but background lifetime waits for cache.put completion', async () => {
        const write = deferred();
        const h = harness({network: async () => new Response('fresh css'), onPut: () => write.promise});
        h.cache(asset, 'cached css');
        const event = h.fetch(asset);
        assert.equal(await (await event.response).text(), 'cached css');
        let settled = false;
        const lifetime = event.settle().then(() => { settled = true; });
        await tick();
        assert.deepEqual(h.calls.put, [h.key(asset)]);
        assert.equal(event.lifetimeTasks, 1);
        assert.equal(settled, false);
        write.resolve();
        await lifetime;
        assert.equal(settled, true);
        assert.equal(await h.stored.get(h.key(asset)).text(), 'fresh css');
    });
    await check('denied cache opening during installation does not block the privacy worker upgrade', async () => {
        const h = harness({onOpen: async () => { throw new Error('SecurityError'); }});
        await h.dispatch('install').settle();
        assert.deepEqual(h.calls.open, [currentCache]);
        assert.equal(h.calls.addAll.length, 0);
        assert.equal(h.calls.skipped, 1);
        await h.dispatch('activate').settle();
        assert.equal(h.calls.claimed, 1);
        const response = await h.fetch('/?emailVerified=test-only-placeholder', {mode: 'navigate'}).response;
        assert.equal(response.status, 200);
        assert.deepEqual(h.calls.put, []);
    });
    await check('failed shell precaching does not reject installation or prevent activation', async () => {
        const h = harness({onAddAll: async () => { throw new Error('Offline precache failed'); }});
        await h.dispatch('install').settle();
        assert.equal(h.calls.addAll.length, 1);
        assert.equal(h.calls.skipped, 1);
        await h.dispatch('activate').settle();
        assert.equal(h.calls.claimed, 1);
    });
    await check('denied cache listing still allows the safer worker to claim clients', async () => {
        const h = harness({onKeys: async () => { throw new Error('SecurityError'); }});
        await h.dispatch('activate').settle();
        assert.deepEqual(h.calls.deleted, []);
        assert.equal(h.calls.claimed, 1);
    });
    await check('failed obsolete cache deletion does not block activation or touch unrelated caches', async () => {
        const h = harness({cacheNames: ['weather-shell-v1', 'unrelated-cache', currentCache],
            onDelete: async () => { throw new Error('Storage unavailable'); }});
        await h.dispatch('activate').settle();
        assert.deepEqual(h.calls.deleted, ['weather-shell-v1']);
        assert.equal(h.calls.claimed, 1);
    });
    await check('denied asset cache reads behave as a cache miss and preserve the network response', async () => {
        const h = harness({network: async () => new Response('fresh css'),
            onMatch: async () => { throw new Error('SecurityError'); }});
        h.cache(asset, 'unreadable cached css');
        const event = h.fetch(asset);
        assert.equal(await (await event.response).text(), 'fresh css');
        await event.settle();
        assert.equal(h.calls.fetch.length, 1);
        assert.deepEqual(h.calls.match, [h.key(asset)]);
        assert.deepEqual(h.calls.put, [h.key(asset)]);
    });
    await check('offline navigation with denied cache access returns a readable fallback 503', async () => {
        const h = harness({network: async () => { throw new Error('Offline'); },
            onMatch: async () => { throw new Error('SecurityError'); }});
        h.cache('/offline.html', 'unreadable offline asset');
        h.cache('/', 'old account-specific page');
        const event = h.fetch('/account', {mode: 'navigate'});
        const response = await event.response;
        assert.equal(response.status, 503);
        assert.match(response.headers.get('Content-Type'), /text\/plain/);
        assert.match(await response.text(), /네트워크 연결/);
        await event.settle();
        assert.deepEqual(h.calls.match, [h.key('/offline.html')]);
        assert.deepEqual(h.calls.put, []);
    });
    console.log(`Weather service worker: ${cases} regression cases passed`);
}
run().catch(error => { console.error(error); process.exitCode = 1; });
