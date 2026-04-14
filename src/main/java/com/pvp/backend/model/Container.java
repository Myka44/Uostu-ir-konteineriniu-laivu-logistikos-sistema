package com.pvp.backend.model;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Container {
    @Id
    @GeneratedValue
    private Long id;
    private Double weight;
    private Double volume;
    private Double maxWeight;
    private Double maxVolume;
    private String warningLabel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getWarningLabel() {
        return warningLabel;
    }

    public void setWarningLabel(String warningLabel) {
        this.warningLabel = warningLabel;
    }
}
