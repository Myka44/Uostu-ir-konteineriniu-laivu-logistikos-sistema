package com.pvp.backend.controller;


import com.pvp.backend.model.*;
import com.pvp.backend.repository.*;
import com.pvp.backend.model.Ship;
import com.pvp.backend.repository.ShipRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/ships")
@CrossOrigin(origins = "*")
public class ShipController {

    private final ShipRepository shipRepository;
    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final FuelAnalysisRepository fuelAnalysisRepository;
    private final CoordinateRepository coordinateRepository;
    private final PortRepository portRepository;

    public ShipController(ShipRepository shipRepository,
                          RouteRepository routeRepository,
                          RouteSegmentRepository routeSegmentRepository,
                          FuelAnalysisRepository fuelAnalysisRepository,
                          CoordinateRepository coordinateRepository,
                          PortRepository portRepository) {
        this.shipRepository = shipRepository;
        this.routeRepository = routeRepository;
        this.routeSegmentRepository = routeSegmentRepository;
        this.fuelAnalysisRepository = fuelAnalysisRepository;
        this.coordinateRepository = coordinateRepository;
        this.portRepository = portRepository;
    }

    @GetMapping
    public List<Ship> getAll() {
        return shipRepository.findAll();
    }

    @GetMapping("/{id}")
    public Ship getOne(@PathVariable Long id) {
        return shipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ship register(@Valid @RequestBody Ship ship) {
        validateShipData(ship);
        ship.setId(null);
        ship.setState(ShipState.ARRIVED);
        return shipRepository.save(ship);
    }

    @PutMapping("/{id}")
    public Ship update(@PathVariable Long id, @Valid @RequestBody Ship updated) {
        Ship existing = shipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));
        validateShipData(updated);
        existing.setName(updated.getName());
        existing.setType(updated.getType());
        existing.setCountry(updated.getCountry());
        existing.setWeight(updated.getWeight());
        existing.setCapacity(updated.getCapacity());
        existing.setBaseFuelConsumption(updated.getBaseFuelConsumption());
        existing.setFuelAmount(updated.getFuelAmount());
        existing.setLength(updated.getLength());
        existing.setWidth(updated.getWidth());
        existing.setHeight(updated.getHeight());
        existing.setState(updated.getState());
        // Preserve state and port — they are managed via dedicated endpoints
        if (updated.getPort() != null && updated.getPort().getId() != null) {
            portRepository.findById(updated.getPort().getId())
                    .ifPresent(existing::setPort);
        }
        return shipRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));

        if (hasShipDeparted(ship)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete a ship that is currently at sea. Wait for it to arrive first.");
        }

        // Cascade delete: coordinates → fuel analyses → route segments → routes
        List<Route> routes = routeRepository.findByShipId(id);
        for (Route route : routes) {
            List<RouteSegment> segments = routeSegmentRepository.findByRouteId(route.getId());
            for (RouteSegment seg : segments) {
                coordinateRepository.deleteByRouteSegmentId(seg.getId());
                fuelAnalysisRepository.deleteByRouteSegmentId(seg.getId());
            }
            routeSegmentRepository.deleteByRouteId(route.getId());
            routeRepository.delete(route);
        }
        shipRepository.delete(ship);
    }

    @PostMapping("/{id}/report-docking")
    public Ship reportDocking(@PathVariable Long id) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));

        if (!hasShipDeparted(ship)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ship has not departed. Cannot report docking arrival.");
        }

        ship.setState(ShipState.AWAITING_DOCKING);
        return shipRepository.save(ship);
    }

    @PostMapping("/{id}/receive")
    public Ship receiveShip(@PathVariable Long id, @RequestBody(required = false) ReceiveRequest req) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));

        if (ship.getState() != ShipState.AWAITING_DOCKING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ship must be in AWAITING_DOCKING state to be received.");
        }

        Route route = routeRepository.findByShipIdAndState(id, RouteState.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active route found."));

        RouteSegment ongoingSegment = routeSegmentRepository
                .findFirstByRouteIdAndStateOrderBySequenceNumber(route.getId(), RouteSegmentState.ONGOING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No ongoing route segment."));

        Port destinationPort = ongoingSegment.getDestinationPort();
        if (destinationPort == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Segment has no destination port.");
        }

        if (!hasEmptyDock(destinationPort)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Destination port has no empty docks available.");
        }

        ship.setState(ShipState.ARRIVED);
        ship.setPort(destinationPort);
        shipRepository.save(ship);

        ongoingSegment.setState(RouteSegmentState.VISITED);
        routeSegmentRepository.save(ongoingSegment);

        boolean hasUnvisited = routeSegmentRepository
                .findFirstByRouteIdAndStateOrderBySequenceNumber(route.getId(), RouteSegmentState.UNVISITED)
                .isPresent()
                || routeSegmentRepository
                .findFirstByRouteIdAndStateOrderBySequenceNumber(route.getId(), RouteSegmentState.ONGOING)
                .isPresent();

        if (!hasUnvisited) {
            route.setState(RouteState.FINISHED);
            routeRepository.save(route);
        }
        // else: trigger Perskaičiuoti maršrutą (recalculate — handled client-side or future impl)

        return ship;
    }

    @PostMapping("/{id}/depart")
    public Ship departShip(@PathVariable Long id) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));

        Route route = routeRepository.findByShipIdAndState(id, RouteState.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ship has no active route. Generate a route before departing."));

        if (!isLoaded(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ship is not loaded. Complete loading before departing.");
        }

        if (!isAccepted(ship)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ship is not in ARRIVED state. Cannot depart.");
        }

        RouteSegment nextSegment = routeSegmentRepository
                .findFirstByRouteIdAndStateOrderBySequenceNumber(route.getId(), RouteSegmentState.UNVISITED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No unvisited segments remaining."));

        Optional<FuelAnalysis> fuelOpt = fuelAnalysisRepository.findByRouteSegmentId(nextSegment.getId());
        if (fuelOpt.isPresent()) {
            FuelAnalysis fa = fuelOpt.get();
            if (!hasEnoughFuel(ship, fa.getPredictedFuelConsumption())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Not enough fuel for the next segment.");
            }
        }

        ship.setState(ShipState.DEPARTED);
        ship.setPort(null);
        shipRepository.save(ship);

        nextSegment.setState(RouteSegmentState.ONGOING);
        routeSegmentRepository.save(nextSegment);

        return ship;
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────
    public static class ReceiveRequest {
        public Long portId;
    }

    private void validateShipData(Ship s) {
        if (s.getName() == null || s.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ship name is required.");
        }
        if (s.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ship type is required.");
        }
        if (s.getCapacity() == null || s.getCapacity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacity must be at least 1.");
        }
        if (s.getFuelAmount() == null || s.getFuelAmount() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fuel amount cannot be negative.");
        }
    }

    private boolean isAccepted(Ship ship) {
        return ship.getState() == ShipState.ARRIVED;
    }

    private boolean hasShipDeparted(Ship ship) {
        return ship.getState() == ShipState.DEPARTED;
    }

    private boolean hasEnoughFuel(Ship ship, double required) {
        return ship.getFuelAmount() != null && ship.getFuelAmount() >= required;
    }

    private boolean hasEmptyDock(Port port) {
        if (port.getDockCount() == null) return true; // assume available if not configured
        long shipsAtPort = shipRepository.findAll().stream()
                .filter(s -> s.getPort() != null && s.getPort().getId().equals(port.getId()))
                .count();
        return shipsAtPort < port.getDockCount();
    }

    private boolean isLoaded(Long shipId) {
        // A ship is loaded if it has at least one stowage in PAKRAUTA state
        return true; // TODO: integrate StowageRepository check
    }
}