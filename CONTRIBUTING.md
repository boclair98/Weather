# Contributing

날씨한편은 작은 변경도 사용자 판단과 예약메일에 영향을 줄 수 있으므로 아래 흐름을 따릅니다.

## 개발 흐름

1. `main`에서 짧은 기능 브랜치를 만듭니다.
2. 제품 행동이 바뀌면 단위/계약 테스트와 문서를 함께 수정합니다.
3. `./gradlew clean test bootJar --no-daemon`을 실행합니다.
4. 비밀값, 실제 이메일, 정확한 사용자 위치가 diff에 포함되지 않았는지 확인합니다.
5. Pull Request에 사용자 가치, 실패 시나리오, 검증 결과를 기록합니다.

## 코드 원칙

- 외부 API 호출에는 timeout과 실패 격리 경계를 둡니다.
- DTO의 계산 규칙은 결정론적으로 유지하고 경계값 테스트를 작성합니다.
- 프런트는 서버 응답의 판단 기준을 재구현하지 않습니다.
- API 필드는 가능한 한 하위 호환으로 추가하고 계약 변경은 `docs/API_CONTRACT.md`에 기록합니다.
- 로그에 API 키, SMTP 자격증명, 이메일 전문을 남기지 않습니다.
- 측정하지 않은 성능 수치를 README의 성과로 표현하지 않습니다.

## 로컬 확인

```powershell
.\gradlew.bat clean test bootJar --no-daemon
.\gradlew.bat bootRun
```

실제 외부 연동이 필요하면 `.env.example`을 참고해 로컬 환경변수로만 키를 주입하세요.
