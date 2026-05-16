package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "coordinates")
public class Coordinate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    private Double azimuth;

    @NotNull
    private Integer sequenceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_segment_id", nullable = false)
    private RouteSegment routeSegment;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getAzimuth() { return azimuth; }
    public void setAzimuth(Double azimuth) { this.azimuth = azimuth; }

    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public RouteSegment getRouteSegment() { return routeSegment; }
    public void setRouteSegment(RouteSegment routeSegment) { this.routeSegment = routeSegment; }
}
