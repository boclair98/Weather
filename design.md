# 날씨한편 UI·제품 설계 계획

## 2026-11-22 · 처음 써도 편하고, 계속 보고 싶은 날씨한편

### 제품 목표와 사용자 여정
- 서비스: 한국 사용자가 동네·역·건물의 현재 관측과 시간별·일별 예보를 확인하고, 원하면 인증한 이메일로 날씨를 받는 생활 날씨 서비스.
- 우선 사용자: 외출 전 휴대폰이나 PC에서 “지금 어떤지, 비가 언제 올지”를 빠르게 알고 싶은 사람. 처음 방문한 사용자도 기능 이름을 몰라도 다음 행동을 찾을 수 있어야 한다.
- 첫 행동: 위치 입력에 동네나 역을 적어 검색한다. 결과가 여러 개면 정확한 장소를 고르고, 하나면 기존 동작대로 바로 선택된다. 위치 권한은 ‘현재 위치’를 직접 눌렀을 때만 요청한다.
- 완료 결과: 선택 장소의 관측·예보를 출처와 시각을 구분해 확인한다. 지역 저장·비교·선택형 생활 정보·이메일 구독은 핵심 확인 뒤에 이어갈 수 있다.
- 상태 흐름: 첫 방문/최근 지역 → 검색 중·검색 결과 없음·오류 → 지역 선택 → 현재 관측 → 시간별 예보 → 일별·대기질 → 선택형 생활 정보·이메일.
- 현상 유지와 대안: 모든 도구를 한 화면에 계속 노출하는 대신, 날씨 본문은 첫 화면에서 명료하게 읽히고 비교·설정·일정 도구는 필요할 때만 펼치는 한 가지 ‘친근한 생활 기상대’ 방향을 채택한다. 새 기능·외부 API·프레임워크는 추가하지 않는다.
- 확인된 사실: 기존 템플릿은 위치 선택, 실제 날씨·공기질, 시간대 상세, 비교, 선택형 외출 정보, 이동 경로와 이메일 인증/구독을 제공한다. 보조 모듈 일부는 이미 접혀 있지만 검색 안내·활성 날씨·툴바가 같은 화면에서 서로 경쟁할 여지가 있다.
- 미확인: 실제 사용자 사용성/선호 조사, 운영 로그의 전환·검색 실패율, 실기기 접근성. 이 계획은 코드와 공개 화면을 바탕으로 한 설계 가설이며 사용자 검증 결과로 표현하지 않는다.

### 확정 방향과 기준
- 한 문장 가치: “우리 동네 날씨, 지금부터 함께 살펴봐요.” 재미는 게임화 점수나 장식 대신, 날씨 자료에 근거한 친근한 한 줄 설명과 누를 때 바로 반응하는 작은 상태 변화로 만든다.
- 최우선 행동: 미선택 화면의 `지역 검색` → `openLocationSearch()`/검색 필드, 또는 사용자가 누른 `현재 위치` → 기존 위치 권한 요청. 장소가 선택되면 `시간별 예보 보기` → `#hourlyWeather`가 가장 가까운 다음 행동이다.
- 보조 행동: 현재 지역 저장·새로고침, 최근·저장 지역 근처의 비교, 보기 설정, 접힌 생활 정보, 이메일 미리보기·구독. 지역 비교는 위쪽 지역 전환과 같은 자리에 한 번만 둔다. 이메일은 플랫폼 로그인과 본인 이메일 인증 흐름을 그대로 보존한다.
- 말투: 쉬운 한국어, 한 번에 한 가지 안내. 관측과 예보·측정값과 등급·오류와 복구를 구분하고 기술 내부 오류 문구 대신 다음 행동을 알려준다. 과장·가짜 수치·임의 안전 보장은 금지한다.
- 정보 순서: 지역 → 현재 기온·상태·관측시각 → 오늘의 핵심 변화 → 시간별 예보 → 아침·낮·저녁/일별 → 대기질 → 접힌 생활 정보·고급 도구 → 이메일. 현재값과 예보를 같은 측정처럼 섞지 않는다.
- 화면 구조: 기존 `index.html` 단일 여정과 Thymeleaf/API를 유지한다. 선택 전에는 지역 검색·현재 위치·저장/최근 장소가 첫 화면이며, 선택 후 검색은 헤더/지역 선택으로 축소되고 현재 날씨와 예보가 위로 올라온다. 시간별만 의도적으로 가로 스크롤한다.
- 디자인 토큰: 기존 관측소 스타일을 재사용한다. 페이지 `#f5f8fc`, 표면 `#fff`, 본문 `#142c42`, 보조 `#526779`, 상호작용 `#2377ae`, 구분선 `#dfe8f0`, 맑음 강조 `#ecb43c`. 새 색상 세계를 추가하지 않는다.
- 타이포: Pretendard Variable + system Korean fallback. 브랜드 20–22px, 지역 24–30px, 현재 기온 58–88px 유동형, 섹션 19–22px, 본문 15–16px/1.55–1.7, 보조 12–14px. 수치는 tabular numerals, 단위는 분리되지 않게 둔다. 값은 기존 CSS 토큰·실제 화면을 보고 조정한다.
- 정렬/밀도: 기존 최대 폭 약1160px와 16–24px 외곽 간격을 보존. 날씨 정보는 의미 단위로 묶고 중첩 카드·반복 테두리를 늘리지 않는다. 검색 입력·주요 동작은 최소44px 높이로 유지한다.
- 모바일: 360px부터 한 열, 현 위치·기온을 먼저 보여주고 다음에 시간 레일을 둔다. 선택 후 장소 검색은 모바일 화면 전체를 차지하는 단일 스크롤 화면으로 열어 검색 입력·닫기·결과의 조작 위치를 고정하고, 하단 빠른 메뉴는 검색 중 숨긴다. 낮은 viewport/키보드에서 하단 메뉴가 접혀도 지역 전환 메뉴 안의 `다른 지역 검색`으로 진입할 수 있다. 검색 결과 영역이 줄어들며 닫기와 입력을 유지하고 safe-area를 반영한다. 첫 방문의 인라인 검색은 기존 흐름을 보존한다.
- 모션: 기존 상태가 먼저 보이고, 짧은 100–180ms의 누름/열기 피드백만 유지한다. 예보를 읽기 전에 기다리는 모션·자동 순환·주의를 빼앗는 효과는 쓰지 않고 `prefers-reduced-motion`을 지킨다.
- 아이콘·자산: 현재 실제 날씨 상태를 나타내는 기존 SVG만 재사용한다. 새 인물·사진·브랜드 자산·가상 날씨 이미지는 추가하지 않는다.

### 실제 화면 참고와 적용 범위
- 주 참고: [네이버 날씨](https://weather.naver.com/) · 2026-11-22 확인 · 라이브 화면에서 데스크톱 1280×720을 직접 관찰했다. 지역명·현재 관측·다음 날 예보를 구분하고 보조 측정과 시간별 예보를 정렬하는 정보 순서를 참고한다. 390×844 viewport에서도 열었으나 본문이 화면 왼쪽 약 110 CSS px에 압축되어 보여 모바일 레이아웃 참고로 채택하지 않았다. 날씨한편은 모든 측정값을 한 줄에 압축하거나 지도·광고 영역을 복제하지 않는다. Naver의 로그인 화면·실기기 키보드·터치 동작은 이번 관찰 범위 밖이다.
- 보조 참고: [K-WEATHER 모바일 정보](https://kweather.co.kr/data/mobile/index.html) · 2026-11-22 확인 · 공식 제품 소개 페이지를 데스크톱 1252×720에서 읽고 아래로 스크롤해 앱 화면 이미지를 관찰했다. 이미지에는 관심지역 검색·저장과 3일/시간별 예보가 별도 화면으로 제시되어 있어 지역 선택과 날씨 읽기를 분리하는 근거로 사용한다. 이는 앱의 정적 공식 소개 이미지이며 현재 모바일 앱의 인터랙션·반응형·접근성을 시험한 것이 아니다. 회사 랜딩, 팝업·광고, 화면 이미지·브랜드 자산·계정 기능은 가져오지 않는다.
- 선별 UI 풀/세계 확장 풀: `selected-ui-patterns.md`, 분야 색인과 `global-design-atlas.md`를 다시 확인했다. 생활 검색은 Airbnb가 선별 사례지만, 이번 검증은 실제 날씨 첫 행동에 집중하므로 화면 요소를 그대로 쓰지 않고 Naver 데스크톱의 관측·예보 계층만 주 참고로 채택한다. 세계 확장 풀의 포트폴리오·아트 디렉터리 사례는 이 과업과 맞지 않아 보류했다.
- 현재 사이트 직접 접근: `weather.coders.kr` 확인 시 IAB 탐색이 시간 초과되어 운영 화면을 다시 확인하지 못했다. 기존 로컬 템플릿/미커밋 화면 캡처는 확인했지만 그 화면과 현재 운영판을 동일하다고 단정하지 않는다.

### 상태와 기능 계약
- 유지: 위치 검색·현재 위치·최근/저장 장소, 현재/시간별/일별 날씨, 시간별 상세·3지역 비교, 대기질·특보·자외선·꽃가루의 독립 부분 실패, 선택형 외출/경로 도구, 메일 미리보기, 본인 인증·구독 관리·해지, 섭씨/화씨·테마·인쇄·PWA.
- 오류/복구: 검색어를 실패 뒤 보존하고 구체적인 검색 방법과 재시도 버튼을 제공한다. 대기질이 안 와도 유효한 날씨를 유지하고 해당 데이터만 정보 없음으로 표시한다. 새 위치 선택 뒤 이전 관측값을 함께 보이지 않는다. 로컬 저장이 실패해도 검색/날씨 조회는 진행하되 저장은 실패했다고 분명히 말한다.
- 이메일: 로그인 → 이메일 입력 → 6자리 번호 확인 → 알림 시간 확인 순서와 서버 검증을 보존한다. 첫 구독을 위해 필수 옵션을 최소한으로 설명하되 동의·보안 단계를 숨기거나 인증을 건너뛰지 않는다.
- 이번 재검토 구현 범위: 선택 후 지역 검색 dialog의 모바일 전체 화면 단일 스크롤/하단 빠른 메뉴 정리, 축소 viewport에서 검색 진입 경로, 닫기 후 포커스 복귀만 수정한다. 첫 방문 인라인 검색, 지역 검색 API/선택 계약, 지역·예보 데이터, 이메일 인증·구독, 다크/라이트 테마는 보존한다. API 계약·개인정보·배포 설정은 바꾸지 않는다.

### 수용 기준과 검증 계획
- 미선택: 첫 화면의 검색 입력·검색·현재 위치·최근/저장 장소가 바로 이해되고, 동작 없는 안내/가짜 날씨가 없다. 검색 정상·검색 결과 없음·API 오류에서 질의와 다음 행동이 보인다.
- 선택: 현재 관측과 시간별 예보가 같은 지역을 가리키고, 오늘의 요약은 실제 예보에서 나온다. 관측·예보 기준 시각과 대기질 실패를 정확히 나타낸다.
- 조작: 지역 전환·비교·시간별 상세·보기 설정·구독 안내·폼은 실제 핸들러/API로 연결된다. Tab/Shift+Tab/Enter/Space/Escape, 검색 시트 닫힘과 포커스 복귀를 확인한다.
- 크기: 360×800, 390×844, 768×1024, 1440×900. 페이지/dialog overflow, 긴 한글 지역명, 낮은 검색 높이, 모바일 검색과 하단 메뉴, 라이트/다크, 키보드 포커스와 reduced-motion을 살핀다.
- 실행: 전체 Gradle 회귀와 프런트 계약 테스트 후 로컬 브라우저에서 검색 열기/닫기, 검색·선택, 키보드가 열린 낮은 viewport 근사, 예보 로딩, 빈/오류 상태를 확인한다. 운영 SSO·실제 메일·실기기·트래픽은 로컬 확인으로 가장하지 않는다.
- 출고 권한: 이번 요청은 Coders.kr 재배포를 명시적으로 승인한다. 승인된 순서는 검증 → canonical upstream commit/push → 실제 포크 동기화 → 기존 Coders 프로젝트 배포 → 운영 URL 확인이며, 비밀값은 저장소에 넣지 않는다.

### 독립 검토와 이번 수정 결정
- 현재 브라우저 390×844에서 장소 검색은 modal 바깥과 결과 목록 두 곳에서 세로 스크롤되고, dim 배경 뒤로 고정 빠른 메뉴가 보인다. 390×460에서는 빠른 메뉴가 접히지만 지역 전환 메뉴에도 검색 진입이 없었다. 단일 검색 스크롤, 짧은 화면용 진입점, 호출한 조작으로의 포커스 복귀를 추가해 키보드 높이가 달라져도 검색 흐름을 이어간다. 기존 앱 실행·API 응답·이메일 경로에는 손대지 않는다.
- 독립 검토는 모바일에서 비교가 위쪽에서 사라질 수 있고 구독 문장이 인증 안내를 반복한다는 점을 짚었다. 비교는 지역 전환 근처에 한 번만 두고, 구독 설명은 기본 아침 알림에서 필요한 시간·위험 알림을 더하는 순서로 바꿨다.
- 예보 한 줄은 기존 실제 강수 자료 규칙과 임계값을 유지하고, 강수확률이 낮을 때와 비·눈 가능성이 있을 때의 문장만 쉬운 한국어로 다듬었다. AI 호출·외부 API·추정값은 추가하지 않았다.
- 최신 소스 오버라이드 로컬 화면을 360×800·390×844·768×1024·1440×900에서 캡처해 비교했다. 가로 넘침은 없었고, 모바일 검색 결과 없음에서도 질의가 남으며 검색·현재 위치·닫기 조작이 구분됐다. 390×500의 키보드 높이 근사 화면에서 하단 메뉴가 검색 버튼을 가리는 것을 발견해 낮은 모바일 화면에서 메뉴를 접도록 수정·재확인했다. 실제 모바일 키보드가 열린 기기 검증은 하지 않았다.
- 밝고 어두운 테마를 데스크톱·모바일에서 확인하고 다시 밝은 테마로 복구했다. 실제 지역 검색 결과에서 서울역을 선택한 뒤 기상청·에어코리아 데이터, 내일 전환·오늘 복귀, 강수 요약, 시간별 상세 모달을 확인했다. 검색/상세 닫기는 Escape로 동작했고 포커스가 복귀했다. 저장 지역 토글은 로컬 미리보기에서 켰다가 꺼 원상 복구했다.
- 메일 미리보기의 초기 문구에서 ‘맞춤 브리핑’처럼 실제로 확인되기 전의 결과를 약속하는 표현을 없애고, 지역 선택·자료 부족 상태를 사용자가 다음 행동을 알 수 있게 바꿨다. 시간대별 안내·검색 빈 결과·실패 문구를 다시 점검했다.
- 로컬 메일 미리보기에서 1°C·습도 80%인데 여름용 소재가 안내되는 우선순위 오류를 확인했다. 체감 기온이 5°C 이하이면 보온 소재를 우선하고, 고온·고습 조건에만 통풍 소재 문구를 적용하도록 규칙을 조정했으며 겨울·온화·고온 조건 회귀 테스트를 추가했다. 이 로직은 기상 입력에 따른 고정 계산이며 외부 모델 호출은 발견되지 않았다.
- 로그인하지 않은 구독 안내의 문구·단계를 확인했지만 SSO 로그인, 인증번호 요청, 구독 저장, 실제 이메일 발송은 하지 않았다. 운영 사이트 현재 화면·실기기·부하·배포는 이번 검증 범위가 아니다.

## 2026-10-25 기능 고도화: 자주 보는 지역과 같은 시간 비교

### Resolved inputs
- Topic: 지역을 반복해서 확인하고 같은 시각의 날씨를 비교하는 날씨한편.
- Offer: 실제 관측·예보, 최대 3지역의 같은 날짜·시간 비교, 시간별 상세와 본인 이메일 구독.
- Audience: 집·직장·방문할 장소의 날씨를 짧게 확인하는 한국 사용자.
- Primary user action: 선택한 지역의 날씨 확인. 보조 동작은 관심지역 전환과 지역 비교.
- Deliverable: 기존 Thymeleaf index.html, weather-observatory.css, 순수 weather-workspace.js 및 격리 가능한 비교 UI 모듈과 테스트.
- Style direction: 기존 Airy weather observatory 유지. 정보가 늘어도 한 화면에 모든 도구를 늘어놓지 않는다.
- Tonal key: 기존 Light/선택형 Dark, 하늘빛·짙은 슬레이트.
- Icon policy: 기존 Topic-native SVG 유지. 새 장식 자산 없음.
- Proof assets: 실제 /api/weather/hourly·current·daily 응답. 날짜·시각이 맞는 자료만 사용.
- Motion intensity: Minimal. 기존 150~220ms 조작 피드백과 reduced-motion 유지.

### Composition contract
- Hero archetype: asymmetrical, 기존 지역·기온 중심 구성을 보존.
- First-viewport structure: 기존 헤더 아래 관심지역 바로가기 한 줄(모바일 자연 줄바꿈), 현재 날씨, 날짜와 시간별 예보. 비교 표는 명시적으로 열었을 때만 표시.
- Primary visual/proof artifact: 같은 예보 유효 날짜·시각을 맞춘 최대 3지역의 기온·강수확률·습도·풍속. 누락 시간을 인접 시간으로 대체하지 않음.
- Interaction behavior: 지역 비교 → 접근 가능한 native dialog → 최근/관심지역 2~3곳 선택 → 날짜·시각 선택 → 조회. 닫기/선택 변경 시 이전 요청을 무효화하고 중단. 각 지역의 실패는 다른 지역의 성공을 지우지 않음. 시간별 항목은 키보드/터치로 상세를 열 수 있음.
- Reuse boundary: 한 디자인. API/인증/메일 계약 및 이전 디자인 유지, 신규 외부 API나 프레임워크 없음.

### 진단과 구현 계약
- localStorage 차단 시 초기화·지역 선택·단위 변경이 중단되는 경로를 안전하게 처리한다. 손상되거나 좌표가 유효하지 않은 저장 지역은 제외한다.
- 마지막 지역은 이 기기에만 보관하고 재방문 시 오늘의 새 자료를 조회한다. 공유 링크/인증 후 초안이 있으면 우선한다. 오래된 측정값을 현재 값으로 자동 표시하지 않는다.
- 같은 offset의 다른 날짜 스냅샷을 재사용하지 않도록 한국 날짜와 유효기간을 확인한다.
- 현재위치 탐색과 수동 검색의 경합을 막는다. GPS는 명시적인 현재 위치 클릭에서만 사용.
- 관심지역/최근지역을 합쳐 중복을 제거하고 비교는 최대 3요청, 클릭 시에만 조회한다. 데이터 캐시 상한·만료와 늦은 응답 방어를 유지한다.
- 사용자가 저장을 요청했으나 브라우저 저장소가 막히면 성공을 가장하지 않고 안내한다.
- UI 토큰: 기존 #f5f8fc 배경/#ffffff 표면/#142c42 본문/#526779 보조/#2377ae 상호작용, Pretendard Variable와 시스템 한글 대체서체. 1160px 콘텐츠, 16px 모바일 바깥 여백, 16px 본문/1.6, 20~24px 제목, 최소44px 조작 영역.
- 비교 dialog는 데스크톱 최대900px 3열, 모바일1열·dvh 스크롤·안전 영역. 명확한 조회/재조회 버튼, 로딩·빈 결과·개별 실패·닫기/포커스 복귀 제공.
- 실제 수정 경로: templates/index.html, static/weather-observatory.css, static/weather-workspace.js, static/weather-compare.js, static/service-worker-v17.js, 관련 Java 서비스/DTO 및 회귀 테스트, README/API_CONTRACT.
- 주 CTA 지역 검색은 openLocationSearch(), 비교는 WeatherCompare.open(), 시간별 상세는 showHourlyDetail()에 연결한다. 이메일 흐름은 유지한다.

### Filled generation prompt 적용
아래 기존 전체 master prompt의 Objective부터 Final self-check까지 유지하며, 입력값은 이 절의 확정값을 사용한다. Create one high-concept page for 지역을 반복해서 확인하고 같은 시각의 날씨를 비교하는 날씨한편. Offer는 실제 관측·예보와 최대 3지역 동일 시각 비교, Audience는 집·직장·방문지 날씨를 확인하는 한국 사용자, Primary user action은 지역 날씨 확인, Deliverable은 기존 Thymeleaf·vanilla JS의 실제 웹 화면과 테스트, Style direction은 Airy weather observatory, Tonal key는 Light/선택형 Dark, Icon policy는 Topic-native SVG, Proof assets는 실제 API 응답, Motion intensity는 Minimal이다. 홍보형 히어로·가상 데이터·기능 카드 나열을 만들지 않고, 기존 기온 중심 첫 화면과 선택형 비교 dialog를 완성한다.

### Output target / 검증 계획
- 위 명시 경로에 구현한다. 이전 로컬 수정과 API 키·운영 DB·메일 계정은 보존한다. 커밋·푸시·배포·실제 이메일 발송 없음.
- 전체 Gradle test bootJar, 기존 JS 회귀+신규 비교/저장소/경합/스냅샷 테스트. 운영 DB·SMTP 없이 로컬 H2 실행.
- 360×800·390×844·768×1024·1440×900 렌더링. 실제 지역 검색→전환→비교→시간 상세, 3개 상한, 날짜 변경, 부분 실패, Escape/Tab 포커스, 가로 넘침·콘솔 오류를 점검한다.

### 구현 후 정제
- 모바일 첫 렌더에서 긴 지역 바로가기들이 144px를 차지해 날씨를 밀었다. 44px의 접이식 지역 선택으로 변경하고 PC의 직접 전환 버튼은 유지했다. 메뉴는 Escape/외부 클릭으로 닫힌다.
- 비교 카드의 2줄 지역명 높이를 맞춰 데스크톱 기온 기준선을 정렬했다. 360·390 모바일 한 열, 768 태블릿 두 열, 1440 데스크톱 세 열로 비교하며 2지역 비교는 너비를 균등하게 사용한다. 긴 이름은 줄바꿈하고 값과 단위는 붙여 둔다.
- 관측 강수량이 누락됐을 때 프런트에서도 '무강수'로 바꾸지 않는다. 공기질 문맥과 조회 시각은 지역을 바꾸는 즉시 로딩 문구로 초기화한다.
- 공개 PWA 자산만 캐시하고, 계정·인증 쿼리·구독 HTML은 저장하지 않는다. 신규 static/offline.html은 개인정보 없는 연결 안내이며 캐시 저장 실패도 정상 네트워크 응답을 막지 않는다.
- 비교 요청은 정확 날짜·정확 시각만 사용하며 오늘 오후/내일 오전을 실제 API로 조회했다. 부분 실패·늦은 응답·닫기·3지역 상한은 실제 모듈 VM 테스트로도 확인한다.
- 최종 전체 Gradle test/bootJar: 164 tests 실패·오류·건너뜀0. 정적 자산 마지막 보완 뒤 FrontendContractTest 3개와 bootJar 재확인. JS 8개 스크립트 모두 통과(서비스워커23, 비교13, 신뢰성13, 위치/날짜15, 기존 상태11·구독3·요약20 assertions·차트19 assertions).
- 새 JAR의 로컬 H2 실행과 분리된 SMTP에서 health UP 및 실제 지역 검색·비교·공기질 시각/기준 메타데이터를 확인했다. 실제 운영 인증·수신 메일·실기기·부하·배포는 미검증이며 키와 운영 데이터는 유지했다.

## 2026-10-23 추가 요청: 실제 날씨 서비스의 시각 구성

이번 요청은 기존 로컬 변경을 보존하면서 웹의 정보 밀도·날씨 정체성·탐색을 크게 개선한다. 아래 계획이 이전 Variant 1의 웹 배치 계획보다 우선하며, 메일·인증·백엔드 계약은 유지한다.

### 현 화면 진단
- 1440×900 실제 화면에서 현재 관측은 좁은 왼쪽 카드에 있고 오른쪽은 긴 제목·요약 표·도구가 차지해 시간별 예보가 첫 화면에서 밀린다.
- 현재 관측·예보 제목·기온 범위·대기질이 여러 번 반복되고 도구 6개가 같은 비중으로 노출된다.
- 지역 미선택 화면에 날씨의 시각적 정체성이 부족하다. 구독과 고급 기능 안내는 날씨를 본 뒤에 읽는 것이 자연스럽다.

### 이번 한 가지 방향 / OpenUI resolved inputs
- Topic: 우리 동네의 현재 관측과 앞으로의 날씨 변화.
- Offer: 실제 기상청·에어코리아 자료를 읽기 쉽게 확인하고 선택적으로 이메일 구독.
- Audience: 외출 전 휴대폰이나 PC로 짧게 날씨를 확인하는 한국 사용자.
- Primary user action: 장소 선택 후 시간별 기온과 강수 확인.
- Deliverable: 기존 Thymeleaf index.html, 신규 weather-observatory.css 및 작은 순수 SVG 차트 모듈. 이전 weather-letter.css의 공통·메일 보기 스타일은 유지.
- Style direction: Airy weather observatory, 넓은 현재 날씨와 정렬된 예보.
- Tonal key: Light, 짙은 슬레이트·하늘빛·흰 표면.
- Icon policy: Topic-native, 실제 상태에 연결한 일관된 SVG. 알 수 없는 하늘은 맑음으로 표시하지 않음.
- Proof assets: 기존 실제 관측·시간별 예보 API 응답. 가상 기온·공식 날씨인 척하는 이미지 없음.
- Motion intensity: Minimal, 버튼/시트 피드백 150~220ms, reduced-motion 존중.

### Composition contract
- Hero archetype: asymmetrical. 전체 폭의 현재 날씨 영역에서 지역 → 큰 기온 → 관측 상태 → 습도·바람·강수 순으로 읽는다.
- First-viewport structure: 72px 브랜드/검색 헤더, 약 280px 현재 날씨, 바로 아래 날짜 선택과 시간별 기온 변화. 모바일은 기온과 아이콘을 한 행에 두고 시간별 예보를 다음 영역으로 이어준다.
- Primary visual/proof artifact: 실제 수치에 기반한 시간별 온도 궤적과 정렬된 강수확률. 누락 구간은 선을 잇지 않는다.
- Interaction behavior: 기존 장소 dialog, 날짜 전환, 시간대 선택, 메일 미리보기, 구독 인증 유지. 보조 도구는 접기 메뉴로 정돈. 온도 차트는 기존 수치 표와 같은 데이터를 사용하고 독립 가로 스크롤을 만들지 않는다.
- Reuse boundary: 한 디자인. 기존 메일 템플릿과 검증된 서버 계약을 유지하며 웹의 시각 구성만 재조직한다.

### 구체적 시스템과 콘텐츠
- 화면 배경 #f5f8fc, 표면 #ffffff, 관측 영역 #eaf4fb, 글자 #142c42, 보조 #526779, 구분선 #dfe8f0, 상호작용 #2377ae, 맑음 아이콘의 노랑 #ecb43c.
- Pretendard Variable 유지. 브랜드22/750, 지역28~34/700, 기온72~100/500, 섹션20~22/700, 본문15~16/1.6, 보조13/1.5. 수치 tabular-nums, 단위는 함께 유지.
- 최대1160px, gutter24px, 간격8/12/16/24/32/40, 표면 radius20px, 버튼10px. 모바일 바깥16px/안쪽20px.
- 현재 날씨 아래는 시간별 예보 → 날짜별 아침/낮/저녁 → 공기질/기온범위 → 선택형 상세와 구독. 실질 정보만 노출.
- 데스크톱 일별 요약과 대기질은 2열, 모바일 1열. 시간별 예보만 가로 스크롤. 검색은 독립 sheet, 44px 조작영역.
- 주 CTA: 지역 검색 → openLocationSearch(), 구독 → openSubscriptionPanel(). 보조 CTA: 시간별 예보 #hourlyWeather.
- 실제 변경 경로: index.html, 신규 weather-observatory.css와 weather-chart.js, service-worker-v17.js, FrontendContractTest, scripts/test-weather-chart.cjs, scripts/test-subscription-draft.cjs, README.md. 서버 계약/키/DB는 유지.
- 기본 디자인은 기존 filled master prompt의 목적·비조작 데이터·접근성·일관성 지침을 모두 유지하며, 위 Inputs와 Composition을 이번 확정값으로 적용한다. Output target은 위 파일이다.

### 검증
- 로컬 H2와 연결되지 않은 SMTP로 실행. JS 요약·상태 경합 회귀, 실제 차트의 결측/시간 공백/단위 전환 검증, 관련 Gradle 테스트와 bootJar.
- 360×800,390×844,768×1024,1440×900 직접 캡처·검토 후 수정. 검색·내일 전환·단위·테마·메일 preview·키보드 확인.
- 커밋·푸시·배포·실제 메일 발송은 이번 요청에 포함되지 않음.

### 실제 렌더 후 정제 내용
- 초기 화면의 좁은 flex 자식 폭과 헤더 겹침을 제거하고, 현재 날씨와 예보가 같은 1160px 기준선에 맞도록 수정했다.
- 모바일에서 보조 도구를 시간별 예보 아래로 이동하고 현재 날씨의 여백·숫자 단위·아이콘 비율을 조정했다.
- 보기 설정의 잘림을 고치고 Escape·외부 클릭 닫기와 키보드 포커스 복귀를 구현했다.
- 구독을 누르면 바로 외부 로그인으로 보내지 않고 절차를 설명한다. 실제 로그인 클릭에만 지역·알림 초안을 보관하며 저장소 차단도 처리한다.
- 일별 자료만 실패한 경우에도 시간별 온도 단위 변경이 적용되도록 회귀를 수정했다.
- 인쇄 규칙에서는 지역 제목을 명시적으로 유지하고 시간별 예보를 6열 여러 행으로 배치한다. 실제 프린터 출력은 검증 범위 밖이다.
- 360·390·768·1440px 화면과 긴 지역명·다크 모드를 실제 렌더링했다. 390×460 검색은 키보드를 모사한 높이 검사이며 실제 휴대폰 키보드 검사는 아니다.
- Thymeleaf 전체 페이지를 로그인/비로그인 양쪽으로 렌더하는 테스트를 추가해 인라인 JS의 템플릿 해석 오류를 방지한다.
- 최종 캡처: docs/images/weather-observatory-{desktop,mobile,small-mobile,tablet,details,subscribe}.jpg. 로컬의 실제 날씨 자료이며 운영 배포 상태를 의미하지 않는다.
- 최종 전체 테스트 152개 통과, bootJar 성공. JS 요약20·차트19 assertions, 상태경합11·구독초안3 케이스 통과. Liveness/readiness UP. 저장소·인증 토큰·운영 DB·SMTP 변경 없음.

## 이전 기준안: Variant 1 (이번 웹 배치는 위 확정값으로 대체)

### Resolved inputs
- Topic: 날씨한편의 지역별 날씨 조회와 날씨 뉴스레터
- Offer: 선택한 지역의 현재 관측·시간별 예보를 확인하고 인증한 본인 이메일로 예약 브리핑을 받는 서비스
- Audience: 한국에서 출근·등교·일상 외출 전 날씨를 빠르게 확인하는 사람
- Primary user action: 지역을 선택해 현재와 앞으로의 날씨 확인
- Deliverable: 기존 Spring MVC·Thymeleaf의 index.html, weather.css, weather-mail.html과 필요한 순수 JavaScript·Java 편집 계층
- Style direction: Editorial, 차분한 하늘빛 일상 날씨 편지
- Tonal key: Light, 기존 #2878a4 인터랙션 색상과 슬레이트 텍스트
- Icon policy: Topic-native, 기존 SVG 날씨 아이콘만 사용
- Proof assets: 실제 기존 API 응답만 표시. 생성한 디자인 시안은 방향 참고이며 실서비스 스크린샷이나 근거로 사용하지 않음
- Motion intensity: Minimal, 새 자료는 즉시 표시하고 지연 등장 효과를 사용하지 않음.

### 서비스 진단과 근거
- 기본 흐름은 지역 선택 → 현재 관측/시간별 예보 → 이메일 인증/구독이다. API/인증/스케줄러를 유지한다.
- index.html의 메일 미리보기는 외출 점수 중심이며 실제 메일도 반복적인 조언이 많아 전 단계의 시안과 정보 위계가 다르다.
- weather.css는 4,700줄에 누적된 여러 레이아웃 규칙이 있다. 기존 기능 스타일을 전면 폐기하지 않고 이번 편집형 요약을 명확한 독립 컴포넌트로 구현한다.
- loadWeatherPreview/loadCurrentWeather는 버전 검사 이전에 전역에 대입한다. 이후 렌더링에서 지역과 데이터가 어긋날 가능성을 수정한다.
- 메일의 첫 4시간 표시는 발송시각을 반영하지 않는다. 이후 시간만 정렬해서 보낸다.
- 원천 누락이 0%·맑음·20°C가 되는 경로를 점검해 공개 표시와 안전 조언에서 자료 없음을 유지한다.
- 인증 owner 생성/구독 소비 계약의 불일치를 수정하고 서로 다른 계정의 6자리 코드가 충돌하지 않게 한다.
- 사용자 변경 없음: 시작 시 weather-main-release, c93e63b, clean. 배포/커밋/푸시는 이번 요청 범위에 없음.

### Composition contract
- Hero archetype: asymmetrical, 날씨 관측 자체가 시각 중심이며 홍보 문구를 큰 히어로로 사용하지 않음.
- First-viewport structure: 얇은 브랜드/탐색 헤더, 지역 미선택이면 설명 한 줄과 검색. 선택 후 넓은 관측 요약과 편집형 예보 한 문장을 먼저 표시. 다음 영역의 시간별 예보가 이어짐.
- Primary visual/proof artifact: 실제 현재 기온과 일관된 날씨 SVG, 실제 출근·낮·퇴근 예보 행. 검증되지 않은 통계 없음.
- Interaction behavior: 검색 modal/sheet와 기존 API, 날짜 전환, 예보 새로고침, 단위/테마 전환, 메일 미리보기, 인증/구독을 유지. 이전 응답을 차단. 상세 기능은 선택형 접기 영역.
- Reuse boundary: 한 개 디자인. 웹과 메일의 타이포/블루/정보 순서를 맞추되 메일은 table+inline styles로 독립 렌더링.

### 콘텐츠·디자인 시스템
- 핵심 메시지: 지역의 현재와 앞으로 달라지는 날씨를 짧고 정확하게.
- 주 CTA: 지역 검색 → searchLocations(); 선택 뒤 이메일로 받기 → openSubscriptionPanel().
- 보조 CTA: 시간별 예보 보기 → #hourlyWeather; 메일형 보기 → openBriefingPreview().
- 배경 #f7f9fc, 본문 표면 #ffffff, 기본 글자 #172330, 보조 #526577, 경계 #e2e9ef.
- 기존 --blue #2878a4 재사용, 접근 가능한 진한 버튼/링크. 태양 #edb850은 날씨 SVG에만.
- Pretendard Variable 기존 공식 배포 유지, fallback system-ui/Apple SD Gothic Neo/Malgun Gothic/sans-serif.
- 브랜드 22px/750, 지역 26~32px/700, 기온 clamp(64px,7vw,88px)/650, 편집형 제목 24~28px/700.
- 본문 16px/1.65, 보조 13~14px/1.55, 수치는 tabular-nums, 단위 nowrap.
- 간격 4/8/12/16/24/32/48, 데스크톱 최대1120px. 모바일12~20px 여백. 카드 반경16px, 버튼12px.
- 본문의 정보는 대부분 구분선과 정렬로 묶으며 카드 중첩을 늘리지 않는다.
- 360/390 모바일 한 열, 768 태블릿 한 열, 1024+ 관측/예보 균형 2열. 시간별 예보만 가로 스크롤.
- 로딩은 실제 요청과 연결. 지역 선택 변경 즉시 기존 측정 표시 제거, 부분 실패는 유효한 자료 유지.
- 날짜별 headline/강수/대기질 요약과 앞으로의 시간대에 집중. 모르는 수치는 자료 없음으로.
- 일반 뉴스 수집/모델 API 추가 없음. 날씨한편 목적에 맞는 브리핑만 편집.
- 이미지/자산: 기존 favicon과 SVG 날씨 아이콘. 생성 시안 이미지를 실화면에 삽입하지 않음.
- reduced-motion 시 transition 제거, focus-visible, dialog Esc/복귀, 44px 이상 조작영역 확인.

### 실제 수정 경로와 유지 계약
- templates/index.html: 편집형 요약, 메일 미리보기, 장소/응답 일관성, 구독 안내.
- static/weather.css 및 신규 weather-letter.css/js: 신규 편집 컴포넌트와 반응형 스타일/순수 편집 함수.
- templates/weather-mail.html, MailService, 순수 메일 편집 DTO: 뉴스레터·이후 시간대·일반 텍스트 본문.
- UserService/EmailVerificationService: 인증 소유자와 코드 범위. API 필드/긴 링크 토큰 호환.
- WeatherService/HourlyWeatherDto/WeatherDto: 누락 값 보존 및 자료 없음 판단.
- README, 필요한 회귀 테스트 및 local 검증 기록.
- 기존 /api/weather, /api/locations, /api/users, 인증·구독 예약·해지 계약과 계정 소유권 보존.
- 메일/DB/배포 인증정보는 사용하거나 노출하지 않음. 운영 메일 실제 발송 금지.

### 실행·검증 계획
- Java17/기존 Gradle test bootJar, 필요한 JS syntax와 비동기 지역 경합 회귀 검증.
- 로컬 H2, SMTP 미연결, 스케줄 대상 없는 독립 개발 프로세스로 실행.
- 저장된 날씨 API 키가 있으면 읽기 전용 조회만 사용. 없으면 실패 상태를 실제로 확인하고 fixture는 테스트임을 구분.
- 웹 및 메일 fixture를 360×800,390×844,768×1024,1440×900에서 직접 렌더/시각 점검.
- 검색, 지역 선택/빠른 변경, 날짜/단위 전환, preview dialog, 폼 validation, 키보드 focus/Esc, 오류/부분실패 확인.
- 실메일 수신/실제 운영 SSO 완료/운영 배포는 이번 검증 범위 아님.

### Filled generation prompt
Create one high-concept page for 날씨한편의 지역별 날씨 조회와 날씨 뉴스레터.

## Inputs
- Topic: 날씨한편의 지역별 날씨 조회와 날씨 뉴스레터.
- Offer: 선택한 지역의 현재 관측·시간별 예보를 확인하고 인증한 본인 이메일로 예약 브리핑을 받는 서비스
- Audience: 한국에서 출근·등교·일상 외출 전 날씨를 빠르게 확인하는 사람
- Primary user action: 지역을 선택해 현재와 앞으로의 날씨 확인
- Deliverable: 기존 Spring MVC·Thymeleaf의 index.html, weather.css, weather-mail.html과 필요한 순수 JavaScript·Java 편집 계층
- Style direction: Editorial, 차분한 하늘빛 일상 날씨 편지
- Tonal key: Light, 기존 #2878a4 인터랙션 색상과 슬레이트 텍스트
- Icon policy: Topic-native, 기존 SVG 날씨 아이콘만 사용
- Proof assets: 실제 기존 API 응답만 표시. 생성한 디자인 시안은 방향 참고이며 실서비스 스크린샷이나 근거로 사용하지 않음
- Motion intensity: Minimal, 조작 피드백에만 짧은 전환을 사용하고 reduced-motion을 존중

## Objective
Design one premium, memorable page with a dominant hero and one coherent visual world derived from the topic itself.
The result should feel specific, intentional, and action-oriented when the concept calls for action, not like a generic startup or SaaS template.

Choose the strongest clear solution.
Do not collapse the concept into a simpler generic pattern just to make execution easier.

- Treat single-file execution as a packaging constraint, not a creative constraint.
Do not reduce conceptual ambition, visual specificity, interaction richness, or production quality merely because the deliverable is concise.

## Internal concept choice
Before composing, decide internally:
- one dominant hero archetype: centered / asymmetrical / immersive
- one dominant proof mode: none / embedded gesture / single artifact
- one palette driver derived from the topic
- one tonal key: light / mid / dark / topic-derived
- one motion role: none / minimal / ambient / interactive
- one icon approach: none / minimal / expressive / topic-native
- optionally one secondary accent only if it strengthens the main direction without creating a second visual world
Do not output this planning.

## Non-negotiables
- Use a strong hero-first composition.
- Headline: 3-12 words, max 3 lines.
- Supporting copy: 1-2 short sentences, concise and specific.
- CTAs are optional. If used, include at most 1 primary and 1 secondary CTA.
- If a clear next step is central to the offer or concept, include a CTA; otherwise CTA omission is allowed.
- CTA labels must be specific to the offer and action, not generic.
- Supporting highlights are optional. If used, show them only once in a single lower support cluster beneath the hero.
- Keep the hero dominant, focused, readable, and uncluttered.
- Proof is optional. Use it only if it clearly strengthens the concept.
- If no real proof assets are provided, do not invent metrics, testimonials, customer logos, screenshots, or fake UI proof.
- Prefer depth inside the hero, proof gesture, CTA behavior, support copy, or background logic rather than adding extra sections by default.

## Composition
- Choose the clearest structure for the topic; do not default to generic two-column marketing layouts.
- A top navigation bar is optional. When used, make it native to the chosen style direction and subordinate to the hero rather than a generic boxed header.
- The hero may be fully integrated rather than framed.
- The hero does not need to become a panel, dashboard, device mockup, rounded card, or floating showcase unless the topic genuinely demands that artifact.
- Build hierarchy through scale, contrast, spacing, alignment, depth, rhythm, and composition before using frames, dividers, or containers.
- Keep supporting content clearly subordinate to the hero.
- Do not interpret structural restraint as permission to flatten the concept into a generic minimal layout.
- Do not use text-on-text layering, typographic collisions, or overlapping live text as a compositional device.

## Visual system
- Build one coherent visual world derived from the topic itself.
- Let the topic drive palette, contrast, typography, spacing, geometry, motifs, materials, and motion.
- Even restrained palettes must feel intentional and specific, not generic premium-neutral.
- The background must actively contribute mood, depth, and topic logic.
- If the topic suggests a real system language, physical behavior, or interface grammar, show that directly instead of generic abstract-tech decoration.
- Treat headline typography as part of the concept, not neutral text.
- The headline may use topic-derived color, selective emphasis, multi-tone treatment, or gradient accents when they strengthen the concept and remain readable.
- Do not default the headline to neutral black, white, or near-white unless that is clearly the strongest topic-native choice.

## Background and tonal diversity
- Premium and atmospheric design may be light, mid-tone, or dark; do not default to dark.
- Choose the tonal key from the topic, offer, audience, and style direction rather than from generic cinematic or tech aesthetics.
- Contrast may come from hue, saturation, temperature, texture, density, or scale, not only darkness.

## Proof and concept expression
- Prefer the minimum visual evidence needed to make the idea feel specific, credible, and native to the topic.
- If the concept is strongest with typography, atmosphere, and one precise gesture, do that.
- If a stronger proof artifact is justified, use one legible, unified artifact rather than multiple fragmented panels.
- Proof may take the form of a signal, trace, contour, command line, path, specimen, structural mark, scene fragment, or other topic-native evidence.
- Typography itself may carry part of the concept or proof.
- When appropriate, use one precise typographic gesture—such as selective word emphasis, chromatic offset, gradient emphasis, distortion, signal breakup, or structural interruption—to make the concept feel more specific and alive.
- Prefer one strong typographic intervention over many scattered decorative effects.
- Avoid decorative pseudo-proof.
- Avoid turning simple ideas into framed showcases, dashboard fragments, floating panels, softened slabs, or generic marketing geometry.

## Icons and symbolic language
- Icons are optional.
- Use icons only when they improve scanability, recognition, navigation, proof clarity, or support-cluster legibility.
- Prefer topic-native icon logic over generic startup glyphs.
- Keep icon style consistent in stroke weight, geometry, corner logic, and detail level.
- Use icons primarily in clearly secondary roles unless a single topic-native symbol is central to the concept.
- Do not let icons compete with the hero headline, proof artifact, or CTA hierarchy.

## Support cluster
- If used, show supporting highlights only once, in a single lower cluster beneath the hero.
- Each highlight should be short, fast to scan, and clearly secondary.
- Supporting highlights may use concise icons or symbolic marks when helpful.
- Support-cluster elements may use subtle motion when it improves scanability, continuity, or atmosphere and remains clearly secondary.
- Card-like grouping is allowed only inside this lower support cluster.
- These modules may use at most one light surface treatment, such as a subtle stroke, tonal fill, or very soft shadow.
- Keep support modules uniform, non-nested, and visually subordinate.
- Do not use card-like structures in the hero, proof, CTA area, or elsewhere on the page.

## Motion
- Motion is optional.
- Use motion only when it supports hierarchy, continuity, feedback, atmosphere, or topic logic.
- Default to none, minimal, or ambient motion depending on what the concept actually needs.
- Motion may live in typography, background behavior, proof logic, support-cluster behavior, or interaction feedback.
- A single key word or short phrase may use subtle text-based motion such as glitch, flicker, jitter, scanline shift, chromatic split, text scramble, pulse, or reveal if it strengthens the concept.
- Keep motion controlled, readable, non-blocking, and subordinate to clarity.
- Avoid scrolljacking, attention-hijacking loops, and ornamental animation.
- Respect reduced-motion preferences.

## Accessibility and readability
- Preserve strong readability and clear hierarchy at all viewport sizes.
- Maintain sufficient contrast between text and background.
- Do not place essential text over busy imagery unless readability remains strong.
- Avoid cramped spacing, visual collisions, and weak focal hierarchy.
- Do not place badges, pills, annotations, stickers, or floating interface fragments over essential text.
- If the deliverable is code, use semantic HTML structure, visible focus states, keyboard-friendly interactions, and reduced-motion handling.
- When in doubt, preserve legibility over expressive overlap.

## Anti-drift rules
- Do not drift into bland corporate SaaS styling.
- Do not default to dashboard aesthetics, feature-grid thinking, boxed-section composition, or template-like modularity.
- Do not use disconnected motifs, random color choices, or interchangeable abstract-tech shapes.
- Do not use card-within-card composition, nested surfaces, floating panels, fake proof, or stock-looking UI.
- Keep one visual world, one dominant hero, and one clear structural logic.
- Do not allow text, labels, or interface elements to overlap, collide, or sit on top of one another.

## Output
Return only the final 기존 Spring MVC·Thymeleaf의 index.html, weather.css, weather-mail.html과 필요한 순수 JavaScript·Java 편집 계층.

If the deliverable is code:
- Output a complete production-ready file or component.
- Keep dependencies minimal unless explicitly requested.
- Make it responsive.
- Keep support highlights in one lower support cluster only when used.

## Final self-check
Before returning, verify that the result keeps:
- one dominant hero
- one coherent visual world
- no invented proof
- no cardification outside the lower support cluster
- no generic SaaS drift
- no unnecessary structural fragmentation
- no nested containment
- no floating sub-surfaces
- all text clearly legible and comfortably spaced

### Output target
- Path: src/main/resources/templates/index.html, src/main/resources/static/weather-letter.css, src/main/resources/static/weather-letter.js, src/main/resources/templates/weather-mail.html

## 2026-10-23 로컬 실행·시각 검증 결과

- 최종 `gradlew.bat test bootJar --no-daemon`: 성공, 41개 suite / 151개 test, 실패·오류·건너뜀 0.
- `node scripts/test-weather-letter.cjs`: 20개 assertion 통과. `node scripts/test-weather-state.cjs`: 실제 loader/renderer 기반 11개 경합·실패 회귀 케이스 통과. 인라인 JS syntax와 `git diff --check` 통과.
- 운영 DB·SMTP와 격리한 H2/localhost 실행. Liveness·readiness 모두 UP. 실제 장소 검색(을지로3가), 선택, 관측·시간별·일별 예보, 내일 전환, 빈 검색 결과/검색어 유지, 미리보기, 닫기와 포커스 복귀, 단위·테마 전환을 확인.
- 1440×900, 768×1024, 390×844, 360×800을 실제 렌더링해 확인. 모두 수평 페이지 넘침 없음. 하단 안내가 모바일 메뉴에 가리지 않는 것도 확인. 관련 콘솔 오류·경고 없음.
- 390×460의 낮은 viewport에서도 검색어·검색·닫기 조작 유지. 실제 휴대폰 키보드 테스트와는 구분.
- 1차 렌더 후 현재 기온 주변 여백 축소, 온도계 관측 아이콘 적용, 저녁 기본 시간대와 미래 날짜 문구 보정. 최종 화면은 `docs/images/weather-letter-20261023-*.jpg`에 저장.
- 메일은 긴 주소·비·밤·결측·부분 실패·내일 HTML fixture를 브라우저에서 검토. 고정된 검사 자료이며 실제 발송·수신 성공이 아님.
- 실제 운영 로그인, Gmail·Naver 수신, 실기기 키보드·확대 글자, 부하 테스트는 미검증. 커밋·푸시·운영 배포·실메일 발송은 수행하지 않음. 운영 데이터·기존 API 키를 변경하지 않음.
