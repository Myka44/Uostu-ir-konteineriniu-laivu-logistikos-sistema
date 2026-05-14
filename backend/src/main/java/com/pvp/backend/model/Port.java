package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "ports")
public class Port {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    private Integer dockCount;

    @Enumerated(EnumType.STRING)
    private Country country;

    private Integer containerCapacity;

    private Boolean open = true;

    private Integer length;
    private Integer width;
    private Integer height;

    /** GPS koordinatės – naudojamos NavigationAPI ir orų prognozių užklausoms */
    private Double latitude;
    private Double longitude;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getDockCount() { return dockCount; }
    public void setDockCount(Integer dockCount) { this.dockCount = dockCount; }

    public Country getCountry() { return country; }
    public void setCountry(Country country) { this.country = country; }

    public Integer getContainerCapacity() { return containerCapacity; }
    public void setContainerCapacity(Integer containerCapacity) { this.containerCapacity = containerCapacity; }

    public Boolean getOpen() { return open; }
    public void setOpen(Boolean open) { this.open = open; }

    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
