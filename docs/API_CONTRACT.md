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

## 로그인 이메일 소유권 인증

운영 환경에서는 Coders 인증 게이트웨이가 전달하는 `X-Coders-User` UUID를 구독 소유자로 사용합니다. 게이트웨이는 원문 Google 이메일을 애플리케이션에 전달하지 않으므로, 구독 화면에서 이메일 소유권 인증을 한 번 완료해야 합니다.

1. 로그인 상태에서 `POST /api/users/email-verification/request`를 호출합니다.

   ```json
   { "email": "me@example.com" }
   ```

   응답은 인증 대상 이메일과 만료시각을 반환합니다. 메일에는 링크 대신 6자리 숫자만 표시됩니다.

2. 메일의 6자리 번호를 화면에 입력하고 `POST /api/users/email-verification/confirm`을 15분 안에 호출합니다.

   ```json
   { "email": "me@example.com", "code": "042731" }
   ```

   번호가 일치하면 `200 OK`와 함께 인증 완료 메시지를 반환합니다. 틀린 번호는 `400 INVALID_REQUEST`로 거절하며, 한 챌린지에서 5회 틀리면 새 번호를 요청해야 합니다. 새 번호를 요청하면 이전 번호는 즉시 무효화됩니다.

3. 같은 6자리 번호를 `POST /api/users/subscribe`의 `verificationCode`로 전달합니다.

   ```json
   {
     "email": "me@example.com",
     "verificationCode": "042731",
     "privacyConsent": true
   }
   ```

운영 구독 요청은 `email`과 `verificationCode`를 정확히 한 개씩 요구하고, 인증 챌린지의 이메일과 요청 이메일이 다르면 거부합니다. 인증번호는 서버에 SHA-256 해시로만 저장하고 구독에 사용한 즉시 폐기합니다. 이메일 인증 챌린지에는 `ownerId`가 함께 저장되므로 다른 로그인 계정이 번호를 재사용할 수 없습니다. 이미 발송된 구버전 링크를 처리하기 위해 `GET /api/users/email-verification/confirm?token=...`과 `verificationToken`은 호환 기간 동안만 유지합니다.
