package com.pvp.backend.controller;

import com.pvp.backend.dto.ShipmentContainerDto;
import com.pvp.backend.dto.ShipmentItemDto;
import com.pvp.backend.dto.ShipmentResultDto;
import com.pvp.backend.model.ShipmentContainer;
import com.pvp.backend.repository.OrderRepository;
import com.pvp.backend.repository.ShipmentContainerRepository;
import com.pvp.backend.repository.ShipmentItemRepository;
import com.pvp.backend.service.ShipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/shipment")
@CrossOrigin(origins = "*")
public class ShipmentController {

    private final ShipmentService shipmentService;
    private final ShipmentContainerRepository shipmentContainerRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final OrderRepository orderRepository;

    public ShipmentController(ShipmentService shipmentService,
                              ShipmentContainerRepository shipmentContainerRepository,
                              ShipmentItemRepository shipmentItemRepository,
                              OrderRepository orderRepository) {
        this.shipmentService = shipmentService;
        this.shipmentContainerRepository = shipmentContainerRepository;
        this.shipmentItemRepository = shipmentItemRepository;
        this.orderRepository = orderRepository;
    }

    @PostMapping("/assign/{orderId}")
    public ResponseEntity<ShipmentResultDto> assign(@PathVariable Long orderId) {
        try {
            return ResponseEntity.ok(shipmentService.formuotiKroviniuSiuntas(orderId));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ShipmentResultDto(orderId, new ArrayList<>()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ShipmentResultDto(orderId, new ArrayList<>()));
        }
    }

    @GetMapping("/result/{orderId}")
    public ResponseEntity<ShipmentResultDto> getResult(@PathVariable Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            return ResponseEntity.notFound().build();
        }
        ShipmentResultDto result = new ShipmentResultDto();
        result.setOrderId(orderId);
        List<ShipmentContainerDto> containerDtos = new ArrayList<>();
        List<ShipmentContainer> containers = shipmentContainerRepository.findByOrderId(orderId);
        for (ShipmentContainer container : containers) {
            ShipmentContainerDto containerDto = new ShipmentContainerDto();
            containerDto.setContainerId(container.getId());
            containerDto.setContainerType(container.getType() == null ? null : container.getType().name());
            containerDto.setHazardous(container.isHazardous());
            containerDto.setWarningLabel(container.getWarningLabel() == null ? null : container.getWarningLabel().name());
            containerDto.setCurrentWeight(container.getCurrentWeight() == null ? 0.0 : container.getCurrentWeight());
            containerDto.setMaxWeight(container.getMaxWeight() == null ? 0.0 : container.getMaxWeight());
            containerDto.setCurrentVolume(container.getCurrentVolume() == null ? 0.0 : container.getCurrentVolume());
            containerDto.setMaxVolume(container.getMaxVolume() == null ? 0.0 : container.getMaxVolume());
            containerDto.setOccupiedVolumePercent(container.getOccupiedVolumePercent());
            List<ShipmentItemDto> itemDtos = new ArrayList<>();
            shipmentItemRepository.findByShipmentContainerId(container.getId()).forEach(item ->
                    itemDtos.add(new ShipmentItemDto(
                            item.getPrekeId(),
                            item.getPrekePavadinimas(),
                            item.getKiekis(),
                            item.getSvoris(),
                            item.getTuris()
                    ))
            );
            containerDto.setItems(itemDtos);
            containerDtos.add(containerDto);
        }
        result.setContainers(containerDtos);
        return ResponseEntity.ok(result);
    }
}