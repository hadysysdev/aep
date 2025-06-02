package com.agrienhance.farmplot.domain.enums;

public enum LandTenureType {
    OWNED("Owned by cultivator/farm owner"),
    LEASED("Leased from another party"),
    COMMUNAL_ACCESS("Communal land with access rights"),
    CUSTOM_AGREEMENT("Custom or informal agreement"),
    UNKNOWN("Tenure status is unknown or not specified");

    private final String description;

    LandTenureType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // Optional: a static method to get enum from string, useful for DTO mapping
    public static LandTenureType fromString(String text) {
        if (text != null) {
            for (LandTenureType b : LandTenureType.values()) {
                if (text.equalsIgnoreCase(b.name()) || text.equalsIgnoreCase(b.description)) {
                    return b;
                }
            }
        }
        return UNKNOWN; // Or throw an IllegalArgumentException
    }
}