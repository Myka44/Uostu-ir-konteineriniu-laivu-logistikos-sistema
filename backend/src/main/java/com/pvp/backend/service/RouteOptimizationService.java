package com.pvp.backend.service;

import com.pvp.backend.client.*;
import com.pvp.backend.model.*;
import com.pvp.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementuoja "Rasti optimalų maršruto segmento variantą" seką.
 *
 * Algoritmas (pagal diagramą):
 *  1. Gauti laivą ir jo konteinerius → calculateWeightCoefficient()
 *  2. Kiekvienam segmento variantui (iš NavigationAPI):
 *     a. Kiekvienai koordinatei:
 *        - calculateDistance()    → Haversine km tarp taškų
 *        - calculateShipCourse()  → azimutas laipsniais
 *        - calculateWeatherCoefficient() → vėjo/bangų poveikis kurui
 *        - calculateFuelCost()    → kuro sąnaudos šiam žingsniui
 *        - sumFuelCost()          → kaupti bendrą kainą
 *     b. isFuelCostBetter()       → palyginti su geriausiu variantu
 *     c. assignSegmentVariantAsBest() jei geriau
 *  3. assignBestSegmentVariantToRoute() → išsaugoti koordinates į DB
 */
@Service
public class RouteOptimizationService {

    private static final Logger log = LoggerFactory.getLogger(RouteOptimizationService.class);

    /** Vidutinis konteinerio svoris tonoms – normalizavimo bazė */
    private static final double BASE_WEIGHT_TONS = 5000.0;

    private final CoordinateRepository coordinateRepository;
    private final ContainerRepository containerRepository;

    public RouteOptimizationService(CoordinateRepository coordinateRepository,
                                    ContainerRepository containerRepository) {
        this.coordinateRepository = coordinateRepository;
        this.containerRepository = containerRepository;
    }

    /**
     * Pagrindinė operacija: iš kelių kandidatų maršrutų (gauti iš NavigationAPI)
     * pasirenka geriausią pagal kuro sąnaudas ir išsaugo koordinates.
     *
     * @param segment    segmentas, kuriam ieškomas geriausias variantas
     * @param candidates maršruto variantai iš NavigationAPI
     * @param weather    orų duomenys iš WeatherForecastAPI
     * @param ship       laivas (reikia svorio ir bazinių kuro sąnaudų)
     * @return bendros prognozuojamos kuro sąnaudos geriausiam variantui
     */
    public double findAndAssignBestSegmentVariant(RouteSegment segment,
                                                  NavigationApiResponse candidates,
                                                  WeatherData weather,
                                                  Ship ship) {
        // 1. Apskaičiuoti svorio koeficientą pagal laivo ir konteinerių svorį
        double weightCoefficient = calculateWeightCoefficient(ship);

        SeaRoute bestRoute = null;
        double bestFuelCost = Double.MAX_VALUE;
        List<NavigationWaypoint> bestWaypoints = null;

        // 2. Kiekvienam variantui skaičiuoti kuro sąnaudas
        for (SeaRoute candidate : candidates.getRoutes()) {
            List<NavigationWaypoint> wps = candidate.getWaypoints();
            if (wps == null || wps.size() < 2) continue;

            double totalFuelCost = 0.0;

            for (int i = 0; i < wps.size() - 1; i++) {
                NavigationWaypoint from = wps.get(i);
                NavigationWaypoint to   = wps.get(i + 1);

                double distanceKm      = calculateDistance(from, to);
                double course          = calculateShipCourse(from, to);
                double weatherCoeff    = calculateWeatherCoefficient(weather, course);
                double stepFuelCost    = calculateFuelCost(distanceKm, ship, weightCoefficient, weatherCoeff);
                totalFuelCost          = sumFuelCost(totalFuelCost, stepFuelCost);
            }

            log.debug("Variantas {}: {} taškų, kuras={:.2f}", candidates.getRoutes().indexOf(candidate),
                    wps.size(), totalFuelCost);

            // 3. Palyginti su geriausiu
            if (isFuelCostBetter(totalFuelCost, bestFuelCost)) {
                bestFuelCost = totalFuelCost;
                bestRoute = candidate;
                bestWaypoints = wps;
            }
        }

        // 4. Priskirti geriausio varianto koordinates segmentui
        if (bestWaypoints != null) {
            assignBestSegmentVariantToRoute(segment, bestWaypoints);
            log.info("Segmentui {} priskirtas geriausias variantas: kuras={}", segment.getId(), bestFuelCost);
        } else {
            // Jei nėra variantų – fallback: tuščias segmentas
            bestFuelCost = 0.0;
            log.warn("Segmentui {} nerasta variantų.", segment.getId());
        }

        return bestFuelCost;
    }

    // ── Diagramos metodai ─────────────────────────────────────────────────────

    /**
     * calculateWeightCoefficient() — koeficientas pagal laivo ir jo krovinio svorį.
     * Diagramoje: gauti laivą + getAllShipContainers().
     * Konteinerių svoris apskaičiuojamas iš visų konteinerių (stowage logika).
     */
    public double calculateWeightCoefficient(Ship ship) {
        double shipWeight = ship.getWeight() != null ? ship.getWeight() : BASE_WEIGHT_TONS;
        // Konteinerių svoris: diagramoje getAllShipContainers() – naudojami visi
        // konteineriai, kurių warningLabel nėra null (realūs kroviniai).
        double containerWeight = containerRepository.findAll().stream()
                .filter(c -> c.getWeight() != null && c.getWeight() > 0)
                .mapToDouble(c -> c.getWeight())
                .sum();
        double totalWeight = shipWeight + containerWeight;
        log.debug("weightCoeff: laivas={}t, konteineriai={}t, koef={}",
                shipWeight, containerWeight, totalWeight / BASE_WEIGHT_TONS);
        return totalWeight / BASE_WEIGHT_TONS;
    }

    /**
     * calculateDistance() — Haversine formulė, grąžina atstumo km tarp dviejų taškų.
     */
    public double calculateDistance(NavigationWaypoint from, NavigationWaypoint to) {
        final double R = 6371.0; // Žemės spindulys km
        double dLat = Math.toRadians(to.getLat() - from.getLat());
        double dLon = Math.toRadians(to.getLon() - from.getLon());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(from.getLat()))
                * Math.cos(Math.toRadians(to.getLat()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * calculateShipCourse() — grąžina azimutą laipsniais (0–360) nuo from → to.
     */
    public double calculateShipCourse(NavigationWaypoint from, NavigationWaypoint to) {
        double dLon = Math.toRadians(to.getLon() - from.getLon());
        double lat1  = Math.toRadians(from.getLat());
        double lat2  = Math.toRadians(to.getLat());
        double x = Math.sin(dLon) * Math.cos(lat2);
        double y = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(x, y));
        return (bearing + 360) % 360;
    }

    /**
     * calculateWeatherCoefficient() — orų poveikio koeficientas kuro sąnaudoms.
     * Stiprus vėjas prieš eigą didina kuro naudojimą (>1.0).
     * Vėjas iš galo mažina (iki 0.85).
     * Bangų aukštis taip pat didina pasipriešinimą.
     *
     * @param weather orų duomenys
     * @param shipCourse laivo kursas laipsniais
     */
    public double calculateWeatherCoefficient(WeatherData weather, double shipCourse) {
        // Kampas tarp laivo kurso ir vėjo krypties
        double angleDiff = Math.abs(shipCourse - weather.getWindDirection()) % 360;
        if (angleDiff > 180) angleDiff = 360 - angleDiff;

        // Vėjo koeficientas: prieš eigą (180°) = maks. pasipriešinimas
        // iš galo (0°) = palengvinimas
        double windFactor;
        double ws = weather.getWindSpeed();
        if (ws <= 0) {
            windFactor = 1.0;
        } else {
            // Normalizuojamas vėjo greitis: 10 m/s = vidutinis
            double normalizedWind = Math.min(ws / 10.0, 2.5);
            double headwindComponent = Math.cos(Math.toRadians(angleDiff)); // -1 = prieš, +1 = iš galo
            windFactor = 1.0 + normalizedWind * (-headwindComponent) * 0.15;
        }

        // Bangų koeficientas: 0 m = 1.0; 3 m = ~1.20
        double waveFactor = 1.0 + Math.min(weather.getWaveHeight(), 5.0) * 0.065;

        double coeff = windFactor * waveFactor;
        // Apriboti: nuo 0.8 iki 2.0
        return Math.max(0.80, Math.min(2.0, coeff));
    }

    /**
     * calculateFuelCost() — kuro sąnaudos vienam žingsniui.
     * Formulė: distanceKm × baseFuel × weightCoeff × weatherCoeff
     */
    public double calculateFuelCost(double distanceKm, Ship ship,
                                    double weightCoefficient, double weatherCoefficient) {
        double base = ship.getBaseFuelConsumption() != null ? ship.getBaseFuelConsumption() : 0.1;
        return distanceKm * base * weightCoefficient * weatherCoefficient;
    }

    /**
     * sumFuelCost() — kaupiamoji suma (atskira operacija kaip diagramoje).
     */
    public double sumFuelCost(double accumulated, double step) {
        return accumulated + step;
    }

    /**
     * isFuelCostBetter() — ar naujasis variantas geresnis nei dabartinis geriausias.
     */
    public boolean isFuelCostBetter(double candidate, double currentBest) {
        return candidate < currentBest;
    }

    /**
     * assignSegmentVariantAsBest() — pažymėti variantą kaip geriausią
     * (vidinis žingsnis prieš assignBestSegmentVariantToRoute).
     */
    public void assignSegmentVariantAsBest(SeaRoute route) {
        // Logika: tiesiog žymėjimas (reali saugojimo logika vyksta assignBestSegmentVariantToRoute)
        log.debug("assignSegmentVariantAsBest: {} taškų", route.getWaypoints().size());
    }

    /**
     * assignBestSegmentVariantToRoute() — išsaugoti geriausio varianto
     * koordinates į DB (pakeičia esamas koordinates šiam segmentui).
     */
    public void assignBestSegmentVariantToRoute(RouteSegment segment,
                                                List<NavigationWaypoint> waypoints) {
        // Ištrinti senas koordinates
        coordinateRepository.deleteByRouteSegmentId(segment.getId());

        // Išsaugoti naujas
        List<Coordinate> toSave = new ArrayList<>();
        for (int i = 0; i < waypoints.size(); i++) {
            NavigationWaypoint wp = waypoints.get(i);
            Coordinate c = new Coordinate();
            c.setRouteSegment(segment);
            c.setSequenceNumber(i + 1);
            c.setLatitude(wp.getLat());
            c.setLongitude(wp.getLon());

            // Apskaičiuoti azimutą (kursą) tarp gretimų taškų
            if (i < waypoints.size() - 1) {
                c.setAzimuth(calculateShipCourse(wp, waypoints.get(i + 1)));
            } else {
                c.setAzimuth(0.0); // paskutinis taškas
            }
            toSave.add(c);
        }
        coordinateRepository.saveAll(toSave);
    }

    // ── Pagalbinis metodas koordinatėms kai uostas turi lat/lon ──────────────

    public double calculateDistanceFromLatLon(double lat1, double lon1,
                                              double lat2, double lon2) {
        return calculateDistance(
                new NavigationWaypoint(lat1, lon1),
                new NavigationWaypoint(lat2, lon2)
        );
    }
}
