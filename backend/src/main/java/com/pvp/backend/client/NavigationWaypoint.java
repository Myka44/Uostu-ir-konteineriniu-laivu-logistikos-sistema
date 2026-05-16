package com.pvp.backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Vienas GPS taškas iš NavigationAPI atsakymo. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NavigationWaypoint {
    private double lat;
    private double lon;

    public NavigationWaypoint() {}
    public NavigationWaypoint(double lat, double lon) { this.lat = lat; this.lon = lon; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLon() { return lon; }
    public void setLon(double lon) { this.lon = lon; }
}
