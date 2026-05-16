package com.pvp.backend.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * «boundary» WeatherForecastAPI — tarpinis sluoksnis tarp RouteController
 * ir išorinio orų prognozių serviso.
 *
 * Konfigūruojama per application.properties:
 *   weather.api.url=https://api.open-meteo.com/v1/forecast
 *
 * Jei išorinis servisas nepasiekiamas, grąžinami fallback duomenys,
 * kad sistemos veikla nesustotų.
 */
@Component
public class WeatherForecastApiClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherForecastApiClient.class);

    private final RestTemplate restTemplate;

    @Value("${weather.api.url:https://api.open-meteo.com/v1/forecast}")
    private String baseUrl;

    @Value("${weather.marine.api.url:https://marine-api.open-meteo.com/v1/marine}")
    private String marineBaseUrl;

    public WeatherForecastApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Fetches weather data for given coordinates.
     * Uses Open-Meteo free API (no key required).
     * Wave height is fetched from the Marine API endpoint.
     *
     * @param lat latitude
     * @param lon longitude
     * @return weather data or fallback if API is unreachable
     */
    public WeatherData getWeatherData(double lat, double lon) {
        try {
            String url = UriComponentsBuilder.fromUriString(baseUrl)
                    .queryParam("latitude", lat)
                    .queryParam("longitude", lon)
                    .queryParam("current", "wind_speed_10m,wind_direction_10m,temperature_2m")
                    .build()
                    .toUriString();

            log.info("Requesting WeatherForecastService: {}", url);
            OpenMeteoResponse raw = restTemplate.getForObject(url, OpenMeteoResponse.class);

            if (raw != null && raw.getCurrent() != null) {
                OpenMeteoResponse.Current c = raw.getCurrent();
                double waveHeight = fetchWaveHeight(lat, lon);
                WeatherData wd = new WeatherData(
                        c.getWindSpeed10m(),
                        c.getWindDirection10m(),
                        waveHeight,
                        c.getTemperature2m()
                );
                log.info("Weather data received: wind={}m/s, dir={}deg, waves={}m",
                        wd.getWindSpeed(), wd.getWindDirection(), wd.getWaveHeight());
                return wd;
            }
        } catch (Exception e) {
            log.warn("WeatherForecastService unreachable ({}). Using fallback data.", e.getMessage());
        }
        return fallback();
    }

    private double fetchWaveHeight(double lat, double lon) {
        try {
            String url = UriComponentsBuilder.fromUriString(marineBaseUrl)
                    .queryParam("latitude", lat)
                    .queryParam("longitude", lon)
                    .queryParam("hourly", "wave_height")
                    .build()
                    .toUriString();
            OpenMeteoResponse raw = restTemplate.getForObject(url, OpenMeteoResponse.class);
            if (raw != null && raw.getHourly() != null
                    && raw.getHourly().getWaveHeight() != null
                    && !raw.getHourly().getWaveHeight().isEmpty()) {
                Double wh = raw.getHourly().getWaveHeight().get(0);
                if (wh != null) return wh;
            }
        } catch (Exception e) {
            log.debug("Marine API unreachable for wave height: {}", e.getMessage());
        }
        return 0.5; // default calm seas
    }

    /** Fallback: ramūs orai (minimalus poveikis kuro skaičiavimui). */
    private WeatherData fallback() {
        log.info("Naudojami fallback orų duomenys");
        return new WeatherData(5.0, 0.0, 0.5, 15.0);
    }

    // ── Vidiniai Open-Meteo JSON atsakymo modeliai ────────────────────────────
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class OpenMeteoResponse {
        private Current current;
        private Hourly hourly;

        public Current getCurrent() { return current; }
        public void setCurrent(Current current) { this.current = current; }
        public Hourly getHourly() { return hourly; }
        public void setHourly(Hourly hourly) { this.hourly = hourly; }

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Current {
            @com.fasterxml.jackson.annotation.JsonProperty("wind_speed_10m")
            private double windSpeed10m;
            @com.fasterxml.jackson.annotation.JsonProperty("wind_direction_10m")
            private double windDirection10m;
            @com.fasterxml.jackson.annotation.JsonProperty("temperature_2m")
            private double temperature2m;

            public double getWindSpeed10m() { return windSpeed10m; }
            public void setWindSpeed10m(double v) { this.windSpeed10m = v; }
            public double getWindDirection10m() { return windDirection10m; }
            public void setWindDirection10m(double v) { this.windDirection10m = v; }
            public double getTemperature2m() { return temperature2m; }
            public void setTemperature2m(double v) { this.temperature2m = v; }
        }

        @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
        public static class Hourly {
            @com.fasterxml.jackson.annotation.JsonProperty("wave_height")
            private java.util.List<Double> waveHeight;
            public java.util.List<Double> getWaveHeight() { return waveHeight; }
            public void setWaveHeight(java.util.List<Double> v) { this.waveHeight = v; }
        }
    }
}