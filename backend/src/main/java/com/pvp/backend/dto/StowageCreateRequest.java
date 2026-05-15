package com.pvp.backend.dto;

import com.pvp.backend.model.StowageType;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class StowageCreateRequest {
    @NotNull
    private Long shipId;

    @NotNull
    private Long portId;

    private StowageType stowageType = StowageType.PAKROVIMAS;

    private List<Long> containerIds = new ArrayList<>();

    public Long getShipId() { return shipId; }

    public void setShipId(Long shipId) { this.shipId = shipId; }

    public Long getPortId() { return portId; }

    public void setPortId(Long portId) { this.portId = portId; }

    public StowageType getStowageType() { return stowageType; }

    public void setStowageType(StowageType stowageType) { this.stowageType = stowageType; }

    public List<Long> getContainerIds() { return containerIds; }

    public void setContainerIds(List<Long> containerIds) { this.containerIds = containerIds; }
}
