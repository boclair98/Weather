# 기관 연계 API 계약

> **이번 계약 보완은 로컬 미배포 작업본입니다. 운영 반영 전 전체 검증이 필요합니다.** 기존 운영 URL의 응답이 아래 변경과 동일하다고 가정하지 마세요. 이 문서의 검증 목록은 실행 방법이며 성공 결과를 의미하지 않습니다.

## 설명 가능한 맞춤 날씨 필드

`GET /api/weather/daily`의 각 시간대 날씨와 관련 브리핑 응답은 다음 파생 필드를 포함합니다. 기존 필드명은 유지하며, 로컬 변경에서 결측 상태를 명시적으로 확장합니다. 숫자가 항상 존재한다고 가정한 소비자는 아래 `null`·`-1`·`UNAVAILABLE` 처리를 먼저 반영해야 합니다.

| 필드 | 타입 | 의미 |
| --- | --- | --- |
| `outingScore` | number | 필수 예보 자료가 있을 때 20~100; 자료 부족 시 `-1`이며 유효한 점수가 아님 |
| `outingScoreLabel` | string | 정상 점수 등급 또는 `자료 부족` |
| `personalizedFeelsLikeTemperature` | number 또는 null | 체감 성향을 반영한 판단용 기온; 원천 기온이 없으면 `null` |
| `personalizationSummary` | string | 적용한 활동 목적·체감 성향 설명 |
| `decisionExplanation` | string | 가장 영향이 큰 위험 요약 |
| `riskFactors` | array | 영향도 내림차순의 위험 근거 |

`riskFactors[]`는 `code`, `label`, `severity`, `scoreImpact`, `evidence`, `action`을 포함합니다. 필수 자료가 갖춰졌을 때 `scoreImpact`의 합을 100에서 차감하고 결과를 20~100으로 제한합니다. `outingScore == -1`은 0점 또는 최저 위험이 아니라 계산 불가이며, 그래프·순위·평균·추천에서 제외해야 합니다. 소비자는 알려지지 않은 `code`를 무시해야 하며, 문구가 아닌 `code`를 분석 키로 사용해야 합니다.

일별 비교 대상 시간대 중 기온·강수확률·강수형태·습도·풍속·하늘상태의 필수 자료를 갖춘 항목이 없으면 `bestPeriod: "UNAVAILABLE"`, `bestPeriodLabel: "자료 부족"`, `bestOutingScore: -1`을 반환합니다. 유효한 최적 시간대나 `-1점`으로 보여주지 말고 비교 자료가 부족하다고 안내해야 합니다. 원래 문자열형인 `WeatherDto`의 누락 측정값은 `"-"` 표기를 유지합니다.

활동 목적은 `DAILY`, `COMMUTE`, `OUTDOOR`, `FORMAL`, 체감 성향은 `NONE`, `COLD`, `HEAT`입니다. 기본값은 각각 `DAILY`, `NONE`입니다.

## `GET /api/weather/current`

웹 화면에서 사용하는 최신 실황 API입니다. 기존 `WEATHER_API_KEY`로 기상청 `getUltraSrtNcst`를 호출하므로 별도 인증키가 필요하지 않습니다.

```http
GET /api/weather/current?nx=61&ny=125&locationName=강남역
```

응답은 관측시각, 현재 기온, 체감온도, 습도, 풍속, 1시간 강수량·강수형태와 행동 안내를 포함합니다. 실황 원천 연결이 지연되면 최대 2시간 이내의 마지막 정상자료를 `fallback: true`로 명확히 표시합니다. 실황 조회 실패는 시간별·3일 예보 응답에 영향을 주지 않습니다.

`oneHourPrecipitation` 누락은 `"-"`, `precipitationType` 누락은 `"정보 없음"`입니다. 이를 0mm·비 없음·맑음으로 대체하면 안 됩니다. 같은 격자라도 요청 지역명이 다른 관측 캐시와 fallback은 구분합니다. 비정상 기온·습도·풍속은 정상 관측으로 반환하지 않습니다.

## 대기질 보조 필드 (`WeatherDto`)

| 필드 | 의미 |
| --- | --- |
| `pm10Value`, `pm25Value` | 에어코리아 1시간 농도. 결측·장비점검은 `"-"` |
| `pm10Grade`, `pm25Grade` | 해당 농도와 같은 1시간 등급(`pm10Grade1h`/`pm25Grade1h`). 1~4; 원천에 없으면 빈 문자열. 24시간 등급을 대체 사용하지 않음 |
| `airQualityObservedAt` | 측정소 관측시각, `+09:00` 오프셋 포함. 없으면 `"-"` |
| `airQualityStationBasis` | `LOCATION_MATCH`: 지역명 일치 측정소, `REGIONAL_REFERENCE`: 지역 대표 측정소, `UNAVAILABLE`: 확인 불가. 물리적 최단거리 측정소라는 뜻은 아님 |

값과 등급은 독립적으로 누락될 수 있습니다. 등급을 농도에서 임의 추정하거나 일부 '좋음'만으로 전체 공기질을 좋다고 표시하지 않습니다. 한 항목의 확인된 나쁨 경고는 다른 항목의 누락 때문에 지우지 않습니다. 원천 필드 구분은 [공공데이터포털 에어코리아 API 명세](https://www.data.go.kr/data/15073861/openapi.do)를 따릅니다.

## `GET /api/weather/hourly`

```http
GET /api/weather/hourly?nx=61&ny=125&locationName=강남역&dayOffset=0
```

`dayOffset`은 `0`(오늘)·`1`(내일)·`2`(모레)를 지원하며 한국 날짜(`Asia/Seoul`)를 기준으로 계산합니다. `forecastDate`는 `yyyy-MM-dd`, `hours[].time`은 `HH:mm` 형식입니다. API는 선택 날짜에 확보한 예보를 반환하고, 웹과 메일은 KST 현재 분보다 지난 시각을 앞으로의 예보에서 제외합니다.

| 필드 | 타입·결측 계약 |
| --- | --- |
| `minimumTemperature`, `maximumTemperature` | 정수 또는 `null` |
| `maximumPrecipitationProbability` | 정수 또는 `null` |
| `hours[].temperature`, `hours[].precipitationProbability`, `hours[].humidity` | 정수 또는 `null` |
| `hours[].windSpeed` | 숫자 또는 `null` |
| `hours[].sky`, `hours[].precipitation` | 상태 문구; 원천 누락이면 `자료 없음` |
| `provenance` | 원천 출처·예보 발표·자료 조회 시각·완전성·fallback 여부 |

`null`은 자료가 없다는 뜻입니다. 0°C·0%·무풍·맑음으로 대체해서는 안 됩니다. 실제 0값은 유효한 자료이므로 일반적인 falsy 판정과 구별해야 합니다. 일부 시간의 강수확률이 없으면 확인된 값만 요약하되 모든 시간의 최대값인 것처럼 표시하거나 비가 오지 않는다고 판단하지 않습니다.

### 웹 지역 비교와 캐시

지역 비교는 기존 `/api/weather/hourly`만 사용합니다. 최대 3지역, 명시적 조회 시에만 호출하며 정확한 `forecastDate`·`hours[].time`이 맞아야 표시합니다. 누락된 시간을 가까운 시간으로 보간하지 않습니다. 지역별 오류와 부분 성공을 별도로 표현합니다.

서버 캐시 키는 격자·지역명·상대 날짜와 한국 기준 절대 예보일을 포함합니다. 브라우저는 `forecastDate=yyyy-MM-dd` 조회 매개변수로 한국 날짜별 HTTP/메모리 캐시를 나눕니다. 이 매개변수는 캐시 구분용이며 실제 조회 날짜 선택은 기존 `dayOffset` 계약입니다. fallback은 요청 날짜 원천자료가 있는 경우에만 허용합니다. 로컬 저장 예보는 같은 지역·예보일·저장 후 3시간 이내일 때만 사용합니다.

서비스워커는 API·계정 HTML·인증 쿼리를 캐시하지 않으며 공개 자산 allowlist만 저장합니다. 오프라인 문서는 개인정보 없는 `/offline.html`입니다.

## 날씨 편지의 표현 계약

웹의 `weather-letter.js`와 메일의 `WeatherMailBriefingDto`는 기존 DTO에서 한 문장·기온·강수·공기질을 편집하며 모델 요청이나 새 뉴스 API를 사용하지 않습니다. 메일은 대표 예보와 `forecastDate`가 같은 시간별 자료만 결합하고 발송 분 이후 최대 6개를 시간순으로 표시합니다. 지난 시각의 대표 예보에 비가 있었다는 이유로 앞으로 우산이 필요하다는 헤드라인을 만들지 않습니다. 대표 예보의 기준시각은 그대로 표시합니다.

메일 날짜에 따라 오늘·내일·모레 표현을 구분합니다. 실제 내일 예보가 없으면 내일 변화 내용을 생성하지 않습니다. 예보 유효시각, 원천 발표시각, 애플리케이션 조회시각을 구분하며 원천의 `Instant`·오프셋 시각은 KST로 표시합니다. 시간별 조회 실패는 확인된 대표 예보를 지우지 않고 해당 부분만 이용 불가로 안내합니다. 메일은 표·인라인 스타일의 HTML과 일반 텍스트를 `multipart/alternative`로 구성합니다.

웹은 관측·일별·시간별 요청을 독립적으로 처리합니다. 지역/날짜가 변경되면 요청을 취소하고 응답 버전과 지역을 확인해 이전 결과·오류·로딩 종료가 현재 화면을 덮지 않게 합니다. 상세 플래너는 펼친 뒤 조회하며, 같은 지역의 유효 결과를 재사용합니다. 이 동작은 프런트엔드의 상태 처리 계약이며 새로운 API 엔드포인트를 추가하지 않습니다.

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

`GET /api/users/me/delivery`는 로그인한 계정의 구독만 조회하며, 다음 정기 메일 예정시각(`nextScheduledAt`, ISO 8601, `+09:00`), 시간대(`Asia/Seoul`), 최근 발송 시도 5건의 시각·성공 여부·위치를 반환합니다. 원문 이메일이나 내부 발송 오류 문구는 응답에 포함하지 않고 `Cache-Control: no-store`를 적용합니다. 스마트 위험 알림은 조건부로 발송되므로 다음 정기 메일 예정시각에 포함하지 않습니다.

## 로그인 이메일 소유권 인증

운영 환경에서는 Coders 인증 게이트웨이가 전달하는 `X-Coders-User` UUID를 구독 소유자로 사용합니다. 게이트웨이는 원문 계정 이메일을 애플리케이션에 전달하지 않으므로, 구독 화면에서 이메일 소유권 인증을 한 번 완료해야 합니다.

1. 로그인 상태에서 `POST /api/users/email-verification/request`를 호출합니다.

   ```json
   { "email": "me@example.com" }
   ```

   응답은 인증 대상 이메일과 만료시각을 반환합니다. 메일에는 링크 대신 6자리 숫자만 표시됩니다. 같은 로그인 계정은 보안을 위해 30초에 한 번만 재전송할 수 있습니다.

2. 메일의 6자리 번호를 화면에 입력하고 `POST /api/users/email-verification/confirm`을 15분 안에 호출합니다.

   ```json
   { "email": "me@example.com", "code": "042731" }
   ```

   번호가 일치하면 `200 OK`와 함께 인증 완료 메시지를 반환합니다. 틀린 번호는 `400 INVALID_REQUEST`로 거절하며, 한 챌린지에서 5회 틀리면 새 번호를 요청해야 합니다. 새 번호를 요청하면 이전 번호는 즉시 무효화됩니다. 30초 안에 재전송하면 `429 VERIFICATION_COOLDOWN`과 남은 대기 초를 담은 `Retry-After` 헤더를 반환합니다.

3. 같은 6자리 번호를 `POST /api/users/subscribe`의 `verificationCode`로 전달합니다.

   ```json
   {
     "email": "me@example.com",
     "verificationCode": "042731",
     "privacyConsent": true
   }
   ```

운영 구독 요청은 `email`과 `verificationCode`를 정확히 한 개씩 요구하고, 인증 챌린지의 이메일과 요청 이메일이 다르면 거부합니다. 인증번호는 서버에 SHA-256 해시로만 저장하고 구독에 사용한 즉시 폐기합니다. 이메일 인증 챌린지에는 `ownerId`가 함께 저장되므로 다른 로그인 계정이 번호를 재사용할 수 없습니다. 이미 발송된 구버전 링크를 처리하기 위해 `GET /api/users/email-verification/confirm?token=...`과 `verificationToken`은 호환 기간 동안만 유지합니다.

로컬 보완에서 새 번호의 해시는 `ownerId`·정규화한 이메일·6자리 번호를 함께 포함합니다. 확인과 구독 소비는 해당 소유자·이메일의 챌린지를 조회한 뒤 번호를 비교하며, 전역 코드 해시 검색을 하지 않습니다. 서로 다른 계정이 우연히 동일한 6자리 번호를 받아도 서로의 챌린지를 승인할 수 없습니다. 개발 모드의 익명 소유자도 요청과 구독 소비에서 같은 정규화 이메일 기반 소유자를 사용합니다. 이미 발급된 구버전 번호의 호환 비교도 계정·이메일 범위 및 기존 만료·시도횟수 제한을 유지합니다. 구버전 GET 링크 경로에 6자리 번호를 넣어서 인증하는 것은 허용하지 않습니다.

## 로컬 검증 체크리스트

```powershell
.\gradlew.bat clean test bootJar --no-daemon
node scripts/test-weather-letter.cjs
node scripts/test-weather-state.cjs
```

- [ ] 정상 0값과 `null`·`"-"`·자료 없음의 구별 및 `-1`·`UNAVAILABLE` 추천 제외
- [ ] UTC 실행 환경의 KST 자정, 지난 분 제외, 같은 날짜의 예보 결합
- [ ] 빠른 지역/날짜 변경, 늦은 성공·실패·finally, 개별 공급자 실패와 플래너 지연 조회
- [ ] 소유자·이메일·번호가 서로 다른 인증, 같은 번호를 받은 여러 계정, 구버전 코드 호환
- [ ] 메일의 HTML·일반 텍스트 대체본문과 날짜별 문구, 긴 주소·비·밤·누락·부분 실패 검사본
- [ ] `360×800`·`390×844`·`768×1024`·`1440×900` 렌더와 실제 API 흐름·키보드·콘솔 확인

Node 스크립트는 실제 편집 함수와 브라우저 조회 함수를 네트워크 없이 검사합니다. 실제 브라우저·메일 수신 클라이언트·운영 배포 검증을 대체하지 않습니다. 완료 여부와 결과는 별도 검증 기록으로 확인해야 합니다.
