package com.pvp.backend.controller;

import com.pvp.backend.dto.AssignContainerToShipRequest;
import com.pvp.backend.dto.StowageCreateRequest;
import com.pvp.backend.dto.StowagePlanResponse;
import com.pvp.backend.model.Container;
import com.pvp.backend.model.Port;
import com.pvp.backend.model.Ship;
import com.pvp.backend.model.Stowage;
import com.pvp.backend.service.StowageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stowage-plans")
@CrossOrigin(origins = "*")
public class StowageController {
    private final StowageService stowageService;

    public StowageController(StowageService stowageService) {
        this.stowageService = stowageService;
    }

    @GetMapping
    public List<Stowage> getStowagePlans() {
        return stowageService.getStowagePlans();
    }

    @GetMapping("/{id}")
    public Stowage open(@PathVariable Long id) {
        return stowageService.getStowagePlan(id);
    }

    @GetMapping("/load-ships")
    public List<Ship> getLoadShips() {
        return stowageService.getLoadShips();
    }

    @GetMapping("/ports")
    public List<Port> getPorts() {
        return stowageService.getPorts();
    }

    @GetMapping("/ships/{shipId}/containers")
    public List<Container> getContainersForShip(@PathVariable Long shipId) {
        return stowageService.getContainersForShip(shipId);
    }

    @GetMapping("/ships/{shipId}/assignable-containers")
    public List<Container> getAssignableContainers(@PathVariable Long shipId) {
        return stowageService.getAssignableContainers(shipId);
    }

    @PostMapping("/ships/{shipId}/containers")
    public List<Container> assignContainerToShip(
            @PathVariable Long shipId,
            @Valid @RequestBody AssignContainerToShipRequest request
    ) {
        return stowageService.assignContainerToShip(shipId, request.getContainerIds());
    }

    @DeleteMapping("/ships/{shipId}/containers/{containerId}")
    public Container unassignContainerFromShip(@PathVariable Long shipId, @PathVariable Long containerId) {
        return stowageService.unassignContainerFromShip(shipId, containerId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StowagePlanResponse submitStowageCreate(@Valid @RequestBody StowageCreateRequest request) {
        return stowageService.submitStowageCreate(request);
    }

    @PostMapping("/{planId}/load-ship/{shipId}")
    public StowagePlanResponse selectPlan(@PathVariable Long planId, @PathVariable Long shipId) {
        return stowageService.selectPlan(planId, shipId);
    }
}
