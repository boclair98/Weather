/* Pure chart geometry; missing observations and missing hours are never interpolated. */
(function (root) {
    "use strict";
    function temperature(value) {
        if (value === null || value === undefined || String(value).trim() === "") return null;
        var number = Number(value);
        return Number.isFinite(number) && number >= -100 && number <= 100 ? number : null;
    }
    function minutes(time) {
        var match = /^(\d{2}):(\d{2})$/.exec(String(time || ""));
        if (!match || Number(match[1]) > 23 || Number(match[2]) > 59) return null;
        return Number(match[1]) * 60 + Number(match[2]);
    }
    function layout(hours) {
        var values = (Array.isArray(hours) ? hours : []).map(function (hour, index) {
            return {value: temperature(hour.temperature), minute: minutes(hour.time), index: index};
        });
        var valid = values.filter(function (point) { return point.value !== null && point.minute !== null; });
        if (!valid.length) return {points: [], paths: [], width: Math.max(1, values.length) * 100};
        var min = Math.min.apply(null, valid.map(function (point) { return point.value; }));
        var max = Math.max.apply(null, valid.map(function (point) { return point.value; }));
        var span = Math.max(6, max - min + 2);
        var center = (min + max) / 2;
        var points = [], paths = [], previous = null, path = "";
        values.forEach(function (point) {
            if (point.value === null || point.minute === null) {
                if (path) paths.push(path);
                path = ""; previous = null; return;
            }
            var x = point.index * 100 + 50;
            var y = Number((35 - (point.value - center) / span * 60).toFixed(2));
            var contiguous = previous && point.minute - previous.minute === 60;
            if (!contiguous && path) { paths.push(path); path = ""; }
            path += (contiguous ? " L " : "M ") + x + " " + y;
            points.push({x: x, y: y, value: point.value});
            previous = point;
        });
        if (path) paths.push(path);
        return {points: points, paths: paths, width: Math.max(1, values.length) * 100};
    }
    var api = {layout: layout};
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.WeatherChart = api;
})(typeof globalThis !== "undefined" ? globalThis : this);
