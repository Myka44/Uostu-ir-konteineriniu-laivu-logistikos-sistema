package com.pvp.backend.controller;

import com.pvp.backend.model.Order;
import com.pvp.backend.model.OrderItem;
import com.pvp.backend.repository.OrderRepository;
import com.pvp.backend.repository.ItemRepository;
import com.pvp.backend.repository.OrderItemRepository;
import com.pvp.backend.service.ShipmentService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import com.pvp.backend.model.UzsakymoBusena;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final ShipmentService shipmentService;

    public OrderController(OrderRepository orderRepository,
                           OrderItemRepository orderItemRepository,
                           ItemRepository itemRepository,
                           ShipmentService shipmentService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.itemRepository = itemRepository;
        this.shipmentService = shipmentService;
    }

    @GetMapping
    public List<Order> getAll() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public Order getOne(@PathVariable Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@RequestBody OrderWithItems request) {
        if (request.atvykimoUostas == null || request.atvykimoUostas.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arrival port (atvykimoUostas) is required");
        }
        if (request.isvykimoUostas == null || request.isvykimoUostas.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departure port (isvykimoUostas) is required");
        }

        Order newOrder = new Order();
        newOrder.setAtvykimoUostas(request.atvykimoUostas);
        newOrder.setIsvykimoUostas(request.isvykimoUostas);
        newOrder.setClientId(request.clientId);

        if (newOrder.getSukurimoData() == null) {
            newOrder.setSukurimoData(LocalDate.now());
        }

        // always set to LAUKIAMA on create
        newOrder.setBusena(UzsakymoBusena.LAUKIAMA);
        newOrder.setId(null);

        Order savedOrder = orderRepository.save(newOrder);

        if (request.items == null || request.items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one order item is required");
        }
        for (OrderItemRequest it : request.items) {
            var item = itemRepository.findById(it.itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

            OrderItem op = new OrderItem();
            op.setOrder(savedOrder);
            op.setItem(item);
            op.setQuantity(it.quantity);
            op.setTotalWeight(item.getWeight() * it.quantity);
            op.setTotalVolume(item.getVolume() * it.quantity);
            orderItemRepository.save(op);
        }

        formuotiKroviniuSiuntas(savedOrder.getId());

        return savedOrder;
    }

    public static class OrderItemRequest {
        public Long itemId;
        public Integer quantity;

        public Long getItemId() { return itemId; }
        public Integer getQuantity() { return quantity; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public static class OrderWithItems {
        public String atvykimoUostas;
        public String isvykimoUostas;
        public Long clientId;
        public java.util.List<OrderItemRequest> items;

        public String getAtvykimoUostas() { return atvykimoUostas; }
        public String getIsvykimoUostas() { return isvykimoUostas; }
        public Long getClientId() { return clientId; }
        public java.util.List<OrderItemRequest> getItems() { return items; }
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable Long id, @Valid @RequestBody OrderWithItems updatedRequest) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (existing.getBusena() != UzsakymoBusena.LAUKIAMA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only orders with status LAUKIAMA can be edited");
        }

        if (updatedRequest.atvykimoUostas == null || updatedRequest.atvykimoUostas.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arrival port (atvykimoUostas) is required");
        }

        if (updatedRequest.isvykimoUostas == null || updatedRequest.isvykimoUostas.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Departure port (isvykimoUostas) is required");
        }

        // only update ports
        existing.setAtvykimoUostas(updatedRequest.atvykimoUostas);
        existing.setIsvykimoUostas(updatedRequest.isvykimoUostas);

        Order saved = orderRepository.save(existing);

        // replace order items
        orderItemRepository.deleteByOrderId(id);

        if (updatedRequest.items == null || updatedRequest.items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one order item is required");
        }

        for (OrderItemRequest it : updatedRequest.items) {
            var item = itemRepository.findById(it.itemId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

            OrderItem op = new OrderItem();
            op.setOrder(saved);
            op.setItem(item);
            op.setQuantity(it.quantity);
            op.setTotalWeight(item.getWeight() * it.quantity);
            op.setTotalVolume(item.getVolume() * it.quantity);
            orderItemRepository.save(op);
        }

        formuotiKroviniuSiuntas(saved.getId());

        return saved;
    }

    private void formuotiKroviniuSiuntas(Long orderId) {
        try {
            shipmentService.formuotiKroviniuSiuntas(orderId);
        } catch (Exception ex) {
            log.error("Cargo assignment failed for order {}", orderId, ex);
        }
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (existing.getBusena() != UzsakymoBusena.LAUKIAMA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only orders with status LAUKIAMA can be deleted");
        }

        existing.setBusena(UzsakymoBusena.ATSAUKTA);
        orderRepository.save(existing);
    }
}
