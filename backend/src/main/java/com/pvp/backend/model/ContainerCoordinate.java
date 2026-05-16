package com.pvp.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;

@Entity
@Table(name = "container_coordinates")
public class ContainerCoordinate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "container_id")
    private Container container;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stowage_id")
    private Stowage stowage;

    @Min(0)
    private Integer lengthPosition;

    @Min(0)
    private Integer widthPosition;

    @Min(0)
    private Integer heightPosition;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Container getContainer() { return container; }

    public void setContainer(Container container) { this.container = container; }

    public Stowage getStowage() { return stowage; }

    public void setStowage(Stowage stowage) { this.stowage = stowage; }

    public Integer getLengthPosition() { return lengthPosition; }

    public void setLengthPosition(Integer lengthPosition) { this.lengthPosition = lengthPosition; }

    public Integer getWidthPosition() { return widthPosition; }

    public void setWidthPosition(Integer widthPosition) { this.widthPosition = widthPosition; }

    public Integer getHeightPosition() { return heightPosition; }

    public void setHeightPosition(Integer heightPosition) { this.heightPosition = heightPosition; }
}
