# 운영 런북

## 상태 확인

| 목적 | 경로 | 기대값 |
| --- | --- | --- |
| 기본 상태 | `/actuator/health` | HTTP 200, `UP` 또는 fallback 가능한 `DEGRADED` |
| 생존 확인 | `/actuator/health/liveness` | HTTP 200 |
| 트래픽 수신 준비 | `/actuator/health/readiness` | DB·기상 API 구성 정상 |
| Prometheus | `/actuator/prometheus` | `X-Admin-Key` 인증 후 노출 |

주요 지표는 `external.api.calls`, `weather.planner.generation`, HTTP 서버 요청시간과 JVM·DB pool 지표입니다.

## 기상 원천 장애

1. 응답의 `provenance.fallback`과 `X-Data-Freshness`를 확인합니다.
2. `external.api.calls{provider="kma-forecast"}` 실패와 circuit-open 증가를 확인합니다.
3. 기상청 API허브 공지와 인증키 호출한도를 확인합니다.
4. 서비스는 2시간 이내 마지막 정상자료가 있으면 `STALE_FALLBACK`으로 명시해 제공합니다.
5. 2시간을 초과하면 오래된 정보를 정상 예보처럼 제공하지 않고 503으로 실패합니다.

## 배포와 롤백

1. PR의 test, container, dependency-review 검사를 모두 통과시킵니다.
2. 생성된 SBOM을 배포 산출물과 함께 보관합니다.
3. Flyway migration과 애플리케이션을 배포합니다.
4. readiness, 기관용 briefing, 위치 검색, 구독 validation을 smoke test 합니다.
5. 오류율 또는 지연이 기준을 넘으면 직전 이미지로 롤백합니다. 적용된 DB migration은 기본적으로 전진 수정합니다.

## 백업·복구

- PostgreSQL은 운영 플랫폼의 자동 백업과 별도 복구 리허설이 필요합니다.
- Redis는 재생성 가능한 rate limit 데이터만 저장하며, 300ms 이상 지연되면 인스턴스별 로컬 제한기로 전환합니다.
- 날씨·장소 캐시는 bounded Caffeine을 사용해 외부 캐시 장애를 요청 경로에서 격리합니다.
- 분기 1회 별도 환경에서 복원시간과 데이터 완전성을 기록합니다.
- secret은 저장소에 넣지 않고 운영 secret store에서 회전합니다.

## 개인정보 요청

- 구독 해지는 발송만 중지합니다.
- `DELETE /api/users/me/data`는 로그인 소유권을 확인한 뒤 구독정보와 메일 발송이력을 함께 삭제합니다.
- 삭제 작업은 원문 이메일을 로그에 남기지 않고 내부 사용자 ID만 기록합니다.
