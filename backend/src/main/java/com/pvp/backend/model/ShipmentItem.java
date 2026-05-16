package com.pvp.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "shipment_items")
public class ShipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long shipmentContainerId;

    @NotNull
    private Long prekeId;

    private String prekePavadinimas;

    @NotNull
    private Integer kiekis;

    @NotNull
    private Double svoris;

    @NotNull
    private Double turis;

    private Double ilgis = 0.0;

    private Double aukstis = 0.0;

    private Double plotis = 0.0;

    private Double xPosition = 0.0;

    private Double yPosition = 0.0;

    private Double zPosition = 0.0;

    public ShipmentItem() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getShipmentContainerId() {
        return shipmentContainerId;
    }

    public void setShipmentContainerId(Long shipmentContainerId) {
        this.shipmentContainerId = shipmentContainerId;
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

    public Double getSvoris() {
        return svoris;
    }

    public void setSvoris(Double svoris) {
        this.svoris = svoris;
    }

    public Double getTuris() {
        return turis;
    }

    public void setTuris(Double turis) {
        this.turis = turis;
    }

    public Double getIlgis() {
        return ilgis;
    }

    public void setIlgis(Double ilgis) {
        this.ilgis = ilgis;
    }

    public Double getAukstis() {
        return aukstis;
    }

    public void setAukstis(Double aukstis) {
        this.aukstis = aukstis;
    }

    public Double getPlotis() {
        return plotis;
    }

    public void setPlotis(Double plotis) {
        this.plotis = plotis;
    }

    public Double getXPosition() {
        return xPosition;
    }

    public void setXPosition(Double xPosition) {
        this.xPosition = xPosition;
    }

    public Double getYPosition() {
        return yPosition;
    }

    public void setYPosition(Double yPosition) {
        this.yPosition = yPosition;
    }

    public Double getZPosition() {
        return zPosition;
    }

    public void setZPosition(Double zPosition) {
        this.zPosition = zPosition;
    }
}