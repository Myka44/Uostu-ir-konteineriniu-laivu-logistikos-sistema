package com.pvp.backend.controller;

import com.pvp.backend.model.Container;
import com.pvp.backend.repository.ContainerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContainerController {

    @Autowired
    private ContainerRepository containerRepository;

    @PostMapping("/container")
    Container create(@RequestBody Container newContainer){
        return containerRepository.save(newContainer);
    }


}
