package com.pvp.backend.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum ContainerType {
    STANDARD(6.06, 2.44, 2.59, 24200),
    REFRIGERATED(6.06, 2.44, 2.59, 22000),
    TANK(6.06, 2.44, 2.59, 28000);

    private final double lengthMeters;
    private final double widthMeters;
    private final double heightMeters;
    private final double maxWeightKg;

    ContainerType(double lengthMeters, double widthMeters, double heightMeters, double maxWeightKg) {
        this.lengthMeters = lengthMeters;
        this.widthMeters = widthMeters;
        this.heightMeters = heightMeters;
        this.maxWeightKg = maxWeightKg;
    }

    public double getLengthMeters() {
        return lengthMeters;
    }

    public double getWidthMeters() {
        return widthMeters;
    }

    public double getHeightMeters() {
        return heightMeters;
    }

    public double getMaxVolume() {
        double vol = lengthMeters * widthMeters * heightMeters;
        return BigDecimal.valueOf(vol).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public double getMaxWeightKg() {
        return maxWeightKg;
    }
}