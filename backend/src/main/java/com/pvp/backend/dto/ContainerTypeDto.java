package com.pvp.backend.dto;

public class ContainerTypeDto {
    private String name;
    private double maxVolume;
    private double maxWeightKg;

    public ContainerTypeDto() {}

    public ContainerTypeDto(String name, double maxVolume, double maxWeightKg) {
        this.name = name;
        this.maxVolume = maxVolume;
        this.maxWeightKg = maxWeightKg;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(double maxVolume) {
        this.maxVolume = maxVolume;
    }

    public double getMaxWeightKg() {
        return maxWeightKg;
    }

    public void setMaxWeightKg(double maxWeightKg) {
        this.maxWeightKg = maxWeightKg;
    }
}
