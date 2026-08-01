package com.example.WebSideProject.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder(toBuilder = true)
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherDto {
    private String date;
    private String time;
    private String periodLabel;
    private String sky;
    private String pty;
    private String tmp;
    private String tmn;
    private String tmx;
    private String pop;
    private String reh;
    private String wsd;
    private String pm10Value;
    private String pm10Grade;
    private String pm25Value;
    private String pm25Grade;
    private String airQualityStation;
    private String styleHeadline;
    private String styleRecommendation;

    public String getDate() {
        return valueOrDash(date);
    }

    public String getTime() {
        return valueOrDash(time);
    }

    public String getPeriodLabel() {
        return valueOrDash(periodLabel);
    }

    public String getTmp() {
        return valueOrDash(tmp);
    }

    public String getTmn() {
        return valueOrDash(tmn);
    }

    public String getTmx() {
        return valueOrDash(tmx);
    }

    public String getPop() {
        return valueOrDash(pop);
    }

    public String getReh() {
        return valueOrDash(reh);
    }

    public String getWsd() {
        return valueOrDash(wsd);
    }

    public String getPm10Value() {
        return valueOrDash(pm10Value);
    }

    public String getPm25Value() {
        return valueOrDash(pm25Value);
    }

    public String getAirQualityStation() {
        return valueOrDash(airQualityStation);
    }

    public String getStyleHeadline() {
        return valueOrDash(styleHeadline);
    }

    public String getStyleRecommendation() {
        return valueOrDash(styleRecommendation);
    }

    public String getPm10Display() {
        return formatDustValue(pm10Value, pm10Grade);
    }

    public String getPm25Display() {
        return formatDustValue(pm25Value, pm25Grade);
    }

    public String getWeatherConditionLine() {
        return "강수 " + getPtyDescription() + " / 강수확률 " + getPop() + "% / 습도 " + getReh() + "%";
    }

    public int getOutingScore() {
        int score = 100;

        if (isRainy()) score -= 22;
        if (isSnowy()) score -= 26;
        if (getPopValue() >= 70) score -= 18;
        else if (getPopValue() >= 40) score -= 10;

        if (getTmpValue() >= 32 || getTmpValue() <= -5) score -= 18;
        else if (getTmpValue() >= 28 || getTmpValue() <= 5) score -= 10;

        if (getWsdValue() >= 8.0) score -= 10;
        if (getRehValue() >= 85) score -= 6;
        if (isBadAirQuality()) score -= 22;
        else if (isModerateAirQuality()) score -= 8;

        return Math.max(20, Math.min(100, score));
    }

    public String getOutingScoreLabel() {
        int score = getOutingScore();
        if (score >= 85) return "좋음";
        if (score >= 70) return "무난";
        if (score >= 50) return "주의";
        return "나쁨";
    }

    public String getOutingScoreAdvice() {
        int score = getOutingScore();
        if (score >= 85) {
            return "외출하기 좋은 편이에요. 기본 준비만 해도 충분해 보여요.";
        }
        if (score >= 70) {
            return "외출은 무난하지만 날씨 변수를 한 번 챙기면 좋아요.";
        }
        if (score >= 50) {
            return "외출 전 준비물이 필요해요. 우산, 마스크, 겉옷을 확인해보세요.";
        }
        return "야외 일정은 짧게 잡는 편이 좋아요. 실내 일정도 함께 고려해보세요.";
    }

    public String getPreparationChecklist() {
        StringBuilder checklist = new StringBuilder();
        appendChecklist(checklist, "옷차림: " + getClothingAdvice());
        appendChecklist(checklist, "우산: " + getUmbrellaAdvice());
        appendChecklist(checklist, "마스크: " + getMaskAdvice());
        return checklist.toString();
    }

    public String getWeatherMood() {
        if (isSnowy()) {
            return "눈";
        }
        if (isRainy() || getPopValue() >= 60) {
            return "비";
        }
        if ("1".equals(sky)) {
            return "화창";
        }
        if ("4".equals(sky)) {
            return "흐림";
        }
        return "구름";
    }

    public String getWeatherTheme() {
        if (isSnowy()) {
            return "snow";
        }
        if (isRainy() || getPopValue() >= 60) {
            return "rain";
        }
        if ("1".equals(sky)) {
            return "sunny";
        }
        if ("4".equals(sky)) {
            return "cloudy";
        }
        return "mild";
    }

    public String getDetailedWeatherMessage() {
        if (isSnowy()) {
            return "눈이 예상돼요. 이동 시간은 넉넉하게 잡고, 밑창이 미끄럽지 않은 신발과 따뜻한 겉옷을 준비해보세요.";
        }
        if (isRainy()) {
            return "비가 오는 날이에요. 우산은 필수에 가깝고, 밝은 색 신발보다는 방수 소재나 어두운 색 신발이 관리하기 편합니다.";
        }
        if (getPopValue() >= 60) {
            return "비가 올 가능성이 높아요. 지금 맑아 보여도 작은 우산을 가방에 넣어두면 하루 일정이 훨씬 편해집니다.";
        }
        if ("1".equals(sky)) {
            if (getTmpValue() >= 28) {
                return "하늘은 화창하지만 기온이 높아요. 햇빛을 피할 모자나 선크림, 통풍 좋은 옷차림을 추천해요.";
            }
            return "화창한 날씨예요. 산책이나 가벼운 외출에 잘 어울리는 날이라 밝고 가벼운 옷차림이 좋아요.";
        }
        if ("4".equals(sky)) {
            return "흐린 하늘이 이어질 수 있어요. 차분한 톤의 옷차림과 실내 일정이 잘 맞습니다.";
        }
        if (getTmpValue() <= 5) {
            return "날은 차가운 편이에요. 얇게 여러 겹 입고, 목 주변 보온을 챙기면 체감 추위를 줄일 수 있어요.";
        }
        return "큰 날씨 변수는 적은 편이에요. 기온 변화에 대비해 가벼운 겉옷 하나만 챙기면 안정적입니다.";
    }

    public String getForecastLabel() {
        if (date == null || date.length() != 8 || time == null || time.length() != 4) {
            return "아침 예보";
        }

        String label = periodLabel == null || periodLabel.isBlank() ? "아침" : periodLabel;
        return date.substring(4, 6) + "월 " + date.substring(6, 8) + "일 " + label + " 예보";
    }

    public String getSkyDescription() {
        return switch (valueOrDash(sky)) {
            case "1" -> "맑음 ☀️";
            case "3" -> "구름많음 ⛅";
            case "4" -> "흐림 ☁️";
            default  -> "알 수 없음";
        };
    }

    public String getPtyDescription() {
        return switch (valueOrDash(pty)) {
            case "0" -> "없음";
            case "1" -> "비 🌧️";
            case "2" -> "비/눈 🌨️";
            case "3" -> "눈 ❄️";
            case "4" -> "소나기 🌦️";
            default  -> "알 수 없음";
        };
    }

    public String getSummaryMessage() {
        if (isRainy()) {
            return "비 소식이 있어요. 이동할 때 우산과 젖어도 관리하기 쉬운 신발을 꼭 챙겨주세요.";
        }
        if (isSnowy()) {
            return "눈 소식이 있어요. 길이 미끄러울 수 있으니 여유 있게 움직이면 좋아요.";
        }
        if (getPopValue() >= 60) {
            return "강수확률이 높은 편이에요. 갑작스러운 비에 대비해 작은 우산을 챙겨보세요.";
        }
        if (getTmpValue() >= 28) {
            return "기온이 높아요. 물을 자주 마시고 한낮 야외 활동은 가볍게 조절해보세요.";
        }
        if (getTmpValue() <= 5) {
            return "쌀쌀한 날씨예요. 외출 전 겉옷과 목도리를 챙기면 든든합니다.";
        }
        if ("1".equals(sky)) {
            return "하늘이 맑은 편이에요. 가벼운 외출이나 산책하기 좋은 날씨입니다.";
        }
        if ("4".equals(sky)) {
            return "흐린 하늘이 예상돼요. 실내 일정도 함께 잡아두면 편합니다.";
        }
        return "큰 날씨 변수는 적어 보여요. 일정에 맞춰 편하게 준비해보세요.";
    }

    public String getAirQualitySummary() {
        if (!hasAirQuality()) {
            return "대기질 정보 -";
        }

        return "미세먼지 " + getPm10GradeLabel() + " / 초미세먼지 " + getPm25GradeLabel();
    }

    public String getAirQualityAdvice() {
        if (!hasAirQuality()) {
            return "대기질 데이터가 없을 때는 민감군이라면 외출 전 한 번 더 확인해보세요.";
        }
        if (isBadAirQuality()) {
            return "대기질이 좋지 않아요. 장시간 야외 활동은 줄이고 KF 마스크를 챙기는 편이 좋아요.";
        }
        if (isModerateAirQuality()) {
            return "대기질은 보통 수준이에요. 민감군은 장시간 외출 시 마스크를 준비해보세요.";
        }
        return "대기질이 좋은 편이에요. 환기나 가벼운 외출에 큰 부담은 적어 보여요.";
    }

    public String getMaskAdvice() {
        if (isBadAirQuality()) {
            return "KF80 이상 마스크 추천";
        }
        if (isModerateAirQuality()) {
            return "민감군은 마스크 권장";
        }
        if (!hasAirQuality()) {
            return "외출 전 대기질 재확인";
        }
        return "필수는 아니에요";
    }

    public String getClothingAdvice() {
        int temperature = getTmpValue();
        if (temperature >= 28) {
            return "반팔, 얇은 셔츠, 통풍이 좋은 옷";
        }
        if (temperature >= 20) {
            return "얇은 긴팔, 셔츠, 가벼운 겉옷";
        }
        if (temperature >= 12) {
            return "가디건, 자켓, 얇은 니트";
        }
        if (temperature >= 6) {
            return "코트, 니트, 따뜻한 겉옷";
        }
        return "패딩, 목도리, 장갑처럼 보온력 있는 옷";
    }

    public String getUmbrellaAdvice() {
        if (isRainy() || isSnowy() || getPopValue() >= 60) {
            return "챙기는 것을 추천해요";
        }
        if (getPopValue() >= 30) {
            return "작은 우산이 있으면 안심돼요";
        }
        return "필수는 아니에요";
    }

    public String getStyleRecommendation(AgeGroup ageGroup, GenderType gender) {
        AgeGroup selectedAgeGroup = ageGroup == null ? AgeGroup.NONE : ageGroup;
        GenderType selectedGender = gender == null ? GenderType.NONE : gender;

        return getStyleHeadline(selectedAgeGroup, selectedGender) + " "
                + getLayeringAdvice() + " "
                + getMaterialAdvice() + " "
                + getStyleCaution();
    }

    public WeatherDto withStylePreference(AgeGroup ageGroup, GenderType gender) {
        AgeGroup selectedAgeGroup = ageGroup == null ? AgeGroup.NONE : ageGroup;
        GenderType selectedGender = gender == null ? GenderType.NONE : gender;
        return toBuilder()
                .styleHeadline(getStyleHeadline(selectedAgeGroup, selectedGender))
                .styleRecommendation(getStyleRecommendation(selectedAgeGroup, selectedGender))
                .build();
    }

    public String getStyleHeadline(AgeGroup ageGroup, GenderType gender) {
        AgeGroup selectedAgeGroup = ageGroup == null ? AgeGroup.NONE : ageGroup;
        GenderType selectedGender = gender == null ? GenderType.NONE : gender;
        String audience = getAudienceLabel(selectedAgeGroup, selectedGender);

        if (isSnowy()) {
            return audience + " 오늘은 보온형 시티룩이 좋아요.";
        }
        if (isRainy() || getPopValue() >= 60) {
            return audience + " 오늘은 비에 강한 깔끔한 룩이 좋아요.";
        }
        if (isBadAirQuality()) {
            return audience + " 오늘은 세탁 쉬운 소재와 마스크 조합이 좋아요.";
        }
        if (getTmpValue() >= 30) {
            return audience + " 오늘은 통풍이 핵심인 가벼운 룩이 좋아요.";
        }
        if (getTmpValue() <= 5) {
            return audience + " 오늘은 레이어드와 보온 액세서리가 핵심이에요.";
        }
        if ("1".equals(sky)) {
            return audience + " 오늘은 밝은 포인트를 주기 좋은 날이에요.";
        }
        return audience + " 오늘은 단정한 데일리룩으로 안정감 있게 가면 좋아요.";
    }

    public String getTopAdvice() {
        int temperature = getTmpValue();
        if (temperature >= 30) {
            return "얇은 반팔, 린넨 셔츠, 통풍 좋은 니트";
        }
        if (temperature >= 24) {
            return "반팔 위 가벼운 셔츠, 얇은 카디건";
        }
        if (temperature >= 18) {
            return "긴팔 티셔츠, 셔츠, 얇은 니트";
        }
        if (temperature >= 10) {
            return "니트, 스웨트셔츠, 셔츠 위 자켓";
        }
        if (temperature >= 4) {
            return "두께감 있는 니트, 기모 이너, 보온 셔츠";
        }
        return "히트텍 계열 이너, 두꺼운 니트, 보온 베이스";
    }

    public String getBottomAdvice() {
        int temperature = getTmpValue();
        if (isRainy() || isSnowy()) {
            return "물 튐이 덜 보이는 어두운 팬츠나 발목이 편한 하의";
        }
        if (temperature >= 28) {
            return "와이드 팬츠, 쇼츠, 얇은 코튼 팬츠";
        }
        if (temperature <= 5) {
            return "기모 팬츠, 두께감 있는 데님, 울 혼방 팬츠";
        }
        return "데님, 슬랙스, 치노 팬츠처럼 활동성 있는 하의";
    }

    public String getOuterAdvice() {
        int temperature = getTmpValue();
        if (temperature >= 26) {
            return "실내 냉방 대비 얇은 셔츠 하나면 충분해요.";
        }
        if (temperature >= 18) {
            return "가벼운 자켓이나 카디건을 손에 들기 좋습니다.";
        }
        if (temperature >= 10) {
            return "자켓, 트렌치, 블루종처럼 바람을 막는 겉옷이 좋아요.";
        }
        if (temperature >= 4) {
            return "울 코트나 패딩 베스트로 체온을 잡아주세요.";
        }
        return "패딩, 두꺼운 코트, 머플러까지 묶어서 준비하세요.";
    }

    public String getFootwearAdvice() {
        if (isSnowy()) {
            return "미끄럼 적은 러버솔 부츠나 방수 스니커즈";
        }
        if (isRainy() || getPopValue() >= 60) {
            return "방수 스니커즈, 레인부츠, 어두운 색 신발";
        }
        if (getWsdValue() >= 8.0) {
            return "발을 안정적으로 잡아주는 스니커즈나 로퍼";
        }
        if (getTmpValue() >= 28) {
            return "통풍 좋은 스니커즈나 샌들";
        }
        return "걷기 편한 스니커즈, 로퍼, 데일리 슈즈";
    }

    public String getColorPalette() {
        if (isSnowy()) {
            return "차콜, 아이보리, 딥그린";
        }
        if (isRainy() || getPopValue() >= 60) {
            return "네이비, 그레이, 블랙";
        }
        if (isBadAirQuality()) {
            return "라이트그레이, 카키, 워시드블루";
        }
        if (getTmpValue() >= 28) {
            return "화이트, 스카이블루, 라이트베이지";
        }
        if (getTmpValue() <= 5) {
            return "카멜, 버건디, 다크브라운";
        }
        if ("1".equals(sky)) {
            return "크림, 민트, 데님블루";
        }
        return "오트밀, 네이비, 세이지";
    }

    public String getMaterialAdvice() {
        if (isSnowy()) {
            return "겉감은 물기를 털기 쉬운 소재, 안쪽은 플리스나 울처럼 열을 잡는 소재가 안정적이에요.";
        }
        if (isRainy() || getPopValue() >= 60) {
            return "나일론, 코팅 코튼, 폴리 혼방처럼 젖어도 빨리 마르는 소재를 추천해요.";
        }
        if (getTmpValue() >= 28 || getRehValue() >= 80) {
            return "린넨, 얇은 코튼, 기능성 드라이 소재처럼 몸에 달라붙지 않는 소재가 좋아요.";
        }
        if (getTmpValue() <= 5) {
            return "울, 플리스, 기모 소재처럼 공기층을 만드는 소재가 체감 온도를 올려줘요.";
        }
        return "코튼, 니트, 가벼운 울 혼방처럼 구김이 적고 움직이기 편한 소재가 좋아요.";
    }

    public String getLayeringAdvice() {
        int temperature = getTmpValue();
        if (temperature >= 28) {
            return "상의는 짧고 가볍게, 실내 냉방용 얇은 겉옷만 더하세요.";
        }
        if (temperature >= 18) {
            return "얇은 상의와 벗기 쉬운 겉옷 조합이 온도 변화에 강해요.";
        }
        if (temperature >= 8) {
            return "이너, 니트, 자켓 3단 구성이 아침저녁 체감에 안정적이에요.";
        }
        return "보온 이너와 두꺼운 아우터를 분리해 실내외 온도 차에 대응하세요.";
    }

    public String getStyleCaution() {
        if (isSnowy()) {
            return "긴 밑단이나 매끄러운 밑창은 피하는 편이 안전합니다.";
        }
        if (isRainy() || getPopValue() >= 60) {
            return "밝은 스웨이드, 긴 와이드 팬츠처럼 물 얼룩이 남기 쉬운 아이템은 피해주세요.";
        }
        if (isBadAirQuality()) {
            return "먼지가 붙기 쉬운 퍼 소재보다 세탁 쉬운 겉옷이 좋습니다.";
        }
        if (getWsdValue() >= 8.0) {
            return "가벼운 모자나 날리는 스카프는 고정감 있게 매치하세요.";
        }
        if (getTmpValue() >= 30) {
            return "두꺼운 데님이나 어두운 긴팔은 한낮 체감 온도를 크게 올릴 수 있어요.";
        }
        return "큰 변수는 적지만 오래 걷는 일정이면 신발 착화감을 우선하세요.";
    }

    public String getMailHeroColor() {
        if (isSnowy()) {
            return "#2f6f9f";
        }
        if (isRainy() || getPopValue() >= 60) {
            return "#1f4f7a";
        }
        if ("1".equals(sky)) {
            return "#d97706";
        }
        if ("4".equals(sky)) {
            return "#475569";
        }
        return "#0f766e";
    }

    public String getOutdoorAdvice() {
        double windSpeed = getWsdValue();
        int humidity = getRehValue();

        if (isBadAirQuality()) {
            return "미세먼지가 높아 야외 활동은 짧게 조절하는 편이 좋아요.";
        }
        if (isRainy() || isSnowy()) {
            return "야외 일정은 이동 동선을 짧게 잡아보세요.";
        }
        if (windSpeed >= 8.0) {
            return "바람이 강한 편이라 가벼운 물건은 잘 고정해주세요.";
        }
        if (getTmpValue() >= 30) {
            return "한낮 장시간 야외 활동은 피하는 편이 좋아요.";
        }
        if (humidity >= 80) {
            return "습도가 높아 체감이 답답할 수 있어요.";
        }
        return "야외 활동에 큰 무리는 없어 보여요.";
    }

    private String getAudienceLabel(AgeGroup ageGroup, GenderType gender) {
        String age = ageGroup == AgeGroup.NONE ? "" : ageGroup.getLabel() + " ";
        String genderLabel = gender == GenderType.NONE ? "" : gender.getLabel() + " ";
        String label = (age + genderLabel).trim();
        return label.isBlank() ? "당신에게는" : label + "에게는";
    }

    private boolean hasAirQuality() {
        return pm10Grade != null && !pm10Grade.isBlank()
                && pm25Grade != null && !pm25Grade.isBlank();
    }

    private boolean isBadAirQuality() {
        return getAirGradeValue(pm10Grade) >= 3 || getAirGradeValue(pm25Grade) >= 3;
    }

    private boolean isModerateAirQuality() {
        return getAirGradeValue(pm10Grade) == 2 || getAirGradeValue(pm25Grade) == 2;
    }

    private String getPm10GradeLabel() {
        return getAirGradeLabel(pm10Grade);
    }

    private String getPm25GradeLabel() {
        return getAirGradeLabel(pm25Grade);
    }

    private String getAirGradeLabel(String grade) {
        return switch (valueOrDash(grade)) {
            case "1" -> "좋음";
            case "2" -> "보통";
            case "3" -> "나쁨";
            case "4" -> "매우나쁨";
            default -> "정보 없음";
        };
    }

    private int getAirGradeValue(String grade) {
        return parseInt(grade, 0);
    }

    private boolean isRainy() {
        return "1".equals(pty) || "2".equals(pty) || "4".equals(pty);
    }

    private boolean isSnowy() {
        return "2".equals(pty) || "3".equals(pty);
    }

    private int getTmpValue() {
        return parseInt(tmp, 20);
    }

    private int getPopValue() {
        return parseInt(pop, 0);
    }

    private int getRehValue() {
        return parseInt(reh, 0);
    }

    private double getWsdValue() {
        return parseDouble(wsd, 0.0);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private double parseDouble(String value, double defaultValue) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String formatDustValue(String value, String grade) {
        String dustValue = valueOrDash(value);
        String gradeLabel = getAirGradeLabel(grade);
        if ("-".equals(dustValue)) {
            return "-";
        }
        if ("정보 없음".equals(gradeLabel)) {
            return dustValue + "㎍/㎥";
        }
        return dustValue + "㎍/㎥ · " + gradeLabel;
    }

    private String valueOrDash(String value) {
        if (value == null || value.isBlank() || "null".equalsIgnoreCase(value)) {
            return "-";
        }
        return value;
    }

    private void appendChecklist(StringBuilder checklist, String item) {
        if (!checklist.isEmpty()) {
            checklist.append(" / ");
        }
        checklist.append(item);
    }
}
