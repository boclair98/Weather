# 기관 연계 API 계약

## 설명 가능한 맞춤 날씨 필드

`GET /api/weather/daily`의 각 시간대 날씨와 관련 브리핑 응답은 다음 파생 필드를 포함합니다. 기존 필드에 대한 하위 호환 확장입니다.

| 필드 | 타입 | 의미 |
| --- | --- | --- |
| `outingScore` | number | 위험 감점을 반영한 20~100 외출 점수 |
| `personalizedFeelsLikeTemperature` | number | 체감 성향을 반영한 판단용 기온 |
| `personalizationSummary` | string | 적용한 활동 목적·체감 성향 설명 |
| `decisionExplanation` | string | 가장 영향이 큰 위험 요약 |
| `riskFactors` | array | 영향도 내림차순의 위험 근거 |

`riskFactors[]`는 `code`, `label`, `severity`, `scoreImpact`, `evidence`, `action`을 포함합니다. `scoreImpact`의 합을 100에서 차감하고 결과를 20~100으로 제한합니다. 소비자는 알려지지 않은 `code`를 무시해야 하며, 문구가 아닌 `code`를 분석 키로 사용해야 합니다.

활동 목적은 `DAILY`, `COMMUTE`, `OUTDOOR`, `FORMAL`, 체감 성향은 `NONE`, `COLD`, `HEAT`입니다. 기본값은 각각 `DAILY`, `NONE`입니다.

## `GET /api/weather/current`

웹 화면에서 사용하는 최신 실황 API입니다. 기존 `WEATHER_API_KEY`로 기상청 `getUltraSrtNcst`를 호출하므로 별도 인증키가 필요하지 않습니다.

```http
GET /api/weather/current?nx=61&ny=125&locationName=강남역
```

응답은 관측시각, 현재 기온, 체감온도, 습도, 풍속, 1시간 강수량·강수형태와 행동 안내를 포함합니다. 실황 원천 연결이 지연되면 최대 2시간 이내의 마지막 정상자료를 `fallback: true`로 명확히 표시합니다. 실황 조회 실패는 시간별·3일 예보 응답에 영향을 주지 않습니다.

## `GET /api/v1/weather/briefing`

안정적인 기관 연계를 위한 버전 고정 API입니다.

요청 예시:

```http
GET /api/v1/weather/briefing?nx=60&ny=127&locationName=서울시청&activityType=COMMUTE
```

응답 헤더:

```text
X-Schema-Version: weather-briefing/1.0
X-Data-Source: KMA_VILAGE_FORECAST
X-Data-Freshness: FRESH | RECENT | STALE | STALE_FALLBACK
X-Data-Quality: VERIFIED | PARTIAL | DEGRADED
X-Request-Id: 요청 추적 ID
```

응답은 생성시각, 데이터 출처·발표시각·수집시각·완전성, 3일 브리핑과 이용상 주의사항을 포함합니다. 소비자는 알 수 없는 필드를 무시하고 `schemaVersion`의 major 변경을 호환성 경계로 사용해야 합니다.

오류는 `application/problem+json`으로 반환되며 `type`, `title`, `status`, `code`, `message`, `requestId`, `timestamp`를 포함합니다.

## 품질 의미

- `VERIFIED`: 핵심 예보 필드 완전성 90% 이상이며 마지막 정상자료 fallback이 아님
- `PARTIAL`: 완전성 70~89%
- `DEGRADED`: 완전성 70% 미만 또는 원천 장애로 마지막 정상자료 제공

방재·대피 판단은 이 서비스 단독 결과가 아니라 기상청 공식 특보와 기관 지침을 우선해야 합니다.

## 구독 알림 시각

구독 생성과 로그인 계정의 알림 설정 변경은 `morningTime`, `afternoonTime`, `eveningTime`을 선택적으로 받습니다. 값은 `HH:mm` 형식이며 한국 시간(`Asia/Seoul`)으로 해석됩니다.

```json
{
  "morningEnabled": true,
  "morningTime": "07:05",
  "afternoonEnabled": false,
  "afternoonTime": "12:10",
  "eveningEnabled": true,
  "eveningTime": "18:20"
}
```

`PATCH /api/users/me/notifications`가 성공하면 저장된 시각과 사용 여부가 함께 반환됩니다. 발송 작업은 매분 해당 시각의 구독자만 조회하며, 같은 사용자·시간대는 한국 날짜마다 한 번만 claim하여 다중 인스턴스 환경의 중복 발송을 줄입니다.
