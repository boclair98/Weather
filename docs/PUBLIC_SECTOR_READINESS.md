# 공공기관 도입 준비도

## 현재 판단

이 저장소는 공공기관 대상 기술검토와 시범사업(PoC)에 제출할 수 있는 수준을 목표로 합니다. 다만 저장소와 운영 URL만으로 기상청의 공식 서비스, 납품 완료 제품, 조달 등록 제품 또는 인증 획득 제품이라고 주장하지 않습니다.

제품 구매·본사업 도입에는 코드 외에 발주기관 요구사항, 데이터 이용 승인, 보안성 검토, 웹 접근성 품질인증, 성능시험, 개인정보 검토, SLA와 유지보수 계약이 필요합니다.

## 코드로 확보한 항목

| 영역 | 구현 상태 |
| --- | --- |
| 데이터 계보 | 기상청 출처, 발표시각, 수집시각, 완전성, 신선도, fallback 여부 제공 |
| 안정성 | 외부 API timeout, 1회 재시도, circuit breaker, 2시간 이내 마지막 정상자료 fallback |
| API 계약 | `/api/v1/weather/briefing`, 스키마 버전, 품질·출처·신선도 헤더, 요청 추적 ID |
| 보안 | 요청별 CSP nonce, HSTS, COOP/CORP, 관리자 API 보호, 입력 검증 |
| 확장성 | Redis 분산 rate limit, Redis 캐시, PostgreSQL, ShedLock, bounded executor |
| 변경 통제 | Flyway 스키마 버전 관리, PR 테스트, 컨테이너 빌드, dependency review |
| 공급망 | 컨테이너 CycloneDX SBOM 생성, Gradle wrapper 검증 |
| 개인정보 | 명시적 동의, 동의 버전·시각 기록, 구독 해지, 메일 이력 포함 완전삭제 |
| 관측성 | Prometheus, health/readiness/liveness, 외부 API·플래너 지표, request ID |
| 접근성 기반 | 키보드 탐색, 상태 알림, reduced motion, 모바일 반응형, CSP 적용 |

## 외부 검증이 필요한 도입 게이트

1. KWCAG 2.2 기준 전문가·사용자 심사와 WA 품질인증
2. 발주기관의 소프트웨어 보안약점 진단 및 모의침투시험
3. 실제 목표 트래픽으로 독립 성능시험과 장시간 soak test
4. PostgreSQL 백업·복구 리허설 및 기관이 승인한 RPO/RTO 확정
5. 개인정보 처리방침 법무 검토, 처리위탁·국외이전 여부 확인
6. 기상청 API 기관회원 인증키, 호출량, 공공누리 표시조건 확인
7. 필요 시 GS 인증, CSAP 환경, ISMS-P 또는 기관별 보안성 검토
8. 장애·변경·취약점 대응시간을 포함한 SLA와 유지보수 인력 계약

## 제안 SLO

아래 수치는 계약 전 부하시험으로 확정해야 하는 목표값입니다.

- 월간 API 가용성 99.9%
- 캐시 적중 브리핑 p95 800ms 이하
- 원천 API 콜드 브리핑 p95 5초 이하
- 오류율 1% 미만(잘못된 사용자 요청 제외)
- 장애 감지 5분, 1차 대응 15분
- 데이터베이스 RPO 15분, RTO 60분

## 공식 기준 참고

- 기상청 API허브: https://apihub.kma.go.kr/
- 웹 접근성 품질인증: https://wa.or.kr/
- Spring 지원정책: https://spring.io/support-policy
- GitHub dependency review: https://docs.github.com/en/code-security/concepts/supply-chain-security/dependency-review
