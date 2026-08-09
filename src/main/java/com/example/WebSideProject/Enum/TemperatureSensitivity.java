package com.example.WebSideProject.Enum;

public enum TemperatureSensitivity {
    NONE("보통"),
    COLD("추위를 많이 타요"),
    HEAT("더위를 많이 타요");

    private final String label;

    TemperatureSensitivity(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
