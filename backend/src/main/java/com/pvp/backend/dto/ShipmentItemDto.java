package com.pvp.backend.dto;

public class ShipmentItemDto {
    private Long prekeId;
    private String prekePavadinimas;
    private Integer kiekis;
    private double svoris;
    private double turis;

    public ShipmentItemDto() {
    }

    public ShipmentItemDto(Long prekeId, String prekePavadinimas, Integer kiekis, double svoris, double turis) {
        this.prekeId = prekeId;
        this.prekePavadinimas = prekePavadinimas;
        this.kiekis = kiekis;
        this.svoris = svoris;
        this.turis = turis;
    }

    public Long getPrekeId() {
        return prekeId;
    }

    public void setPrekeId(Long prekeId) {
        this.prekeId = prekeId;
    }

    public String getPrekePavadinimas() {
        return prekePavadinimas;
    }

    public void setPrekePavadinimas(String prekePavadinimas) {
        this.prekePavadinimas = prekePavadinimas;
    }

    public Integer getKiekis() {
        return kiekis;
    }

    public void setKiekis(Integer kiekis) {
        this.kiekis = kiekis;
    }

    public double getSvoris() {
        return svoris;
    }

    public void setSvoris(double svoris) {
        this.svoris = svoris;
    }

    public double getTuris() {
        return turis;
    }

    public void setTuris(double turis) {
        this.turis = turis;
    }
}