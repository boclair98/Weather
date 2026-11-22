/* Deterministic presentation only: no model requests or invented measurements. */
(function (root) {
    "use strict";
    function number(value) {
        if (value === null || value === undefined || String(value).trim() === "") return null;
        var parsed = Number(value);
        return Number.isFinite(parsed) ? parsed : null;
    }
    function probability(value) {
        var n = number(value);
        return n !== null && n >= 0 && n <= 100 ? n : null;
    }
    function clean(value) {
        return String(value || "").replace(/[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}\uFE0F\u200D]/gu, "").trim();
    }
    function defaultPeriod(dayOffset, now) {
        if (Number(dayOffset) > 0) return "MORNING";
        var hour = Number(new Intl.DateTimeFormat("en-GB", {
            timeZone: "Asia/Seoul", hour: "2-digit", hourCycle: "h23"
        }).format(now || new Date()));
        return hour < 11 ? "MORNING" : hour < 17 ? "AFTERNOON" : "EVENING";
    }
    function periods(daily) {
        return [["아침", daily.morning], ["낮", daily.afternoon], ["저녁", daily.evening]]
            .filter(function (entry) { return Boolean(entry[1]); });
    }
    function build(daily, hours, options) {
        daily = daily || {};
        hours = Array.isArray(hours) ? hours : [];
        options = options || {};
        var format = options.formatTemperature || function (value) { return number(value) === null ? "자료 없음" : value + "°C"; };
        var slots = periods(daily);
        var samples = hours.length ? hours : slots.map(function (slot) {
            return {precipitationProbability: slot[1].pop, precipitation: slot[1].precipitation};
        });
        var pops = samples.map(function (hour) { return probability(hour.precipitationProbability); });
        var knownPops = pops.filter(function (value) { return value !== null; });
        var allKnown = pops.length > 0 && knownPops.length === pops.length;
        var maxPop = knownPops.length ? Math.max.apply(null, knownPops) : null;
        var wet = hours.find(function (hour) {
            return /비|눈|소나기/.test(String(hour.precipitation || "")) || probability(hour.precipitationProbability) >= 60;
        });
        var lows = slots.map(function (slot) { return number(slot[1].tmn); }).filter(function (n) { return n !== null; });
        var highs = slots.map(function (slot) { return number(slot[1].tmx); }).filter(function (n) { return n !== null; });
        var temperatures = slots.map(function (slot) { return number(slot[1].tmp); })
            .filter(function (n) { return n !== null; });
        if (!temperatures.length) {
            temperatures = hours.map(function (hour) { return number(hour.temperature); })
                .filter(function (n) { return n !== null; });
        }
        var range = lows.length && highs.length
            ? "최저 " + format(Math.min.apply(null, lows)) + " · 최고 " + format(Math.max.apply(null, highs))
            : temperatures.length
                ? "시간대 " + format(Math.min.apply(null, temperatures)) + " ~ " + format(Math.max.apply(null, temperatures))
                : "기온 자료 없음";
        var rain = maxPop === null ? "강수 자료 없음"
            : (allKnown ? "최대 " : "확인된 시간 최대 ") + maxPop + "%";
        // A daily activity recommendation may refer to an elapsed period.
        // Keep the primary headline factual when upcoming hourly evidence is unavailable.
        var headline = slots.length ? "오늘 기온과 비 소식을 시간별로 살펴봐요" : "시간별 날씨를 확인해 보세요";
        var dayLabel = clean(daily.dayLabel);
        var isUpcomingDay = dayLabel && dayLabel !== "오늘";
        if (wet && wet.time) {
            var phenomenon = /눈/.test(String(wet.precipitation || "")) ? "눈" : "비";
            var particle = phenomenon === "눈" ? "이" : "가";
            headline = (isUpcomingDay ? dayLabel + " " : "") + wet.time + " 무렵 " + phenomenon + particle + " 올 수 있어요. 외출 전 한 번 더 확인해요";
        } else if (hours.length && allKnown && maxPop < 30) {
            headline = isUpcomingDay ? dayLabel + " 예보에는 비 소식이 적어요. 시간별로 확인해 보세요" : "남은 시간은 비 소식이 적어요. 시간별 예보를 확인해 보세요";
        }
        var air = slots.map(function (slot) { return clean(slot[1].airQualitySummary); })
            .find(function (value) { return value && value !== "-"; }) || "측정 자료 없음";
        return {headline: headline, range: range, rain: rain, air: air, incompleteRain: !allKnown};
    }
    var api = {build: build, number: number, probability: probability, periods: periods, defaultPeriod: defaultPeriod};
    if (typeof module !== "undefined" && module.exports) module.exports = api;
    else root.WeatherLetter = api;
})(typeof globalThis !== "undefined" ? globalThis : this);
