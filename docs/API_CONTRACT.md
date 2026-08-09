# 기관 연계 API 계약

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
