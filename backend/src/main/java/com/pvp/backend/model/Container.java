package com.pvp.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    /**
     * A container can be assigned to one ship before a stowage plan is created.
     * Stowage creation then uses only containers whose ship_id matches the selected ship.
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ship_id")
    private Ship ship;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public ContainerType getType() { return type; }

    public void setType(ContainerType type) { this.type = type; }

    public Double getWeight() { return weight; }

    public void setWeight(Double weight) { this.weight = weight; }

    public Double getVolume() { return volume; }

    public void setVolume(Double volume) { this.volume = volume; }

    public Double getMaxWeight() { return maxWeight; }

    public void setMaxWeight(Double maxWeight) { this.maxWeight = maxWeight; }

    public Double getMaxVolume() { return maxVolume; }

    public void setMaxVolume(Double maxVolume) { this.maxVolume = maxVolume; }

    public WarningLabel getWarningLabel() { return warningLabel; }

    public void setWarningLabel(WarningLabel warningLabel) { this.warningLabel = warningLabel; }

    public Ship getShip() { return ship; }

    public void setShip(Ship ship) { this.ship = ship; }
}
