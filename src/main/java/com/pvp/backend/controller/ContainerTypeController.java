package com.pvp.backend.controller;

import com.pvp.backend.dto.ContainerTypeDto;
import com.pvp.backend.model.ContainerType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/container-types")
@CrossOrigin(origins = "*")
public class ContainerTypeController {

    @GetMapping
    public List<ContainerTypeDto> getAll() {
        return Arrays.stream(ContainerType.values())
                .map(t -> new ContainerTypeDto(t.name(), t.getMaxVolume(), t.getMaxWeightKg()))
                .collect(Collectors.toList());
    }
}
