package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "route_segments")
public class RouteSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 255)
    private RouteSegmentState state;

    @NotNull
    private Integer sequenceNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    // destination port of this segment
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_port_id")
    private Port destinationPort;

    // starting port of this segment
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "start_port_id")
    private Port startPort;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public RouteSegmentState getState() { return state; }
    public void setState(RouteSegmentState state) { this.state = state; }

    public Integer getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(Integer sequenceNumber) { this.sequenceNumber = sequenceNumber; }

    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }

    public Port getDestinationPort() { return destinationPort; }
    public void setDestinationPort(Port destinationPort) { this.destinationPort = destinationPort; }

    public Port getStartPort() { return startPort; }
    public void setStartPort(Port startPort) { this.startPort = startPort; }
}
