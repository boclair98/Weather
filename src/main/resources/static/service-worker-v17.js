const CACHE_NAME = "weather-shell-v38";
const APP_SHELL = ["/offline.html", "/weather.css?v=20261013-rollback-tablet-v1", "/weather-letter.css?v=20261023-observatory-v1", "/weather-observatory.css?v=20261122-first-use-v4", "/weather-letter.js?v=20261122-friendly-copy-v1", "/weather-chart.js?v=20261023-observatory-v1", "/weather-workspace.js?v=20261025-v1", "/weather-compare.js?v=20261025-v1", "/favicon.svg", "/manifest.webmanifest"];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE_NAME).then((cache) => cache.addAll(APP_SHELL)).catch(() => undefined));
  self.skipWaiting();
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key.startsWith("weather-shell-") && key !== CACHE_NAME).map((key) => caches.delete(key))))
      .catch(() => undefined)
      .then(() => self.clients.claim())
  );
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  if (request.method !== "GET") return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin) return;

  if (url.pathname.startsWith("/api/")) {
    event.respondWith(
      fetch(request).catch(() => new Response(
        JSON.stringify({message: "네트워크 연결을 확인한 뒤 다시 시도해주세요.", code: "OFFLINE"}),
        {status: 503, headers: {"Content-Type": "application/json;charset=UTF-8"}}
      ))
    );
    return;
  }

  if (request.mode === "navigate") {
    // Never cache account-specific HTML, SSO callbacks, tokens or subscription pages.
    event.respondWith(
      fetch(request)
        .catch(async () => (await caches.match("/offline.html").catch(() => undefined)) || new Response("네트워크 연결을 확인해주세요.", {status: 503, headers: {"Content-Type": "text/plain;charset=UTF-8"}}))
    );
    return;
  }

  // Only public shell assets belong in the service-worker cache.
  if (!APP_SHELL.includes(url.pathname + url.search)) return;

  event.respondWith(
    caches.match(request).catch(() => undefined).then((cached) => {
      const network = fetch(request).then(async (response) => {
        if (response.ok) {
          try { await (await caches.open(CACHE_NAME)).put(request, response.clone()); }
          catch { /* Cache quota/storage failure must not block a valid network response. */ }
        }
        return response;
      });
      if (cached) {
        event.waitUntil(network.catch(() => undefined));
        return cached;
      }
      return network;
    })
  );
});
