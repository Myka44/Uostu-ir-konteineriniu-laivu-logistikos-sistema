package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stowage_plans")
public class Stowage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate data;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StowageType stowageType = StowageType.PAKROVIMAS;

    @NotNull
    @Enumerated(EnumType.STRING)
    private StowageStatus stowageStatus = StowageStatus.LAUKIA_PAKROVIMO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ship_id")
    private Ship ship;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "port_id")
    private Port port;

    @OneToMany(mappedBy = "stowage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContainerCoordinate> coordinates = new ArrayList<>();

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public LocalDate getData() { return data; }

    public void setData(LocalDate data) { this.data = data; }

    public StowageType getStowageType() { return stowageType; }

    public void setStowageType(StowageType stowageType) { this.stowageType = stowageType; }

    public StowageStatus getStowageStatus() { return stowageStatus; }

    public void setStowageStatus(StowageStatus stowageStatus) { this.stowageStatus = stowageStatus; }

    public Ship getShip() { return ship; }

    public void setShip(Ship ship) { this.ship = ship; }

    public Port getPort() { return port; }

    public void setPort(Port port) { this.port = port; }

    public List<ContainerCoordinate> getCoordinates() { return coordinates; }

    public void setCoordinates(List<ContainerCoordinate> coordinates) { this.coordinates = coordinates; }
}
