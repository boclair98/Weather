package com.example.WebSideProject.Enum;

public enum GenderType {
    NONE("선택 안 함"),
    FEMALE("여성"),
    MALE("남성");

    private final String label;

    GenderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
