package com.pvp.backend.controller;

import com.pvp.backend.model.Container;
import com.pvp.backend.repository.ContainerRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/containers")
@CrossOrigin(origins = "*")
public class ContainerController {

    private final ContainerRepository containerRepository;

    public ContainerController(ContainerRepository containerRepository) {
        this.containerRepository = containerRepository;
    }

    @GetMapping
    public List<Container> getAll() {
        return containerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Container getOne(@PathVariable Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Container not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Container create(@Valid @RequestBody Container newContainer) {
        validateBusinessRules(newContainer);
        newContainer.setId(null);
        return containerRepository.save(newContainer);
    }

    @PutMapping("/{id}")
    public Container update(@PathVariable Long id, @Valid @RequestBody Container updatedContainer) {
        validateBusinessRules(updatedContainer);

        return containerRepository.findById(id)
                .map(container -> {
                    container.setType(updatedContainer.getType());
                    container.setWeight(updatedContainer.getWeight());
                    container.setVolume(updatedContainer.getVolume());
                    container.setMaxWeight(updatedContainer.getMaxWeight());
                    container.setMaxVolume(updatedContainer.getMaxVolume());
                    container.setWarningLabel(updatedContainer.getWarningLabel());
                    return containerRepository.save(container);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Container not found"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        if (!containerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Container not found");
        }
        containerRepository.deleteById(id);
    }

    private void validateBusinessRules(Container container) {
        if (container.getWeight() != null && container.getMaxWeight() != null
                && container.getWeight() > container.getMaxWeight()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Weight cannot be greater than maxWeight");
        }

        if (container.getVolume() != null && container.getMaxVolume() != null
                && container.getVolume() > container.getMaxVolume()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Volume cannot be greater than maxVolume");
        }
    }
}