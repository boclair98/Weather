package com.example.WebSideProject.Enum;

public enum ActivityType {
    DAILY("편안한 일상"),
    COMMUTE("출근·등교"),
    OUTDOOR("산책·야외 활동"),
    FORMAL("미팅·격식 일정");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
