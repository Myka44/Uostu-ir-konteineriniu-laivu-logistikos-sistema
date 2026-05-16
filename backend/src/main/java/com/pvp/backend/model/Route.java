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
    @Enumerated(EnumType.STRING)
    @Column(length = 255)
    private RouteState state;

    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ship_id", nullable = false)
    private Ship ship;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RouteState getState() { return state; }
    public void setState(RouteState state) { this.state = state; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Ship getShip() { return ship; }
    public void setShip(Ship ship) { this.ship = ship; }
}
