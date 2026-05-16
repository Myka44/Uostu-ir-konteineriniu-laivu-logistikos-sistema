package com.pvp.backend.dto;

import java.util.ArrayList;
import java.util.List;

public class ShipmentResultDto {
    private Long orderId;
    private List<ShipmentContainerDto> containers = new ArrayList<>();

    public ShipmentResultDto() {
    }

    public ShipmentResultDto(Long orderId, List<ShipmentContainerDto> containers) {
        this.orderId = orderId;
        this.containers = containers;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public List<ShipmentContainerDto> getContainers() {
        return containers;
    }

    public void setContainers(List<ShipmentContainerDto> containers) {
        this.containers = containers;
    }
}