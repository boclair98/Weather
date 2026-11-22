/* On-demand comparison. No background requests, account data or persistence. */
(function (root) {
    'use strict';
    function init(options) {
        const doc = options.document || document;
        const model = root.WeatherWorkspace;
        const dialog = doc.getElementById('weatherCompareDialog');
        const choices = doc.getElementById('comparePlaceChoices');
        const day = doc.getElementById('compareDay');
        const time = doc.getElementById('compareTime');
        const run = doc.getElementById('runWeatherCompare');
        const status = doc.getElementById('compareStatus');
        const results = doc.getElementById('compareResults');
        let places = [], version = 0;
        function element(tag, className, text) {
            const node = doc.createElement(tag);
            if (className) node.className = className;
            if (text !== undefined) node.textContent = text;
            return node;
        }
        function cancel() {
            version += 1;
            for (let index = 0; index < 3; index += 1) options.cancel('compare-' + index);
            results.removeAttribute('aria-busy');
        }
        function selected() {
            return [...choices.querySelectorAll('input:checked')].map(input => places[Number(input.value)]).filter(Boolean);
        }
        function resetResults() {
            cancel();
            results.replaceChildren();
            const count = selected().length;
            run.disabled = count < 2 || count > 3 || !time.value;
            run.textContent = '날씨 비교하기';
            status.textContent = !time.value ? '오늘 남은 정시 예보가 없어요. 내일을 선택해주세요.'
                : count < 2 ? '지역을 2곳 이상 선택해주세요. 다른 지역은 검색한 뒤 비교할 수 있어요.'
                : `${count}곳을 선택했어요. 날짜와 시간을 확인하고 비교해주세요.`;
        }
        function setTimes() {
            const previous = time.value;
            time.replaceChildren();
            const parts = new Intl.DateTimeFormat('en-GB', {timeZone: 'Asia/Seoul', hour: '2-digit', minute: '2-digit', hourCycle: 'h23'})
                .formatToParts(new Date()).reduce((all, part) => {all[part.type] = part.value; return all;}, {});
            const start = day.value === '0' ? Number(parts.hour) + (Number(parts.minute) > 0 ? 1 : 0) : 0;
            for (let hour = start; hour < 24; hour += 1) {
                const value = String(hour).padStart(2, '0') + ':00';
                const option = element('option', '', value);
                option.value = value;
                time.appendChild(option);
            }
            if ([...time.options].some(option => option.value === previous)) time.value = previous;
            else if (day.value !== '0') time.value = '09:00';
            resetResults();
        }
        function open() {
            cancel();
            places = model.uniqueLocations(options.locations(), 12);
            choices.replaceChildren();
            places.forEach((place, index) => {
                const label = element('label', 'compare-place-choice');
                const input = element('input');
                input.type = 'checkbox'; input.value = String(index); input.checked = index < 3;
                const name = element('span', '', place.locationName);
                label.append(input, name);
                choices.appendChild(label);
            });
            day.value = String(options.day());
            setTimes();
            if (!time.value) { day.value = '1'; setTimes(); }
            dialog.showModal();
        }
        function metric(value, unit, min, max) {
            if (value === null || value === undefined || String(value).trim() === '') return '자료 없음';
            const number = Number(value);
            return Number.isFinite(number) && number >= min && number <= max ? `${number}${unit}` : '자료 없음';
        }
        function renderCard(card, place, payload, date, hour, dayOffset) {
            const match = model.comparisonHour(payload, date, hour);
            card.replaceChildren(element('h3', '', place.locationName));
            if (!match) {
                card.appendChild(element('p', 'compare-missing', '선택한 날짜·시간의 예보가 없어요. 다른 시간으로 다시 조회해주세요.'));
                return false;
            }
            const summary = element('div', 'compare-weather');
            const icon = element('span', 'weather-icon');
            icon.setAttribute('aria-hidden', 'true');
            options.icon(icon, match);
            summary.append(icon, element('strong', '', options.temperature(match.temperature)));
            card.append(summary, element('p', 'compare-condition', match.precipitation && match.precipitation !== '없음'
                ? match.precipitation : (match.sky || '상태 정보 없음')));
            const list = element('dl', 'compare-metrics');
            for (const [label, value] of [
                ['강수확률', metric(match.precipitationProbability, '%', 0, 100)],
                ['습도', metric(match.humidity, '%', 0, 100)],
                ['바람', metric(match.windSpeed, 'm/s', 0, 200)]
            ]) list.append(element('dt', '', label), element('dd', '', value));
            const provenance = payload.provenance || {};
            card.append(list, element('p', 'compare-source', `${provenance.fallback ? '마지막 정상 예보' : '기상청 예보'} · 발표 ${options.issued(provenance.forecastIssuedAt)}`));
            const view = element('button', 'compare-view', '이 지역 자세히 보기');
            view.type = 'button';
            view.addEventListener('click', () => {dialog.close(); options.select(place, dayOffset);});
            card.appendChild(view);
            return true;
        }
        choices.addEventListener('change', event => {
            if (selected().length > 3) {
                event.target.checked = false;
                status.textContent = '한 번에 3곳까지 비교할 수 있어요. 선택한 지역 하나를 해제해주세요.';
                return;
            }
            resetResults();
        });
        day.addEventListener('change', setTimes);
        time.addEventListener('change', resetResults);
        doc.getElementById('closeWeatherCompare').addEventListener('click', () => dialog.close());
        doc.getElementById('compareFindPlace').addEventListener('click', () => {dialog.close(); options.search();});
        dialog.addEventListener('close', cancel);
        dialog.addEventListener('click', event => {if (event.target === dialog) dialog.close();});
        doc.getElementById('weatherCompareForm').addEventListener('submit', async event => {
            event.preventDefault();
            const selectedPlaces = selected();
            if (selectedPlaces.length < 2 || selectedPlaces.length > 3 || !time.value) return;
            cancel();
            const runVersion = version;
            const dayOffset = Number(day.value), hour = time.value, date = model.koreaDate(dayOffset);
            results.replaceChildren();
            results.setAttribute('aria-busy', 'true');
            run.disabled = true;
            run.textContent = '예보 확인 중';
            status.textContent = `${date} ${hour} 예보를 확인하고 있어요.`;
            let success = 0;
            await Promise.all(selectedPlaces.map(async (place, index) => {
                const card = element('section', 'compare-card');
                card.append(element('h3', '', place.locationName), element('p', 'compare-loading', '예보를 불러오는 중이에요…'));
                results.appendChild(card);
                try {
                    const params = new URLSearchParams({nx: place.nx, ny: place.ny, locationName: place.locationName, dayOffset});
                    const payload = await options.request(`/api/weather/hourly?${params}`, {key: 'compare-' + index, fallbackMessage: '이 지역의 예보를 불러오지 못했어요.'});
                    if (runVersion !== version || !dialog.open) return;
                    if (renderCard(card, place, payload, date, hour, dayOffset)) success += 1;
                } catch (error) {
                    if (runVersion !== version || !dialog.open) return;
                    card.replaceChildren(element('h3', '', place.locationName), element('p', 'compare-error', '예보를 불러오지 못했어요. 아래 다시 조회를 눌러주세요.'));
                }
            }));
            if (runVersion !== version || !dialog.open) return;
            results.removeAttribute('aria-busy');
            run.disabled = false;
            run.textContent = '다시 조회';
            status.textContent = `${date} ${hour} · ${selectedPlaces.length}곳 중 ${success}곳의 예보를 확인했어요.${success < selectedPlaces.length ? ' 일부 지역은 자료가 없거나 조회에 실패했어요.' : ''}`;
        });
        return {open, cancel};
    }
    root.WeatherCompare = {init};
})(typeof globalThis !== 'undefined' ? globalThis : this);
