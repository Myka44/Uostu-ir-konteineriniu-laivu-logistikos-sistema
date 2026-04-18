package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "containers")
public class Container {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    private ContainerType type;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private Double weight;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private Double volume;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private Double maxWeight;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private Double maxVolume;

    @Enumerated(EnumType.STRING)
    private WarningLabel warningLabel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ContainerType getType() {
        return type;
    }

    public void setType(ContainerType type) {
        this.type = type;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Double getVolume() {
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
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
}