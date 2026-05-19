package com.pvp.backend.controller;

import com.pvp.backend.client.NavigationApiClient;
import com.pvp.backend.client.NavigationApiResponse;
import com.pvp.backend.client.WeatherForecastApiClient;
import com.pvp.backend.client.WeatherData;
import com.pvp.backend.model.*;
import com.pvp.backend.repository.*;
import com.pvp.backend.service.RouteOptimizationService;
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
    private final StowageRepository stowageRepository;
    private final WeatherForecastApiClient weatherClient;
    private final NavigationApiClient navigationClient;
    private final RouteOptimizationService optimizationService;

    public ShipController(ShipRepository shipRepository,
                          RouteRepository routeRepository,
                          RouteSegmentRepository routeSegmentRepository,
                          FuelAnalysisRepository fuelAnalysisRepository,
                          CoordinateRepository coordinateRepository,
                          PortRepository portRepository,
                          StowageRepository stowageRepository,
                          WeatherForecastApiClient weatherClient,
                          NavigationApiClient navigationClient,
                          RouteOptimizationService optimizationService) {
        this.shipRepository = shipRepository;
        this.routeRepository = routeRepository;
        this.routeSegmentRepository = routeSegmentRepository;
        this.fuelAnalysisRepository = fuelAnalysisRepository;
        this.coordinateRepository = coordinateRepository;
        this.portRepository = portRepository;
        this.stowageRepository = stowageRepository;
        this.weatherClient = weatherClient;
        this.navigationClient = navigationClient;
        this.optimizationService = optimizationService;
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

    @GetMapping("/{id}/stowages")
    public List<Stowage> getStowagesForShip(@PathVariable Long id) {
        if (!shipRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found");
        return stowageRepository.findByShipId(id);
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
        if (updated.getState() != null) existing.setState(updated.getState());
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

        if (ship.getState() != ShipState.ARRIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Laivą galima ištrinti tik kai jo būsena yra ARRIVED (prisijungęs prie uosto).");
        }

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

        if (ship.getState() != ShipState.DEPARTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Laivas turi būti DEPARTED būsenos, kad galėtų pranešti apie atvykimą.");
        }

        ship.setState(ShipState.AWAITING_DOCKING);
        return shipRepository.save(ship);
    }

    @PostMapping("/{id}/receive")
    public ReceiveShipResponse receiveShip(@PathVariable Long id) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));

        if (ship.getState() != ShipState.AWAITING_DOCKING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Laivas turi būti AWAITING_DOCKING būsenos, kad būtų galima jį priimti.");
        }

        Route route = routeRepository.findByShipIdAndState(id, RouteState.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active route found."));

        RouteSegment ongoingSegment = routeSegmentRepository
                .findFirstByRouteIdAndStateOrderBySequenceNumber(route.getId(), RouteSegmentState.ONGOING)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No ongoing route segment."));

        Port destinationPort = ongoingSegment.getDestinationPort();
        if (destinationPort == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Segment has no destination port.");

        if (!hasEmptyDock(destinationPort))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Destination port has no empty docks available.");

        // ── 1. Atnaujinti laivo kurą pagal numatytą sąnaudą ──────────────────
        Optional<FuelAnalysis> completedFa = fuelAnalysisRepository.findByRouteSegmentId(ongoingSegment.getId());
        completedFa.ifPresent(fa -> {
            double consumed = fa.getPredictedFuelConsumption() != null ? fa.getPredictedFuelConsumption() : 0.0;
            double newFuel = Math.max(0.0, (ship.getFuelAmount() != null ? ship.getFuelAmount() : 0.0) - consumed);
            ship.setFuelAmount(round2(newFuel));
        });

        // ── 2. Pakeisti laivo būseną ir uostą ────────────────────────────────
        ship.setState(ShipState.ARRIVED);
        ship.setPort(destinationPort);
        shipRepository.save(ship);

        ongoingSegment.setState(RouteSegmentState.VISITED);
        routeSegmentRepository.save(ongoingSegment);

        // ── 3. Patikrinti ar liko neaplankytų segmentų ───────────────────────
        Optional<RouteSegment> nextUnvisited = routeSegmentRepository
                .findFirstByRouteIdAndStateOrderBySequenceNumber(route.getId(), RouteSegmentState.UNVISITED);

        if (nextUnvisited.isEmpty()) {
            route.setState(RouteState.FINISHED);
            routeRepository.save(route);
            return new ReceiveShipResponse(ship, false, false, null, ship.getFuelAmount());
        }

        // ── 4. Automatiškai perskaičiuoti likusius maršruto segmentus ────────
        try {
            double[] shipCoords = portToLatLon(destinationPort);
            WeatherData weather = weatherClient.getWeatherData(shipCoords[0], shipCoords[1]);

            List<RouteSegment> remainingSegments = routeSegmentRepository
                    .findByRouteIdOrderBySequenceNumber(route.getId())
                    .stream()
                    .filter(s -> s.getState() != RouteSegmentState.VISITED)
                    .toList();

            double remainingFuel = ship.getFuelAmount() != null ? ship.getFuelAmount() : 0.0;

            for (RouteSegment seg : remainingSegments) {
                double[] fromCoords = portToLatLon(seg.getStartPort());
                double[] toCoords   = portToLatLon(seg.getDestinationPort());
                NavigationApiResponse navResponse = navigationClient.getSeaRoutes(
                        fromCoords[0], fromCoords[1], toCoords[0], toCoords[1]);

                double fuelForSegment = optimizationService.findAndAssignBestSegmentVariant(
                        seg, navResponse, weather, ship);

                remainingFuel -= fuelForSegment;

                FuelAnalysis fa = fuelAnalysisRepository.findByRouteSegmentId(seg.getId())
                        .orElse(new FuelAnalysis());
                fa.setRouteSegment(seg);
                fa.setPredictedFuelConsumption(round2(fuelForSegment));
                fa.setPredictedFuelRemaining(round2(Math.max(0, remainingFuel)));
                fuelAnalysisRepository.save(fa);
            }

            // ── 5. Patikrinti ar pakanka kuro sekančiam segmentui ─────────────
            RouteSegment first = nextUnvisited.get();
            Optional<FuelAnalysis> nextFa = fuelAnalysisRepository.findByRouteSegmentId(first.getId());
            Double nextFuelRequired = nextFa.map(FuelAnalysis::getPredictedFuelConsumption).orElse(null);
            boolean sufficient = nextFuelRequired != null && ship.getFuelAmount() != null
                    && ship.getFuelAmount() >= nextFuelRequired;

            return new ReceiveShipResponse(ship, true, sufficient, nextFuelRequired, ship.getFuelAmount());

        } catch (Exception e) {
            // Perskaičiavimas nepavyko – grąžiname laivą su žinoma informacija
            return new ReceiveShipResponse(ship, true, false, null, ship.getFuelAmount());
        }
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

        if (ship.getState() != ShipState.ARRIVED) {
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
                        ship.getName() + " has insufficient fuel for the next segment. " +
                                "Available: " + (ship.getFuelAmount() != null ? ship.getFuelAmount().intValue() : 0) + " l, " +
                                "required: " + (fa.getPredictedFuelConsumption() != null ? fa.getPredictedFuelConsumption().intValue() : "?") + " l.");
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
    public record ReceiveShipResponse(
            Ship ship,
            boolean hasActiveRoute,
            boolean sufficientFuel,
            Double nextSegmentFuelRequired,
            Double currentFuelAmount
    ) {}

    // ── Pagalbiniai metodai ───────────────────────────────────────────────────
    private void validateShipData(Ship s) {
        if (s.getName() == null || s.getName().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ship name is required.");
        if (s.getType() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ship type is required.");
        if (s.getCapacity() == null || s.getCapacity() < 1)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Capacity must be at least 1.");
        if (s.getFuelAmount() == null || s.getFuelAmount() < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fuel amount cannot be negative.");
    }

    private boolean hasEnoughFuel(Ship ship, double required) {
        return ship.getFuelAmount() != null && ship.getFuelAmount() >= required;
    }

    private boolean hasEmptyDock(Port port) {
        if (port.getDockCount() == null) return true;
        long shipsAtPort = shipRepository.findAll().stream()
                .filter(s -> s.getPort() != null && s.getPort().getId().equals(port.getId()))
                .count();
        return shipsAtPort < port.getDockCount();
    }

    private boolean isLoaded(Long shipId) {
        return true; // TODO: integrate StowageRepository check
    }

    private double[] portToLatLon(Port port) {
        if (port == null) return new double[]{57.5, 20.0};
        if (port.getLatitude() != null && port.getLongitude() != null
                && (port.getLatitude() != 0.0 || port.getLongitude() != 0.0)) {
            return new double[]{port.getLatitude(), port.getLongitude()};
        }
        return switch (port.getName()) {
            case "Klaipėda" -> new double[]{55.7033, 21.1396};
            case "Riga"     -> new double[]{56.9496, 24.1052};
            case "Tallinn"  -> new double[]{59.4370, 24.7536};
            case "Gdansk"   -> new double[]{54.3520, 18.6466};
            default         -> new double[]{57.5, 20.0};
        };
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
