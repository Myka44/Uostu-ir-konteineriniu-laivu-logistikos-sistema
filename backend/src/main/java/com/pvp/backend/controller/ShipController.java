package com.pvp.backend.controller;

import com.pvp.backend.model.Ship;
import com.pvp.backend.repository.ShipRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ships")
@CrossOrigin(origins = "*")
public class ShipController {
    private final ShipRepository shipRepository;

    public ShipController(ShipRepository shipRepository) {
        this.shipRepository = shipRepository;
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
    public Ship create(@Valid @RequestBody Ship ship) {
        ship.setId(null);
        return shipRepository.save(ship);
    }

    @PutMapping("/{id}")
    public Ship update(@PathVariable Long id, @Valid @RequestBody Ship updated) {
        Ship ship = shipRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));
        ship.setName(updated.getName());
        ship.setType(updated.getType());
        ship.setRegistrationCountry(updated.getRegistrationCountry());
        ship.setWeight(updated.getWeight());
        ship.setCapacity(updated.getCapacity());
        ship.setShipStatus(updated.getShipStatus());
        ship.setBaseFuelConsumption(updated.getBaseFuelConsumption());
        ship.setFuelAmount(updated.getFuelAmount());
        ship.setLength(updated.getLength());
        ship.setWidth(updated.getWidth());
        ship.setHeight(updated.getHeight());
        ship.setPort(updated.getPort());
        return shipRepository.save(ship);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!shipRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found");
        }
        shipRepository.deleteById(id);
    }
}
