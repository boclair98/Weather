# Weather Mail Subscription Service

사용자가 원하는 지역을 검색해 구독하면, 기상청 단기예보를 기반으로 매일 날씨 메일을 발송하는 Spring Boot 프로젝트입니다.

## 미리보기

사용자는 위치, 알림 시간, 연령대, 성별을 선택해 구독합니다.

![구독 화면 미리보기](docs/images/subscription-form-preview.svg)

선택한 정보와 날씨 데이터를 조합해 메일에서 날씨 요약, 옷차림, 우산 여부, 야외활동 팁, 스타일링 추천을 제공합니다.

![날씨 메일 미리보기](docs/images/weather-mail-preview.svg)

## 주요 기능

- 지역명 검색 기반 위치 선택
- Kakao Local API를 통한 주소/장소 검색
- 위도/경도 좌표를 기상청 격자 좌표로 변환
- 기상청 단기예보 API 연동
- 구독 등록 시 웰컴 날씨 메일 즉시 발송
- 아침/점심/저녁 알림 시간 선택
- 연령대/성별 기반 날씨별 스타일링 추천
- 날씨 조건별 옷차림, 우산, 야외활동 추천 문구 제공
- 스케줄러를 통한 시간대별 날씨 메일 자동 발송
- 구독 위치 변경 API
- 이메일 수신 거부 및 재구독 API
- Thymeleaf HTML 메일 템플릿

## 기술 스택

- Java 17
- Spring Boot 3.2.5
- Spring Web
- Spring Data JPA
- Spring Mail
- Spring Scheduler
- Thymeleaf
- MySQL
- Kakao Local API
- 기상청 단기예보 API

## 프로젝트 구조

```text
src/main/java/com/example/WebSideProject
├── config
│   └── AppConfig.java
├── controller
│   ├── HomeController.java
│   ├── LocationController.java
│   ├── UserController.java
│   ├── WeatherController.java
│   └── WeatherMailController.java
├── dto
│   ├── LocationDto.java
│   ├── UserDto.java
│   └── WeatherDto.java
├── entity
│   └── User.java
├── repository
│   └── UserRepository.java
├── scheduler
│   └── WeatherMailScheduler.java
└── service
    ├── LocationService.java
    ├── MailService.java
    ├── UserService.java
    └── WeatherService.java
```

## API

### 위치 검색

```http
GET /api/locations/search?query=강남역
```

Kakao Local API로 장소 또는 주소를 검색하고, 기상청 격자 좌표까지 변환해 반환합니다.

```json
[
  {
    "locationName": "강남역",
    "latitude": 37.4979,
    "longitude": 127.0276,
    "nx": 61,
    "ny": 125
  }
]
```

### 날씨 조회

```http
GET /api/weather?nx=61&ny=125&period=MORNING
```

### 구독 등록

```http
POST /api/users/subscribe
Content-Type: application/json
```

```json
{
  "name": "홍길동",
  "email": "user@example.com",
  "locationName": "강남역",
  "latitude": 37.4979,
  "longitude": 127.0276,
  "nx": 61,
  "ny": 125,
  "ageGroup": "TWENTIES",
  "gender": "FEMALE",
  "morningEnabled": true,
  "afternoonEnabled": false,
  "eveningEnabled": true
}
```

### 수신 거부

```http
GET /api/users/unsubscribe?email=user@example.com
PATCH /api/users/unsubscribe?email=user@example.com
```

### 재구독

```http
PATCH /api/users/resubscribe?email=user@example.com
```

### 스타일 추천 기준 변경

이미 구독한 이메일의 연령대와 성별 선택값을 변경합니다.

```http
PATCH /api/users/style-preference
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "ageGroup": "THIRTIES",
  "gender": "MALE"
}
```

사용 가능한 값은 다음과 같습니다.

```text
ageGroup: NONE, TEENS, TWENTIES, THIRTIES, FORTIES, FIFTIES_PLUS
gender: NONE, FEMALE, MALE
```

### 구독 위치 변경

이미 구독한 이메일의 지역 정보를 새 위치로 변경합니다.

```http
PATCH /api/users/location
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "locationName": "강남역",
  "latitude": 37.4979,
  "longitude": 127.0276,
  "nx": 61,
  "ny": 125
}
```

### 구독자 전체 수동 발송

```http
POST /api/weather-mails/send-now
```

특정 시간대만 수동 발송할 수도 있습니다.

```http
POST /api/weather-mails/send-now?period=MORNING
POST /api/weather-mails/send-now?period=AFTERNOON
POST /api/weather-mails/send-now?period=EVENING
```

## 환경 변수

실제 키와 비밀번호는 GitHub에 올리지 않고 환경변수로 관리합니다.

```env
DB_URL=jdbc:mysql://localhost:3306/weatherdb?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your_email@gmail.com
SMTP_PASSWORD=your_gmail_app_password

WEATHER_API_KEY=your_kma_api_key
WEATHER_API_BASE_URL=https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0
KAKAO_REST_API_KEY=your_kakao_rest_api_key

APP_BASE_URL=http://localhost:8080
```

## 실행 방법

MySQL에 `weatherdb` 데이터베이스를 만든 뒤 실행합니다.

로컬 개발에서는 `src/main/resources/application-local.yml`을 사용할 수 있습니다. 이 파일은 `.gitignore`에 포함되어 GitHub에는 올라가지 않으며, IntelliJ에서 바로 실행할 때 개인 DB 비밀번호, SMTP 비밀번호, API 키를 보관하는 용도입니다.

GitHub에 올라가는 `application.yml`은 환경변수만 참조합니다.

```bash
./gradlew bootRun
```

Windows PowerShell 예시:

```powershell
$env:DB_PASSWORD="your_mysql_password"
$env:SMTP_USERNAME="your_email@gmail.com"
$env:SMTP_PASSWORD="your_gmail_app_password"
$env:WEATHER_API_KEY="your_kma_api_key"
$env:KAKAO_REST_API_KEY="your_kakao_rest_api_key"
.\gradlew.bat bootRun
```

## 개선 예정

- 사용자별 발송 시간 직접 지정
- 메일 발송 이력 저장
- 대기질 API 연동
- 스타일 참고 이미지 API 연동
