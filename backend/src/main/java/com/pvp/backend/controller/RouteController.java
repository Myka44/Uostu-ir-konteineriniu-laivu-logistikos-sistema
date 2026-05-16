package com.pvp.backend.controller;

import com.pvp.backend.client.*;
import com.pvp.backend.model.*;
import com.pvp.backend.repository.*;
import com.pvp.backend.service.RouteOptimizationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
public class RouteController {

    private final RouteRepository routeRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final CoordinateRepository coordinateRepository;
    private final FuelAnalysisRepository fuelAnalysisRepository;
    private final ShipRepository shipRepository;
    private final PortRepository portRepository;
    private final WeatherForecastApiClient weatherClient;
    private final NavigationApiClient navigationClient;
    private final RouteOptimizationService optimizationService;

    public RouteController(RouteRepository routeRepository,
                           RouteSegmentRepository routeSegmentRepository,
                           CoordinateRepository coordinateRepository,
                           FuelAnalysisRepository fuelAnalysisRepository,
                           ShipRepository shipRepository,
                           PortRepository portRepository,
                           WeatherForecastApiClient weatherClient,
                           NavigationApiClient navigationClient,
                           RouteOptimizationService optimizationService) {
        this.routeRepository = routeRepository;
        this.routeSegmentRepository = routeSegmentRepository;
        this.coordinateRepository = coordinateRepository;
        this.fuelAnalysisRepository = fuelAnalysisRepository;
        this.shipRepository = shipRepository;
        this.portRepository = portRepository;
        this.weatherClient = weatherClient;
        this.navigationClient = navigationClient;
        this.optimizationService = optimizationService;
    }

    // ── Peržiūrėti maršrutus ─────────────────────────────────────────────────
    @GetMapping("/api/routes")
    public List<Route> getAllRoutes() {
        return routeRepository.findAll();
    }

    @GetMapping("/api/routes/{id}")
    public RouteDetailResponse getRoute(@PathVariable Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));
        List<RouteSegment> segments = routeSegmentRepository.findByRouteIdOrderBySequenceNumber(id);
        List<FuelAnalysis> fuelAnalyses = segments.stream()
                .map(s -> fuelAnalysisRepository.findByRouteSegmentId(s.getId()))
                .filter(Optional::isPresent).map(Optional::get).toList();
        return new RouteDetailResponse(route, segments, fuelAnalyses);
    }

    @GetMapping("/api/ships/{shipId}/routes")
    public List<Route> getRoutesForShip(@PathVariable Long shipId) {
        if (!shipRepository.existsById(shipId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found");
        return routeRepository.findByShipId(shipId);
    }

    @GetMapping("/api/routes/{id}/segments")
    public List<SegmentDetailResponse> getSegments(@PathVariable Long id) {
        if (!routeRepository.existsById(id))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found");
        return routeSegmentRepository.findByRouteIdOrderBySequenceNumber(id).stream()
                .map(seg -> new SegmentDetailResponse(
                        seg,
                        coordinateRepository.findByRouteSegmentIdOrderBySequenceNumber(seg.getId()),
                        fuelAnalysisRepository.findByRouteSegmentId(seg.getId()).orElse(null)))
                .toList();
    }

    @GetMapping("/api/ships/{shipId}/active-route")
    public RouteDetailResponse getActiveRoute(@PathVariable Long shipId) {
        Route route = routeRepository.findByShipIdAndState(shipId, RouteState.ACTIVE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No active route for this ship."));
        return getRoute(route.getId());
    }

    // ── Sugeneruoti maršrutą (su WeatherForecastAPI + NavigationAPI) ──────────
    @PostMapping("/api/ships/{shipId}/generate-route")
    @ResponseStatus(HttpStatus.CREATED)
    public Route generateRoute(@PathVariable Long shipId,
                               @RequestBody GenerateRouteRequest req) {
        Ship ship = shipRepository.findById(shipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));

        if (ship.getState() != ShipState.ARRIVED)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Laivas turi būti ARRIVED būsenos, kad būtų galima generuoti maršrutą.");

        if (routeRepository.findByShipIdAndState(shipId, RouteState.ACTIVE).isPresent())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Laivas jau turi aktyvų maršrutą.");

        if (req.destinationPortIds == null || req.destinationPortIds.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reikalingas bent vienas paskirties uostas.");

        Port startPort = ship.getPort();
        if (startPort == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Laivui nepriskirtas uostas. Priskirkite pradžios uostą.");

        List<Port> destinationPorts = req.destinationPortIds.stream()
                .map(pid -> portRepository.findById(pid)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Uostas " + pid + " nerastas")))
                .toList();

        // ── 1. Gauti orų duomenis (WeatherForecastAPI) ────────────────────────
        // Naudojami pradžios uosto koordinatės; Port modelyje koordinačių nėra,
        // todėl naudojame fiksuotas Baltijos jūros koordinates kaip pradinę vietą
        double[] startCoords = portToLatLon(startPort);
        WeatherData weather = weatherClient.getWeatherData(startCoords[0], startCoords[1]);

        // ── 2. Sukurti maršrutą ───────────────────────────────────────────────
        Route route = new Route();
        route.setShip(ship);
        route.setState(RouteState.ACTIVE);
        route.setName(req.routeName != null ? req.routeName :
                ship.getName() + ": " + startPort.getName() + " → "
                        + destinationPorts.get(destinationPorts.size() - 1).getName());
        Route savedRoute = routeRepository.save(route);

        // ── 3. Kiekvienam segmentui: NavigationAPI + optimizacija ─────────────
        Port current = startPort;
        double remainingFuel = ship.getFuelAmount() != null ? ship.getFuelAmount() : 0.0;

        for (int i = 0; i < destinationPorts.size(); i++) {
            Port dest = destinationPorts.get(i);

            RouteSegment segment = new RouteSegment();
            segment.setRoute(savedRoute);
            segment.setStartPort(current);
            segment.setDestinationPort(dest);
            segment.setSequenceNumber(i + 1);
            segment.setState(RouteSegmentState.UNVISITED);
            RouteSegment savedSeg = routeSegmentRepository.save(segment);

            // Gauti koordinates iš NavigationAPI
            double[] fromCoords = portToLatLon(current);
            double[] toCoords   = portToLatLon(dest);
            NavigationApiResponse navResponse = navigationClient.getSeaRoutes(
                    fromCoords[0], fromCoords[1], toCoords[0], toCoords[1]);

            // Rasti optimalų segmento variantą ir priskirti koordinates
            double fuelForSegment = optimizationService.findAndAssignBestSegmentVariant(
                    savedSeg, navResponse, weather, ship);

            remainingFuel -= fuelForSegment;

            // Kuro analizė
            FuelAnalysis fa = new FuelAnalysis();
            fa.setRouteSegment(savedSeg);
            fa.setPredictedFuelConsumption(round2(fuelForSegment));
            fa.setPredictedFuelRemaining(round2(Math.max(0, remainingFuel)));
            fuelAnalysisRepository.save(fa);

            current = dest;
        }

        return savedRoute;
    }

    // ── Perskaičiuoti maršrutą (su WeatherForecastAPI + NavigationAPI) ────────
    @PostMapping("/api/routes/{id}/recalculate")
    public RecalculateResponse recalculate(@PathVariable Long id) {
        Route route = routeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));

        if (route.getState() != RouteState.ACTIVE)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Galima perskaičiuoti tik aktyvų maršrutą.");

        Ship ship = route.getShip();

        // ── 1. Gauti orų duomenis (WeatherForecastAPI) ────────────────────────
        double[] shipCoords = ship.getPort() != null
                ? portToLatLon(ship.getPort())
                : new double[]{55.7, 21.1}; // Klaipėda fallback
        WeatherData weather = weatherClient.getWeatherData(shipCoords[0], shipCoords[1]);

        // ── 2. Gauti aktyvaus maršruto segmentus ──────────────────────────────
        List<RouteSegment> segments = routeSegmentRepository.findByRouteIdOrderBySequenceNumber(id);

        double remainingFuel = ship.getFuelAmount() != null ? ship.getFuelAmount() : 0.0;

        // ── 3. Kiekvienam neaplankytam segmentui: NavigationAPI + optimizacija ─
        for (RouteSegment seg : segments) {
            if (seg.getState() == RouteSegmentState.VISITED) {
                // Jau aplankytas – atskaičiuoti faktiškai sunaudotą kurą
                Optional<FuelAnalysis> existing = fuelAnalysisRepository.findByRouteSegmentId(seg.getId());
                existing.ifPresent(fa -> {});
                continue;
            }

            // Gauti NavigationAPI variantus
            double[] fromCoords = portToLatLon(seg.getStartPort());
            double[] toCoords   = portToLatLon(seg.getDestinationPort());
            NavigationApiResponse navResponse = navigationClient.getSeaRoutes(
                    fromCoords[0], fromCoords[1], toCoords[0], toCoords[1]);

            // Perskaičiuoti optimalų variantą su naujais orų duomenimis
            double fuelForSegment = optimizationService.findAndAssignBestSegmentVariant(
                    seg, navResponse, weather, ship);

            remainingFuel -= fuelForSegment;

            // Atnaujinti arba sukurti kuro analizę
            FuelAnalysis fa = fuelAnalysisRepository.findByRouteSegmentId(seg.getId())
                    .orElse(new FuelAnalysis());
            fa.setRouteSegment(seg);
            fa.setPredictedFuelConsumption(round2(fuelForSegment));
            fa.setPredictedFuelRemaining(round2(Math.max(0, remainingFuel)));
            fuelAnalysisRepository.save(fa);
        }

        // ── 4. Patikrinti ar pirmam neaplankytam segmentui pakanka kuro ────────
        RouteSegment firstUnvisited = routeSegmentRepository
                .findFirstByRouteIdAndStateOrderBySequenceNumber(id, RouteSegmentState.UNVISITED)
                .orElse(null);

        boolean sufficientFuel = false;
        if (firstUnvisited != null) {
            Optional<FuelAnalysis> fa = fuelAnalysisRepository.findByRouteSegmentId(firstUnvisited.getId());
            sufficientFuel = fa.map(f -> ship.getFuelAmount() != null
                    && ship.getFuelAmount() >= f.getPredictedFuelConsumption()).orElse(false);
        }

        return new RecalculateResponse(route, sufficientFuel,
                round2(remainingFuel), weather);
    }

    // ── Pagalbiniai metodai ───────────────────────────────────────────────────

    /**
     * Uosto koordinatės: naudoja Port.latitude/longitude jei priskirtos,
     * kitaip fallback pagal pavadinimą, kitaip Baltijos jūros centras.
     */
    private double[] portToLatLon(Port port) {
        if (port == null) return new double[]{57.5, 20.0};
        if (port.getLatitude() != null && port.getLongitude() != null
                && (port.getLatitude() != 0.0 || port.getLongitude() != 0.0)) {
            return new double[]{port.getLatitude(), port.getLongitude()};
        }
        // Fallback pagal pavadinimą (seed duomenys dar neturi koordinačių)
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

    // ── Response / Request DTO ────────────────────────────────────────────────
    public record RouteDetailResponse(Route route, List<RouteSegment> segments,
                                      List<FuelAnalysis> fuelAnalyses) {}

    public record SegmentDetailResponse(RouteSegment segment, List<Coordinate> coordinates,
                                        FuelAnalysis fuelAnalysis) {}

    public record RecalculateResponse(Route route, boolean sufficientFuel,
                                      double remainingFuelEstimate, WeatherData usedWeather) {}

    public static class GenerateRouteRequest {
        public List<Long> destinationPortIds;
        public String routeName;
    }
}
