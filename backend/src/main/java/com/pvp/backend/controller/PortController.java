package com.pvp.backend.controller;

import com.pvp.backend.model.Port;
import com.pvp.backend.repository.PortRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ports")
@CrossOrigin(origins = "*")
public class PortController {

    private final PortRepository portRepository;

    public PortController(PortRepository portRepository) {
        this.portRepository = portRepository;
    }

    @GetMapping
    public List<Port> getAll() {
        return portRepository.findAll();
    }
}
