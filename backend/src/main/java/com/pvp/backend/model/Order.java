package com.pvp.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Entity
@Table(name = "uzsakymai")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDate sukurimoData;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UzsakymoBusena busena;

    @NotNull
    private String atvykimoUostas;

    @NotNull
    private String isvykimoUostas;

    @NotNull
    private Long clientId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSukurimoData() {
        return sukurimoData;
    }

    public void setSukurimoData(LocalDate sukurimoData) {
        this.sukurimoData = sukurimoData;
    }

    public UzsakymoBusena getBusena() {
        return busena;
    }

    public void setBusena(UzsakymoBusena busena) {
        this.busena = busena;
    }

    public String getAtvykimoUostas() {
        return atvykimoUostas;
    }

    public void setAtvykimoUostas(String atvykimoUostas) {
        this.atvykimoUostas = atvykimoUostas;
    }

    public String getIsvykimoUostas() {
        return isvykimoUostas;
    }

    public void setIsvykimoUostas(String isvykimoUostas) {
        this.isvykimoUostas = isvykimoUostas;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}
