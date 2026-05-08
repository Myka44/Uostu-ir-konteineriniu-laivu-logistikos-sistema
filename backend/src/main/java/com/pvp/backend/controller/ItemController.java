package com.pvp.backend.controller;

import com.pvp.backend.model.Item;
import com.pvp.backend.repository.ItemRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@CrossOrigin(origins = "*")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public List<Item> getAll() {
        return itemRepository.findAll();
    }
}
