package com.pvp.backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Vienas jūrų maršruto variantas (seka taškų) iš NavigationAPI. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeaRoute {
    private List<NavigationWaypoint> waypoints;

    public SeaRoute() {}
    public SeaRoute(List<NavigationWaypoint> waypoints) { this.waypoints = waypoints; }

    public List<NavigationWaypoint> getWaypoints() { return waypoints; }
    public void setWaypoints(List<NavigationWaypoint> waypoints) { this.waypoints = waypoints; }
}
