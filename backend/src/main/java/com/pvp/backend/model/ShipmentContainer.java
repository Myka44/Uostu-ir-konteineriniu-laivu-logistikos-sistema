package com.pvp.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "shipment_containers")
public class ShipmentContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long orderId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ContainerType type;

    @NotNull
    private Double currentWeight = 0.0;

    @NotNull
    private Double currentVolume = 0.0;

    @NotNull
    private Double maxWeight;

    @NotNull
    private Double maxVolume;

    @Enumerated(EnumType.STRING)
    private WarningLabel warningLabel;

    private boolean isHazardous = false;

    public ShipmentContainer() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }

    public Double getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(Double currentWeight) {
        this.currentWeight = currentWeight;
    }

    public Double getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(Double currentVolume) {
        this.currentVolume = currentVolume;
    }

    public Double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(Double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public Double getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(Double maxVolume) {
        this.maxVolume = maxVolume;
    }

    public WarningLabel getWarningLabel() {
        return warningLabel;
    }

    public void setWarningLabel(WarningLabel warningLabel) {
        this.warningLabel = warningLabel;
    }

    public boolean isHazardous() {
        return isHazardous;
    }

    public void setHazardous(boolean hazardous) {
        isHazardous = hazardous;
    }

    public double getOccupiedVolumePercent() {
        if (maxVolume == null || maxVolume <= 0.0) {
            return 0.0;
        }
        double volume = currentVolume == null ? 0.0 : currentVolume;
        return (volume / maxVolume) * 100.0;
    }

    public boolean hasCapacityFor(double weight, double volume) {
        double currentWeightValue = currentWeight == null ? 0.0 : currentWeight;
        double currentVolumeValue = currentVolume == null ? 0.0 : currentVolume;
        double maxWeightValue = maxWeight == null ? 0.0 : maxWeight;
        double maxVolumeValue = maxVolume == null ? 0.0 : maxVolume;
        return currentWeightValue + weight <= maxWeightValue + 0.000001
                && currentVolumeValue + volume <= maxVolumeValue + 0.000001;
    }
}