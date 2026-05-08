package com.pvp.backend.controller;

import com.pvp.backend.model.Order;
import com.pvp.backend.model.Item;
import com.pvp.backend.model.OrderItem;
import com.pvp.backend.repository.OrderRepository;
import com.pvp.backend.repository.ItemRepository;
import com.pvp.backend.repository.OrderItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/items")
@CrossOrigin(origins = "*")
public class OrderItemController {

    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;

    public OrderItemController(OrderItemRepository orderItemRepository,
                               ItemRepository itemRepository,
                               OrderRepository orderRepository) {
        this.orderItemRepository = orderItemRepository;
        this.itemRepository = itemRepository;
        this.orderRepository = orderRepository;
    }

    @GetMapping
    public List<OrderItem> getAll(@PathVariable Long orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public static class CreateRequest {
        public Long itemId;
        public Integer quantity;

        public Long getItemId() { return itemId; }
        public Integer getQuantity() { return quantity; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItem create(@PathVariable Long orderId, @RequestBody CreateRequest payload) {
        Item item = itemRepository.findById(payload.itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (payload.quantity == null || payload.quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
        }

        OrderItem itemEntity = new OrderItem();
        itemEntity.setOrder(order);
        itemEntity.setItem(item);
        itemEntity.setQuantity(payload.quantity);
        itemEntity.setTotalWeight(item.getWeight() * payload.quantity);
        itemEntity.setTotalVolume(item.getVolume() * payload.quantity);

        return orderItemRepository.save(itemEntity);
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long orderId, @PathVariable Long itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
        if (!item.getOrder().getId().equals(orderId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item does not belong to order");
        }
        orderItemRepository.deleteById(itemId);
    }
}
