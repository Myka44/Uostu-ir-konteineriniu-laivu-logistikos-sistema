package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "fuel_analyses")
public class FuelAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Double predictedFuelConsumption;

    @NotNull
    private Double predictedFuelRemaining;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_segment_id", nullable = false)
    private RouteSegment routeSegment;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getPredictedFuelConsumption() { return predictedFuelConsumption; }
    public void setPredictedFuelConsumption(Double v) { this.predictedFuelConsumption = v; }

    public Double getPredictedFuelRemaining() { return predictedFuelRemaining; }
    public void setPredictedFuelRemaining(Double v) { this.predictedFuelRemaining = v; }

    public RouteSegment getRouteSegment() { return routeSegment; }
    public void setRouteSegment(RouteSegment routeSegment) { this.routeSegment = routeSegment; }
}
