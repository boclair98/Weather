package com.example.WebSideProject.dto;

import com.example.WebSideProject.Enum.AgeGroup;
import com.example.WebSideProject.Enum.GenderType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
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

    public String getForecastLabel() {
        if (date == null || date.length() != 8 || time == null || time.length() != 4) {
            return "아침 예보";
        }

        String label = periodLabel == null || periodLabel.isBlank() ? "아침" : periodLabel;
        return date.substring(4, 6) + "월 " + date.substring(6, 8) + "일 " + label + " 예보";
    }

    public String getSkyDescription() {
        return switch (sky) {
            case "1" -> "맑음 ☀️";
            case "3" -> "구름많음 ⛅";
            case "4" -> "흐림 ☁️";
            default  -> "알 수 없음";
        };
    }

    public String getPtyDescription() {
        return switch (pty) {
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
            return "비 소식이 있어요. 이동할 때 우산과 신발 상태를 한 번 더 챙겨주세요.";
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

        String weatherPoint = getWeatherStylePoint();
        String baseStyle = getBaseStyleByTemperature();
        String demographicStyle = getDemographicStyle(selectedAgeGroup, selectedGender);
        String rainPoint = getRainStylePoint();

        return weatherPoint + " " + baseStyle + " " + demographicStyle + rainPoint;
    }

    public String getOutdoorAdvice() {
        double windSpeed = getWsdValue();
        int humidity = getRehValue();

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

    private String getWeatherStylePoint() {
        if (isSnowy()) {
            return "눈 예보가 있어 보온성과 미끄럼 방지를 우선으로 잡아보세요.";
        }
        if (isRainy()) {
            return "비 예보가 있어 젖어도 관리가 쉬운 소재와 어두운 톤이 좋아요.";
        }
        if (getTmpValue() >= 28) {
            return "더운 날씨라 통풍과 가벼운 소재가 스타일의 핵심이에요.";
        }
        if (getTmpValue() <= 5) {
            return "추운 날씨라 레이어드와 보온 액세서리를 중심으로 잡아보세요.";
        }
        return "날씨 변수가 크지 않아 깔끔한 데일리룩으로 맞추기 좋아요.";
    }

    private String getBaseStyleByTemperature() {
        int temperature = getTmpValue();
        if (temperature >= 28) {
            return "린넨 셔츠, 반팔 니트, 와이드 팬츠처럼 몸에 붙지 않는 조합을 추천해요.";
        }
        if (temperature >= 20) {
            return "얇은 셔츠나 긴팔 티셔츠에 가벼운 아우터를 더하면 온도 변화에 대응하기 좋아요.";
        }
        if (temperature >= 12) {
            return "니트, 가디건, 자켓을 활용한 레이어드가 안정적이에요.";
        }
        if (temperature >= 6) {
            return "코트나 두께감 있는 자켓 안에 니트 또는 맨투맨을 받쳐 입어보세요.";
        }
        return "패딩이나 울 코트에 머플러, 장갑을 더해 체온을 지키는 쪽이 좋아요.";
    }

    private String getDemographicStyle(AgeGroup ageGroup, GenderType gender) {
        if (gender == GenderType.FEMALE) {
            return getFemaleStyle(ageGroup);
        }
        if (gender == GenderType.MALE) {
            return getMaleStyle(ageGroup);
        }
        return getNeutralStyle(ageGroup);
    }

    private String getFemaleStyle(AgeGroup ageGroup) {
        return switch (ageGroup) {
            case TEENS -> "10대 여성 스타일로는 컬러감 있는 스웨트셔츠, 데님, 스니커즈 조합이 잘 어울려요.";
            case TWENTIES -> "20대 여성 스타일로는 크롭 자켓, 셔츠, 슬랙스나 롱스커트로 가볍게 포인트를 주세요.";
            case THIRTIES -> "30대 여성 스타일로는 차분한 톤의 블라우스, 니트, 테일러드 자켓 조합이 깔끔해요.";
            case FORTIES -> "40대 여성 스타일로는 고급스러운 니트, 트렌치, 스트레이트 팬츠처럼 선이 정돈된 룩을 추천해요.";
            case FIFTIES_PLUS -> "50대 이상 여성 스타일로는 편안한 핏의 코트, 니트, 스카프 포인트가 잘 맞아요.";
            case NONE -> "여성 스타일로는 실루엣이 깔끔한 상의와 편한 하의를 맞추면 좋아요.";
        };
    }

    private String getMaleStyle(AgeGroup ageGroup) {
        return switch (ageGroup) {
            case TEENS -> "10대 남성 스타일로는 후드, 데님, 조거 팬츠와 스니커즈 조합이 활동적이에요.";
            case TWENTIES -> "20대 남성 스타일로는 오버핏 셔츠, 미니멀 자켓, 와이드 팬츠로 캐주얼하게 잡아보세요.";
            case THIRTIES -> "30대 남성 스타일로는 셔츠, 니트, 슬랙스, 블루종 조합이 단정하면서 실용적이에요.";
            case FORTIES -> "40대 남성 스타일로는 카라 니트, 치노 팬츠, 코트처럼 차분한 기본템이 좋아요.";
            case FIFTIES_PLUS -> "50대 이상 남성 스타일로는 가벼운 니트, 점퍼, 편한 팬츠로 보온과 활동성을 챙겨보세요.";
            case NONE -> "남성 스타일로는 기본 셔츠나 니트에 편한 팬츠를 맞추면 안정적이에요.";
        };
    }

    private String getNeutralStyle(AgeGroup ageGroup) {
        return switch (ageGroup) {
            case TEENS -> "10대 스타일로는 스웨트셔츠, 데님, 스니커즈처럼 활동적인 조합이 좋아요.";
            case TWENTIES -> "20대 스타일로는 셔츠, 가벼운 아우터, 와이드 팬츠로 트렌디하게 맞춰보세요.";
            case THIRTIES -> "30대 스타일로는 니트, 셔츠, 자켓을 활용한 단정한 데일리룩이 좋아요.";
            case FORTIES -> "40대 스타일로는 차분한 컬러와 좋은 소재감이 드러나는 기본템을 추천해요.";
            case FIFTIES_PLUS -> "50대 이상 스타일로는 편한 핏과 보온성을 중심으로, 스카프나 모자로 포인트를 주세요.";
            case NONE -> "선호 스타일을 따로 고르지 않았다면, 편한 핏의 기본템 위주로 준비해보세요.";
        };
    }

    private String getRainStylePoint() {
        if (isRainy() || getPopValue() >= 60) {
            return " 신발은 방수 소재나 어두운 색을 고르면 관리가 편합니다.";
        }
        return "";
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
}
