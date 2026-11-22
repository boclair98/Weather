/* Pure location/date helpers. No storage, network, DOM or credentials. */
(function (root) {
    "use strict";

    var koreaFormatter = new Intl.DateTimeFormat("en-CA", {
        timeZone: "Asia/Seoul", year: "numeric", month: "2-digit", day: "2-digit"
    });
    var snapshotMaxAge = 3 * 60 * 60 * 1000;

    function record(value) {
        return value !== null && typeof value === "object" && !Array.isArray(value);
    }

    function number(value) {
        if (typeof value !== "number" && typeof value !== "string") return null;
        if (typeof value === "string" && value.trim() === "") return null;
        var parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    }

    function text(value) {
        if (typeof value !== "string") return "";
        return value.normalize("NFC").replace(/[\u0000-\u001f\u007f]/g, " ")
            .replace(/\s+/g, " ").trim().slice(0, 100);
    }

    function coordinate(value, min, max) {
        var parsed = number(value);
        return parsed !== null && parsed >= min && parsed <= max ? parsed : null;
    }

    function normalizeLocation(raw) {
        if (!record(raw)) return null;
        var nx = number(raw.nx), ny = number(raw.ny);
        var name = text(raw.locationName);
        if (!name || !Number.isInteger(nx) || nx < 1 || nx > 149
            || !Number.isInteger(ny) || ny < 1 || ny > 253) return null;
        return {
            locationName: name,
            regionName: text(raw.regionName) || name,
            nx: nx,
            ny: ny,
            latitude: coordinate(raw.latitude, -90, 90),
            longitude: coordinate(raw.longitude, -180, 180)
        };
    }

    function locationKey(location) {
        var normalized = normalizeLocation(location);
        return normalized ? JSON.stringify([normalized.nx, normalized.ny, normalized.locationName]) : "";
    }

    function uniqueLocations(locations, limit) {
        if (!Array.isArray(locations)) return [];
        var maximum = limit === undefined ? 8 : number(limit);
        if (!Number.isInteger(maximum) || maximum < 1) return [];
        maximum = Math.min(maximum, 32);
        var seen = new Set(), result = [];
        for (var i = 0; i < locations.length && result.length < maximum; i += 1) {
            var location = normalizeLocation(locations[i]);
            if (!location) continue;
            var key = locationKey(location);
            if (seen.has(key)) continue;
            seen.add(key);
            result.push(location);
        }
        return result;
    }

    function timestamp(value) {
        if (!(value instanceof Date) && typeof value !== "string" && typeof value !== "number") return NaN;
        return new Date(value).getTime();
    }

    function normalizeDate(value) {
        if (typeof value !== "string") return null;
        var match = /^(\d{4})-?(\d{2})-?(\d{2})$/.exec(value);
        if (!match) return null;
        // Do not accept mixed delimiters, rollover dates or two-digit Date.UTC years.
        if (value.length !== 8 && value.length !== 10) return null;
        var year = Number(match[1]), month = Number(match[2]), day = Number(match[3]);
        if (year < 1000 || month < 1 || month > 12 || day < 1 || day > 31) return null;
        var date = new Date(Date.UTC(year, month - 1, day));
        if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null;
        return match[1] + "-" + match[2] + "-" + match[3];
    }

    function koreaDate(offset, now) {
        var days = offset === undefined ? 0 : number(offset);
        var time = timestamp(now === undefined ? new Date() : now);
        if (!Number.isInteger(days) || Math.abs(days) > 366 || !Number.isFinite(time)) return null;
        var parts = {};
        koreaFormatter.formatToParts(new Date(time)).forEach(function (part) { parts[part.type] = part.value; });
        var date = new Date(Date.UTC(Number(parts.year), Number(parts.month) - 1, Number(parts.day) + days));
        return normalizeDate(date.toISOString().slice(0, 10));
    }

    function comparisonHour(payload, date, time) {
        var targetDate = normalizeDate(date);
        if (!record(payload) || !targetDate || normalizeDate(payload.forecastDate) !== targetDate
            || typeof time !== "string" || !/^(?:[01]\d|2[0-3]):[0-5]\d$/.test(time)
            || !Array.isArray(payload.hours)) return null;
        var matches = payload.hours.filter(function (hour) { return record(hour) && hour.time === time; });
        // Ambiguous duplicate times are unavailable, never silently selected.
        return matches.length === 1 ? Object.assign({}, matches[0]) : null;
    }

    function validSnapshot(snapshot, location, dayOffset, now) {
        var offset = dayOffset === undefined ? 0 : number(dayOffset);
        var current = timestamp(now === undefined ? new Date() : now);
        if (!record(snapshot) || !record(snapshot.forecast) || !Number.isFinite(current)
            || !Number.isInteger(offset) || offset < 0 || offset > 2) return false;
        var key = locationKey(location);
        if (!key || locationKey(snapshot.location) !== key) return false;
        var date = normalizeDate(snapshot.forecast.forecastDate);
        if (!date || date !== koreaDate(offset, current)) return false;
        if (typeof snapshot.savedAt !== "string" || !/T.*(?:Z|[+-]\d{2}:\d{2})$/.test(snapshot.savedAt)) return false;
        var saved = timestamp(snapshot.savedAt);
        return Number.isFinite(saved) && saved <= current && current - saved <= snapshotMaxAge;
    }

    var api = {
        normalizeLocation: normalizeLocation,
        locationKey: locationKey,
        uniqueLocations: uniqueLocations,
        koreaDate: koreaDate,
        comparisonHour: comparisonHour,
        validSnapshot: validSnapshot
    };
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.WeatherWorkspace = api;
})(typeof globalThis !== "undefined" ? globalThis : this);
