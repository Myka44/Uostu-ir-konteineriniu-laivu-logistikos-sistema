package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "routes")
public class Route {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 255)
    private RouteState state = RouteState.ACTIVE;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ship_id")
    private Ship ship;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "start_port_id")
    private Port startPort;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "end_port_id")
    private Port endPort;

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

    public RouteState getState() {
        return state;
    }

    public void setState(RouteState state) {
        this.state = state;
    }

    public Ship getShip() {
        return ship;
    }

    public void setShip(Ship ship) {
        this.ship = ship;
    }

    public Port getStartPort() {
        return startPort;
    }

    public void setStartPort(Port startPort) {
        this.startPort = startPort;
    }

    public Port getEndPort() {
        return endPort;
    }

    public void setEndPort(Port endPort) {
        this.endPort = endPort;
    }
}
