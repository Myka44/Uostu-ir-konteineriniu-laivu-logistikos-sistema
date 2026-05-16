package com.pvp.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class ShipmentContainerDto {
    private Long containerId;
    private String containerType;
    private boolean isHazardous;
    private String warningLabel;
    private double currentWeight;
    private double maxWeight;
    private double currentVolume;
    private double maxVolume;
    private double occupiedVolumePercent;
    private List<ShipmentItemDto> items = new ArrayList<>();

    public ShipmentContainerDto() {
    }

    public ShipmentContainerDto(Long containerId, String containerType, boolean isHazardous, String warningLabel,
                                double currentWeight, double maxWeight, double currentVolume, double maxVolume,
                                double occupiedVolumePercent, List<ShipmentItemDto> items) {
        this.containerId = containerId;
        this.containerType = containerType;
        this.isHazardous = isHazardous;
        this.warningLabel = warningLabel;
        this.currentWeight = currentWeight;
        this.maxWeight = maxWeight;
        this.currentVolume = currentVolume;
        this.maxVolume = maxVolume;
        this.occupiedVolumePercent = occupiedVolumePercent;
        this.items = items;
    }

    public Long getContainerId() {
        return containerId;
    }

    public void setContainerId(Long containerId) {
        this.containerId = containerId;
    }

    public String getContainerType() {
        return containerType;
    }

    public void setContainerType(String containerType) {
        this.containerType = containerType;
    }

    public boolean isHazardous() {
        return isHazardous;
    }

    public void setHazardous(boolean hazardous) {
        isHazardous = hazardous;
    }

    public String getWarningLabel() {
        return warningLabel;
    }

    public void setWarningLabel(String warningLabel) {
        this.warningLabel = warningLabel;
    }

    public double getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(double currentWeight) {
        this.currentWeight = currentWeight;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public double getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(double currentVolume) {
        this.currentVolume = currentVolume;
    }

    public double getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(double maxVolume) {
        this.maxVolume = maxVolume;
    }

    public double getOccupiedVolumePercent() {
        return occupiedVolumePercent;
    }

    public void setOccupiedVolumePercent(double occupiedVolumePercent) {
        this.occupiedVolumePercent = occupiedVolumePercent;
    }

    public List<ShipmentItemDto> getItems() {
        return items;
    }

    public void setItems(List<ShipmentItemDto> items) {
        this.items = items;
    }
}