package com.pvp.backend.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Atsakymas iš WeatherForecastService (išorinis API).
 * Laukai atitinka Open-Meteo / OpenWeatherMap JSON struktūrą;
 * @JsonIgnoreProperties leidžia ignoruoti nereikalingus laukus.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherData {
    /** Vidutinis vėjo greitis, m/s */
    private double windSpeed;

    /** Vėjo kryptis, laipsniais (0 = šiaurė, 90 = rytai, ...) */
    private double windDirection;

    /** Bangų aukštis, m */
    private double waveHeight;

    /** Oro temperatūra, °C */
    private double temperature;

    public WeatherData() {}

    public WeatherData(double windSpeed, double windDirection, double waveHeight, double temperature) {
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.waveHeight = waveHeight;
        this.temperature = temperature;
    }

    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }

    public double getWindDirection() { return windDirection; }
    public void setWindDirection(double windDirection) { this.windDirection = windDirection; }

    public double getWaveHeight() { return waveHeight; }
    public void setWaveHeight(double waveHeight) { this.waveHeight = waveHeight; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
}
