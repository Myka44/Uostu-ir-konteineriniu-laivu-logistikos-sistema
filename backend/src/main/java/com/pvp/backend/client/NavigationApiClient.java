package com.pvp.backend.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * «boundary» NavigationAPI
 *
 * Naudoja searoute.com API — specializuotą nemokamą jūrų maršrutų servisą,
 * kuris išvengia sausumos ir tinka Baltijos jūros uostams.
 *
 * Kadangi SeaRoute API grąžina VIENĄ maršrutą, kelių variantų generavimui
 * taikoma strategija: pirmasis variantas gaunamas iš API (tiesioginis jūrų
 * kelias), o papildomi variantai generuojami perpendikulariais poslinkiais
 * (±offset) nuo tiesios linijos tarp uostų. Tai atspindi realią navigaciją,
 * kai laivas gali rinktis truputį skirtingus kursus (vengti perkrauto koridor-
 * iaus, pasinaudoti srovėmis ir pan.).
 *
 * Konfigūracija application.properties:
 *   navigation.api.url=https://searoute.com/api/route
 *   navigation.variants.count=3
 *   navigation.variants.offset.deg=0.5
 */
@Component
public class NavigationApiClient {

    private static final Logger log = LoggerFactory.getLogger(NavigationApiClient.class);

    private final RestTemplate restTemplate;

    @Value("${navigation.api.url:https://searoute.com/api/route}")
    private String baseUrl;

    /** Kiek variantų generuoti (1 = API + 0 papildomų) */
    @Value("${navigation.variants.count:3}")
    private int variantCount;

    /** Perpendikularinis poslinkis laipsniais generuojant variantus */
    @Value("${navigation.variants.offset.deg:0.5}")
    private double offsetDeg;

    public NavigationApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Gauna kelis jūrų maršruto variantus tarp dviejų taškų.
     *
     * Pirmas variantas: realus jūrų kelias iš SeaRoute API.
     * Kiti variantai: generuojami perpendikulariais poslinkiais.
     *
     * Visada grąžina bent {@code variantCount} variantų (fallback garantuoja tai).
     */
    public NavigationApiResponse getSeaRoutes(double fromLat, double fromLon,
                                              double toLat,   double toLon) {
        List<SeaRoute> allVariants = new ArrayList<>();

        // 1. Gauti pagrindinį maršrutą iš SeaRoute API
        SeaRoute primary = fetchFromSeaRouteApi(fromLat, fromLon, toLat, toLon);
        allVariants.add(primary);

        // 2. Sugeneruoti papildomus variantus perpendikulariais poslinkiais
        // Kiekvienas variantas - šiek tiek pakrypęs, skirtingas kuro ir oro poveikis
        for (int v = 1; v < variantCount; v++) {
            // Kintamas poslinkis: +offset, -offset, +2*offset, ...
            double sign = (v % 2 == 1) ? 1.0 : -1.0;
            double magnitude = Math.ceil(v / 2.0) * offsetDeg;
            allVariants.add(generateOffsetVariant(primary, sign * magnitude));
        }

        log.info("getSeaRoutes: grąžinama {} variantų ({} -> {})",
                allVariants.size(), coords(fromLat, fromLon), coords(toLat, toLon));
        return new NavigationApiResponse(allVariants);
    }

    // ── SeaRoute API kreipinys ────────────────────────────────────────────────

    /**
     * Kviečia https://searoute.com/api/route
     * Atsakymas: GeoJSON LineString su jūros keliu.
     * Jei nepasiekiamas – grąžina fallback (tiesus maršrutas).
     */
    private SeaRoute fetchFromSeaRouteApi(double fromLat, double fromLon,
                                          double toLat,   double toLon) {
        try {
            // SeaRoute API priima: ?sx=lon&sy=lat&ex=lon&ey=lat
            String url = String.format(
                    "%s?sx=%.6f&sy=%.6f&ex=%.6f&ey=%.6f",
                    baseUrl, fromLon, fromLat, toLon, toLat);

            log.info("Kreipiamasi į SeaRoute API: {}", url);
            SeaRouteApiResponse raw = restTemplate.getForObject(url, SeaRouteApiResponse.class);

            if (raw != null && raw.getGeometry() != null
                    && raw.getGeometry().getCoordinates() != null
                    && !raw.getGeometry().getCoordinates().isEmpty()) {

                List<NavigationWaypoint> wps = raw.getGeometry().getCoordinates().stream()
                        .map(coord -> new NavigationWaypoint(coord.get(1), coord.get(0))) // [lon,lat] → lat/lon
                        .toList();

                log.info("SeaRoute API: gautas maršrutas su {} taškais", wps.size());
                return new SeaRoute(wps);
            }
            log.warn("SeaRoute API grąžino tuščią atsakymą. Naudojamas fallback.");
        } catch (org.springframework.web.client.ResourceAccessException e) {
            // Covers: SocketTimeoutException, "Unexpected end of file", connection refused
            log.warn("SeaRoute API nepasiekiamas (I/O klaida): {}. Naudojamas fallback.", e.getMessage());
        } catch (Exception e) {
            log.warn("SeaRoute API klaida: {}. Naudojamas fallback.", e.getMessage());
        }

        return straightLineFallback(fromLat, fromLon, toLat, toLon, 8);
    }

    // ── Variantų generavimas ──────────────────────────────────────────────────

    /**
     * Generuoja naują variantą iš esamo maršruto, perkeliant vidurinius taškus
     * statmenai pradiniam kursui.
     *
     * Taip imituojamas laivo sprendimas plaukti šiek tiek kitu kursu —
     * pvz., vengiant perkrauto laivybos koridoriaus arba pasinaudojant esamomis srovėmis.
     *
     * @param base      bazinis maršrutas (paprastai iš API)
     * @param offsetDeg poslinkis laipsniais statmenai kursui (+ arba -)
     */
    private SeaRoute generateOffsetVariant(SeaRoute base, double offsetDeg) {
        List<NavigationWaypoint> original = base.getWaypoints();
        if (original == null || original.size() < 2) {
            return base;
        }

        // Apskaičiuoti bendrą kursą (nuo pradžios iki pabaigos)
        NavigationWaypoint first = original.get(0);
        NavigationWaypoint last  = original.get(original.size() - 1);
        double courseDeg = bearingDeg(first, last);

        // Statmens kryptis: kursas + 90°
        double perpDeg = (courseDeg + 90.0) % 360.0;
        double dLat = offsetDeg * Math.cos(Math.toRadians(perpDeg));
        double dLon = offsetDeg * Math.sin(Math.toRadians(perpDeg));

        List<NavigationWaypoint> shifted = new ArrayList<>();
        shifted.add(original.get(0)); // pradžia nesikeičia

        // Viduriniai taškai perkeliami
        for (int i = 1; i < original.size() - 1; i++) {
            NavigationWaypoint wp = original.get(i);
            // Gaussinis svoris: didžiausias poslinkis viduryje, nyksta kraštų link
            double frac   = (double) i / (original.size() - 1);
            double weight = Math.sin(Math.PI * frac); // 0 → 1 → 0
            shifted.add(new NavigationWaypoint(
                    wp.getLat() + dLat * weight,
                    wp.getLon() + dLon * weight));
        }

        shifted.add(original.get(original.size() - 1)); // pabaiga nesikeičia
        return new SeaRoute(shifted);
    }

    /**
     * Paprastas tiesus maršrutas su {@code steps} tarpiniais taškais.
     * Naudojamas kai SeaRoute API nepasiekiamas.
     */
    private SeaRoute straightLineFallback(double fromLat, double fromLon,
                                          double toLat,   double toLon, int steps) {
        List<NavigationWaypoint> wps = new ArrayList<>();
        for (int i = 0; i <= steps; i++) {
            double frac = (double) i / steps;
            wps.add(new NavigationWaypoint(
                    fromLat + frac * (toLat - fromLat),
                    fromLon + frac * (toLon - fromLon)));
        }
        log.info("Fallback: tiesus maršrutas su {} taškais", wps.size());
        return new SeaRoute(wps);
    }

    // ── Geometriniai pagalbiniai metodai ─────────────────────────────────────

    /** Azimutas laipsniais tarp dviejų taškų. */
    private double bearingDeg(NavigationWaypoint from, NavigationWaypoint to) {
        double dLon = Math.toRadians(to.getLon() - from.getLon());
        double lat1 = Math.toRadians(from.getLat());
        double lat2 = Math.toRadians(to.getLat());
        double x = Math.sin(dLon) * Math.cos(lat2);
        double y = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(x, y)) + 360) % 360;
    }

    private String coords(double lat, double lon) {
        return String.format("(%.4f, %.4f)", lat, lon);
    }

    // ── SeaRoute API GeoJSON atsakymo modelis ─────────────────────────────────

    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class SeaRouteApiResponse {
        private Geometry geometry;

        public Geometry getGeometry() { return geometry; }
        public void setGeometry(Geometry geometry) { this.geometry = geometry; }

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Geometry {
            /** [[lon, lat], [lon, lat], ...] */
            private java.util.List<java.util.List<Double>> coordinates;

            public java.util.List<java.util.List<Double>> getCoordinates() { return coordinates; }
            public void setCoordinates(java.util.List<java.util.List<Double>> c) { this.coordinates = c; }
        }
    }
}
