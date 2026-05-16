package com.pvp.backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Pilnas NavigationAPI atsakymas su keliais maršruto variantais. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NavigationApiResponse {
    private List<SeaRoute> routes;

    public NavigationApiResponse() {}
    public NavigationApiResponse(List<SeaRoute> routes) { this.routes = routes; }

    public List<SeaRoute> getRoutes() { return routes; }
    public void setRoutes(List<SeaRoute> routes) { this.routes = routes; }
}
