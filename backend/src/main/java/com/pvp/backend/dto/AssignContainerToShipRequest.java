package com.pvp.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AssignContainerToShipRequest {
    @NotEmpty
    private List<@NotNull Long> containerIds = new ArrayList<>();

    public List<Long> getContainerIds() { return containerIds; }

    public void setContainerIds(List<Long> containerIds) { this.containerIds = containerIds; }
}
