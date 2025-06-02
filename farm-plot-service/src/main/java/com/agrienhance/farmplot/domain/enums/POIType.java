package com.agrienhance.farmplot.domain.enums;

public enum POIType {
    WATER_SOURCE("Water Source (e.g., well, pump, river access)"),
    BUILDING("Building (e.g., shed, house, storage)"),
    ACCESS_POINT("Access Point (e.g., gate, road entry)"),
    HAZARD("Hazard (e.g., rocky area, erosion point)"),
    SOIL_SENSOR("Soil Sensor Location"),
    WEATHER_STATION("Weather Station Location"),
    INFRASTRUCTURE("Other Infrastructure (e.g., fence line start/end, irrigation valve)"),
    OTHER("Other type of point of interest"),
    UNKNOWN("Type is unknown or not specified");

    private final String description;

    POIType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    // Optional: fromString method similar to LandTenureType
    public static POIType fromString(String text) {
        if (text != null) {
            for (POIType b : POIType.values()) {
                if (text.equalsIgnoreCase(b.name()) || text.equalsIgnoreCase(b.description)) {
                    return b;
                }
            }
        }
        return UNKNOWN; // Or throw an IllegalArgumentException
    }
}