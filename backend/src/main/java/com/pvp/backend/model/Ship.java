package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "ships")
public class Ship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @Enumerated(EnumType.STRING)
    private ShipType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 255)
    private Country country;

    private String registrationCountry;

    @DecimalMin(value = "0.0", inclusive = true)
    private Double weight;

    @Min(1)
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    @Column(length = 255)
    private ShipState state = ShipState.DISPATCHER;

    @Enumerated(EnumType.STRING)
    @Column(length = 255)
    private ShipStatus shipStatus = ShipStatus.DISPECERIS;

    @DecimalMin(value = "0.0", inclusive = true)
    private Double baseFuelConsumption;

    @DecimalMin(value = "0.0", inclusive = true)
    private Double fuelAmount;

    @Min(1)
    private Integer length = 10;

    @Min(1)
    private Integer width = 6;

    @Min(1)
    private Integer height = 4;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "port_id")
    private Port port;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ShipType getType() {
        return type;
    }

    public void setType(ShipType type) {
        this.type = type;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
        this.registrationCountry = country == null ? null : country.name();
    }

    public String getRegistrationCountry() {
        return registrationCountry;
    }

    public void setRegistrationCountry(String registrationCountry) {
        this.registrationCountry = registrationCountry;
        if (registrationCountry != null) {
            try {
                this.country = Country.valueOf(registrationCountry);
            } catch (IllegalArgumentException ignored) {
                // Keep custom registrationCountry strings without forcing them into the enum.
            }
        }
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public ShipState getState() {
        return state;
    }

    public void setState(ShipState state) {
        this.state = state;
        if (state == ShipState.ARRIVED || state == ShipState.ACCEPTED) {
            this.shipStatus = ShipStatus.PRIIMTAS;
        } else if (state == ShipState.DEPARTED || state == ShipState.SENT) {
            this.shipStatus = ShipStatus.ISSIUSTAS;
        }
    }

    public ShipStatus getShipStatus() {
        return shipStatus;
    }

    public void setShipStatus(ShipStatus shipStatus) {
        this.shipStatus = shipStatus;
        if (shipStatus == ShipStatus.PRIIMTAS) {
            this.state = ShipState.ARRIVED;
        } else if (shipStatus == ShipStatus.ISSIUSTAS) {
            this.state = ShipState.DEPARTED;
        }
    }

    public Double getBaseFuelConsumption() {
        return baseFuelConsumption;
    }

    public void setBaseFuelConsumption(Double baseFuelConsumption) {
        this.baseFuelConsumption = baseFuelConsumption;
    }

    public Double getFuelAmount() {
        return fuelAmount;
    }

    public void setFuelAmount(Double fuelAmount) {
        this.fuelAmount = fuelAmount;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Port getPort() {
        return port;
    }

    public void setPort(Port port) {
        this.port = port;
    }
}
