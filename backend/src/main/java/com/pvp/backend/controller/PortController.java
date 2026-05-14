package com.pvp.backend.controller;

import com.pvp.backend.model.Port;
import com.pvp.backend.repository.PortRepository;
import com.pvp.backend.repository.ShipRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/ports")
@CrossOrigin(origins = "*")
public class PortController {

    private final PortRepository portRepository;
    private final ShipRepository shipRepository;

    public PortController(PortRepository portRepository, ShipRepository shipRepository) {
        this.portRepository = portRepository;
        this.shipRepository = shipRepository;
    }

    @GetMapping
    public List<Port> getAll() {
        return portRepository.findAll();
    }

    @GetMapping("/{id}")
    public Port getOne(@PathVariable Long id) {
        return portRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Port not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Port create(@Valid @RequestBody Port port) {
        port.setId(null);
        return portRepository.save(port);
    }

    @PutMapping("/{id}")
    public Port update(@PathVariable Long id, @Valid @RequestBody Port updated) {
        Port existing = portRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Port not found"));
        existing.setName(updated.getName());
        existing.setCountry(updated.getCountry());
        existing.setDockCount(updated.getDockCount());
        existing.setContainerCapacity(updated.getContainerCapacity());
        existing.setOpen(updated.getOpen());
        existing.setLength(updated.getLength());
        existing.setWidth(updated.getWidth());
        existing.setHeight(updated.getHeight());
        return portRepository.save(existing);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!portRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Port not found");
        }
        portRepository.deleteById(id);
    }

    /** Returns how many ships are currently docked at this port */
    @GetMapping("/{id}/ship-count")
    public long getDockedShipCount(@PathVariable Long id) {
        if (!portRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Port not found");
        }
        return shipRepository.findAll().stream()
                .filter(s -> s.getPort() != null && s.getPort().getId().equals(id))
                .count();
    }
}
