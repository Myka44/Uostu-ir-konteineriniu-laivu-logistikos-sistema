package com.pvp.backend.service;

import com.pvp.backend.dto.ShipmentContainerDto;
import com.pvp.backend.dto.ShipmentItemDto;
import com.pvp.backend.dto.ShipmentResultDto;
import com.pvp.backend.model.ContainerType;
import com.pvp.backend.model.HazardLabel;
import com.pvp.backend.model.Item;
import com.pvp.backend.model.ItemCategory;
import com.pvp.backend.model.Order;
import com.pvp.backend.model.OrderItem;
import com.pvp.backend.model.ShipmentContainer;
import com.pvp.backend.model.ShipmentItem;
import com.pvp.backend.model.UzsakymoBusena;
import com.pvp.backend.model.WarningLabel;
import com.pvp.backend.repository.ItemRepository;
import com.pvp.backend.repository.OrderItemRepository;
import com.pvp.backend.repository.OrderRepository;
import com.pvp.backend.repository.ShipmentContainerRepository;
import com.pvp.backend.repository.ShipmentItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ShipmentService {
    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);

    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;
    private final ShipmentContainerRepository shipmentContainerRepository;
    private final ShipmentItemRepository shipmentItemRepository;
    private final OrderRepository orderRepository;

    public ShipmentService(OrderItemRepository orderItemRepository,
                           ItemRepository itemRepository,
                           ShipmentContainerRepository shipmentContainerRepository,
                           ShipmentItemRepository shipmentItemRepository,
                           OrderRepository orderRepository) {
        this.orderItemRepository = orderItemRepository;
        this.itemRepository = itemRepository;
        this.shipmentContainerRepository = shipmentContainerRepository;
        this.shipmentItemRepository = shipmentItemRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public ShipmentResultDto formuotiKroviniuSiuntas(Long orderId) {
        AssignmentContext context = new AssignmentContext(orderId);
        context.order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        context.sourceItems = getItem(orderId);
        validateData(context.sourceItems);

        for (OrderItem orderItem : context.sourceItems) {
            if (isHazardousItem(orderItem)) {
                context.hazardousItems.add(orderItem);
            } else {
                context.regularItems.add(orderItem);
            }
        }

        Deque<PendingItem> hazardousQueue = buildPendingQueue(context.hazardousItems);
        Deque<PendingItem> regularQueue = buildPendingQueue(context.regularItems);

        assignHazardousItems(context, hazardousQueue);
        assignRegularItems(context, regularQueue);
        optimizeContainers(context);

        if (!validateAllContainers(context)) {
            throw new IllegalStateException("Cargo assignment failed validation. Manual reassignment required.");
        }

        deletePersistedShipments(orderId);
        persistContainersAndItems(context);
        update(context.order);
        orderRepository.save(context.order);

        return buildResultDto(orderId, context.containers);
    }

    private List<OrderItem> getItem(Long orderId) {
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No items found for order");
        }
        return new ArrayList<>(items);
    }

    private void validateData(List<OrderItem> items) {
        for (OrderItem orderItem : items) {
            if (orderItem.getItem() == null || orderItem.getItem().getId() == null) {
                throw new IllegalArgumentException("Invalid item reference found in order");
            }
            if (orderItem.getQuantity() == null || orderItem.getQuantity() < 1) {
                throw new IllegalArgumentException("Invalid item quantity found in order");
            }
            if (orderItem.getItem().getWeight() == null
                    || orderItem.getItem().getVolume() == null
                    || orderItem.getItem().getLength() == null
                    || orderItem.getItem().getWidth() == null
                    || orderItem.getItem().getHeight() == null) {
                throw new IllegalArgumentException("Incomplete item dimensions found in order");
            }
        }
    }

    private void assignHazardousItems(AssignmentContext context, Deque<PendingItem> pendingQueue) {
        if (pendingQueue.isEmpty()) {
            hazardousAssignmentDone();
            return;
        }

        List<PendingItem> orderedItems = new ArrayList<>(pendingQueue);
        sortByHazardType(orderedItems);
        sortBySizeDescending(orderedItems);
        pendingQueue.clear();
        pendingQueue.addAll(orderedItems);

        while (!pendingQueue.isEmpty()) {
            PendingItem pendingItem = pendingQueue.removeFirst();
            WarningLabel warningLabel = resolveWarningLabel(pendingItem.preke);
            ShipmentContainer container = getContainer(context, true, warningLabel, pendingItem.unitSvoris, pendingItem.unitTuris);
            if (container == null) {
                container = submitContainerCreate(context, true, warningLabel);
            }

            int assignedUnits = submitContainerEdit(context, container, pendingItem);
            if (assignedUnits == 0) {
                container = submitContainerCreate(context, true, warningLabel);
                assignedUnits = submitContainerEdit(context, container, pendingItem);
            }

            if (assignedUnits <= 0) {
                throw new IllegalStateException("Item cannot be assigned to any hazardous container");
            }

            if (pendingItem.remainingKiekis > 0) {
                pendingQueue.addLast(pendingItem);
            }
        }

        hazardousAssignmentDone();
    }

    private void sortByHazardType(List<PendingItem> items) {
        items.sort(Comparator.comparing(pendingItem -> pendingItem.preke.getHazardLabel().ordinal()));
    }

    private void sortBySizeDescending(List<PendingItem> items) {
        items.sort(Comparator.comparing(PendingItem::totalTuris, Comparator.reverseOrder()));
    }

    private int pickNextItem(PendingItem pendingItem, ShipmentContainer container) {
        if (pendingItem == null || container == null || pendingItem.remainingKiekis <= 0) {
            return 0;
        }

        double currentWeight = container.getCurrentWeight() == null ? 0.0 : container.getCurrentWeight();
        double currentVolume = container.getCurrentVolume() == null ? 0.0 : container.getCurrentVolume();
        double remainingWeight = (container.getMaxWeight() == null ? 0.0 : container.getMaxWeight()) - currentWeight;
        double remainingVolume = (container.getMaxVolume() == null ? 0.0 : container.getMaxVolume()) - currentVolume;
        if (remainingWeight <= 0.0 || remainingVolume <= 0.0) {
            return 0;
        }

        double byWeight = pendingItem.unitSvoris > 0.0 ? Math.floor(remainingWeight / pendingItem.unitSvoris) : 0.0;
        double byVolume = pendingItem.unitTuris > 0.0 ? Math.floor(remainingVolume / pendingItem.unitTuris) : 0.0;

        int unitsFit = (int) Math.min(
                pendingItem.remainingKiekis,
                Math.floor(Math.min(byWeight, byVolume))
        );

        return Math.max(unitsFit, 0);
    }

    private ShipmentContainer getContainer(AssignmentContext context, boolean hazardous, WarningLabel warningLabel, double weight, double volume) {
        for (ShipmentContainer container : context.containers) {
            if (container.isHazardous() != hazardous) {
                continue;
            }
            if (hazardous && !Objects.equals(container.getWarningLabel(), warningLabel)) {
                continue;
            }
            if (container.hasCapacityFor(weight, volume)) {
                return container;
            }
        }
        return null;
    }

    private ShipmentContainer getContainer(AssignmentContext context, ItemCategory category, double weight, double volume) {
        for (ShipmentContainer container : context.containers) {
            if (container.isHazardous()) {
                continue;
            }
            if (container.hasCapacityFor(weight, volume)) {
                return container;
            }
        }
        return null;
    }

    private ShipmentContainer submitContainerCreate(AssignmentContext context, boolean hazardous, WarningLabel warningLabel) {
        ShipmentContainer container = new ShipmentContainer();
        container.setOrderId(context.orderId);
        container.setType(ContainerType.STANDARD);
        container.setMaxWeight(ContainerType.STANDARD.getMaxWeightKg());
        container.setMaxVolume(ContainerType.STANDARD.getMaxVolume());
        container.setCurrentWeight(0.0);
        container.setCurrentVolume(0.0);
        container.setHazardous(hazardous);
        container.setWarningLabel(warningLabel);
        context.containers.add(container);
        context.itemsByContainer.put(container, new ArrayList<>());
        return container;
    }

    private int submitContainerEdit(AssignmentContext context, ShipmentContainer container, PendingItem pendingItem) {
        int unitsFit = pickNextItem(pendingItem, container);
        if (unitsFit < 1) {
            return 0;
        }
        assignItem(context, container, pendingItem, unitsFit);
        pendingItem.remainingKiekis -= unitsFit;
        return unitsFit;
    }

    private void assignItem(AssignmentContext context, ShipmentContainer container, PendingItem pendingItem, int unitsFit) {
        List<ShipmentItem> shipmentItems = context.itemsByContainer.computeIfAbsent(container, key -> new ArrayList<>());
        ShipmentItem shipmentItem = new ShipmentItem();
        shipmentItem.setShipmentContainerId(container.getId());
        Item item = pendingItem.preke;
        shipmentItem.setPrekeId(item.getId());
        shipmentItem.setPrekePavadinimas(item.getName());
        shipmentItem.setKiekis(unitsFit);
        shipmentItem.setSvoris(unitsFit * pendingItem.unitSvoris);
        shipmentItem.setTuris(unitsFit * pendingItem.unitTuris);
        shipmentItem.setIlgis(item.getLength());
        shipmentItem.setAukstis(item.getHeight());
        shipmentItem.setPlotis(item.getWidth());
        double centerX = container.getType().getLengthMeters() / 2.0;
        double centerY = container.getType().getWidthMeters() / 2.0;
        double centerZ = container.getType().getHeightMeters() / 2.0;
        shipmentItem.setXPosition(centerX);
        shipmentItem.setYPosition(centerY);
        shipmentItem.setZPosition(centerZ);
        shipmentItems.add(shipmentItem);

        double currentWeight = container.getCurrentWeight() == null ? 0.0 : container.getCurrentWeight();
        double currentVolume = container.getCurrentVolume() == null ? 0.0 : container.getCurrentVolume();
        container.setCurrentWeight(currentWeight + shipmentItem.getSvoris());
        container.setCurrentVolume(currentVolume + shipmentItem.getTuris());
    }

    private boolean hazardousAssignmentDone() {
        return true;
    }

    private void assignRegularItems(AssignmentContext context, Deque<PendingItem> pendingQueue) {
        if (pendingQueue.isEmpty()) {
            regularAssignmentDone();
            return;
        }

        List<PendingItem> orderedItems = new ArrayList<>(pendingQueue);
        sortByCategory(orderedItems);
        sortBySizeDescending(orderedItems);
        pendingQueue.clear();
        pendingQueue.addAll(orderedItems);

        while (!pendingQueue.isEmpty()) {
            PendingItem pendingItem = pendingQueue.removeFirst();   // This instead of pickNextItem(), since its a built in function of the queue
            ShipmentContainer container = getContainer(context, pendingItem.preke.getCategory(), pendingItem.unitSvoris, pendingItem.unitTuris);
            boolean fits3d = container != null && pendingItem.preke.getLength() <= container.getType().getLengthMeters()
                    && pendingItem.preke.getHeight() <= container.getType().getHeightMeters()
                    && pendingItem.preke.getWidth() <= container.getType().getWidthMeters();

            if (container == null || !fits3d) {
                container = submitContainerCreate(context, false, null);
            }

            int assignedUnits = submitContainerEdit(context, container, pendingItem);
            if (assignedUnits == 0) {
                container = submitContainerCreate(context, false, null);
                assignedUnits = submitContainerEdit(context, container, pendingItem);
            }

            if (assignedUnits <= 0) {
                throw new IllegalStateException("Item cannot be assigned to any regular container");
            }

            if (pendingItem.remainingKiekis > 0) {
                pendingQueue.addLast(pendingItem);
            }
        }

        regularAssignmentDone();
    }

    private void sortByCategory(List<PendingItem> items) {
        items.sort(Comparator.comparing(pendingItem -> pendingItem.preke.getCategory().ordinal()));
    }

    private boolean regularAssignmentDone() {
        return true;
    }

    private void optimizeContainers(AssignmentContext context) {
        List<ShipmentContainer> containers = getContainer(context);
        sortByOccupiedVolume(containers);

        while (!containers.isEmpty()) {
            ShipmentContainer leastFilled = containers.get(0);
            if (leastFilled.getOccupiedVolumePercent() >= 50.0) {
                break;
            }
            if (!tryMoveItemsToFuller(context, leastFilled, containers)) {
                break;
            }
            sortByOccupiedVolume(containers);
        }

        optimizationDone();
    }

    private List<ShipmentContainer> getContainer(AssignmentContext context) {
        return context.containers;
    }

    private void sortByOccupiedVolume(List<ShipmentContainer> containers) {
        containers.sort(Comparator.comparingDouble(ShipmentContainer::getOccupiedVolumePercent));
    }

    private boolean tryMoveItemsToFuller(AssignmentContext context, ShipmentContainer sourceContainer, List<ShipmentContainer> sortedContainers) {
        List<ShipmentItem> sourceItems = context.itemsByContainer.getOrDefault(sourceContainer, new ArrayList<>());
        if (sourceItems.isEmpty()) {
            return false;
        }

        List<ShipmentContainer> targets = new ArrayList<>(sortedContainers);
        targets.sort(Comparator.comparingDouble(ShipmentContainer::getOccupiedVolumePercent).reversed());

        for (ShipmentItem shipmentItem : new ArrayList<>(sourceItems)) {
            if (tryNextContainer(context, sourceContainer, shipmentItem, targets)) {
                return true;
            }
        }

        return false;
    }

    private boolean tryNextContainer(AssignmentContext context, ShipmentContainer sourceContainer, ShipmentItem shipmentItem, List<ShipmentContainer> targets) {
        for (ShipmentContainer target : targets) {
            if (target == sourceContainer) {
                continue;
            }
            if (sourceContainer.isHazardous() != target.isHazardous()) {
                continue;
            }
            if (sourceContainer.isHazardous() && !Objects.equals(sourceContainer.getWarningLabel(), target.getWarningLabel())) {
                continue;
            }
            if (target.hasCapacityFor(shipmentItem.getSvoris(), shipmentItem.getTuris()) && moveItem(context, sourceContainer, target, shipmentItem)) {
                return true;
            }
        }
        return false;
    }

    private boolean moveItem(AssignmentContext context, ShipmentContainer sourceContainer, ShipmentContainer targetContainer, ShipmentItem shipmentItem) {
        List<ShipmentItem> sourceItems = context.itemsByContainer.get(sourceContainer);
        if (sourceItems == null || !sourceItems.remove(shipmentItem)) {
            return false;
        }

        List<ShipmentItem> targetItems = context.itemsByContainer.computeIfAbsent(targetContainer, key -> new ArrayList<>());
        double sourceWeight = sourceContainer.getCurrentWeight() == null ? 0.0 : sourceContainer.getCurrentWeight();
        double sourceVolume = sourceContainer.getCurrentVolume() == null ? 0.0 : sourceContainer.getCurrentVolume();
        double targetWeight = targetContainer.getCurrentWeight() == null ? 0.0 : targetContainer.getCurrentWeight();
        double targetVolume = targetContainer.getCurrentVolume() == null ? 0.0 : targetContainer.getCurrentVolume();

        sourceContainer.setCurrentWeight(sourceWeight - shipmentItem.getSvoris());
        sourceContainer.setCurrentVolume(sourceVolume - shipmentItem.getTuris());
        targetContainer.setCurrentWeight(targetWeight + shipmentItem.getSvoris());
        targetContainer.setCurrentVolume(targetVolume + shipmentItem.getTuris());
        shipmentItem.setShipmentContainerId(targetContainer.getId());
        targetItems.add(shipmentItem);
        return true;
    }

    private boolean optimizationDone() {
        return true;
    }

    private boolean validateAllContainers(AssignmentContext context) {
        validateConstraints(context.containers);
        boolean massCenterValid = calculateMassCenterAndWeight(context);
        if (massCenterValid) {
            update(context.order);
            return true;
        }

        deleteContainer(context);
        reassignItems();
        return false;
    }

    private void validateConstraints(List<ShipmentContainer> containers) {
        for (ShipmentContainer container : containers) {
            double currentWeight = container.getCurrentWeight() == null ? 0.0 : container.getCurrentWeight();
            double currentVolume = container.getCurrentVolume() == null ? 0.0 : container.getCurrentVolume();
            if (currentWeight > container.getMaxWeight() + 0.000001) {
                throw new IllegalStateException("Container weight exceeds allowed maximum");
            }
            if (currentVolume > container.getMaxVolume() + 0.000001) {
                throw new IllegalStateException("Container volume exceeds allowed maximum");
            }
        }
    }

    private boolean calculateMassCenterAndWeight(AssignmentContext context) {
        for (ShipmentContainer container : context.containers) {
            List<ShipmentItem> items = context.itemsByContainer.getOrDefault(container, new ArrayList<>());
            if (items.isEmpty()) {
                continue;
            }

            double totalWeight = 0.0;
            double xWeightedSum = 0.0;
            double yWeightedSum = 0.0;
            double zWeightedSum = 0.0;

            for (ShipmentItem item : items) {
                double weight = item.getSvoris() == null ? 0.0 : item.getSvoris();
                totalWeight += weight;
                xWeightedSum += (item.getXPosition() == null ? 0.0 : item.getXPosition()) * weight;
                yWeightedSum += (item.getYPosition() == null ? 0.0 : item.getYPosition()) * weight;
                zWeightedSum += (item.getZPosition() == null ? 0.0 : item.getZPosition()) * weight;
            }

            if (totalWeight <= 0.0) {
                return false;
            }

            double centerX = xWeightedSum / totalWeight;
            double centerY = yWeightedSum / totalWeight;
            double centerZ = zWeightedSum / totalWeight;
            double geometricCenterX = container.getType().getLengthMeters() / 2.0;
            double geometricCenterY = container.getType().getWidthMeters() / 2.0;
            double geometricCenterZ = container.getType().getHeightMeters() / 2.0;

            if (!isWithinTolerance(centerX, geometricCenterX)
                    || !isWithinTolerance(centerY, geometricCenterY)
                    || !isWithinTolerance(centerZ, geometricCenterZ)) {
                return false;
            }
        }

        return true;
    }

    private boolean isWithinTolerance(double center, double geometricCenter) {
        double tolerance = geometricCenter * 0.10;
        return Math.abs(center - geometricCenter) <= tolerance + 0.000001;
    }

    private Order update(Order order) {
        order.setBusena(UzsakymoBusena.VYKDOMA);
        return order;
    }

    private void deleteContainer(AssignmentContext context) {
        context.containers.clear();
        context.itemsByContainer.clear();
    }

    private void reassignItems() {
        log.warn("Cargo assignment validation failed, reassignment needed");
        throw new IllegalStateException("Cargo assignment failed validation. Manual reassignment required.");
    }

    private void deletePersistedShipments(Long orderId) {
        List<ShipmentContainer> existingContainers = shipmentContainerRepository.findByOrderId(orderId);
        for (ShipmentContainer container : existingContainers) {
            shipmentItemRepository.deleteByShipmentContainerId(container.getId());
        }
        shipmentContainerRepository.deleteByOrderId(orderId);
    }

    private void persistContainersAndItems(AssignmentContext context) {
        for (ShipmentContainer container : context.containers) {
            ShipmentContainer savedContainer = shipmentContainerRepository.save(container);
            List<ShipmentItem> items = context.itemsByContainer.getOrDefault(container, new ArrayList<>());
            for (ShipmentItem item : items) {
                item.setShipmentContainerId(savedContainer.getId());
                shipmentItemRepository.save(item);
            }
        }
    }

    private ShipmentResultDto buildResultDto(Long orderId, List<ShipmentContainer> containers) {
        ShipmentResultDto result = new ShipmentResultDto();
        result.setOrderId(orderId);
        List<ShipmentContainerDto> containerDtos = new ArrayList<>();
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
            for (ShipmentItem item : shipmentItemRepository.findByShipmentContainerId(container.getId())) {
                itemDtos.add(new ShipmentItemDto(
                        item.getPrekeId(),
                        item.getPrekePavadinimas(),
                        item.getKiekis(),
                        item.getSvoris(),
                        item.getTuris()
                ));
            }
            containerDto.setItems(itemDtos);
            containerDtos.add(containerDto);
        }
        result.setContainers(containerDtos);
        return result;
    }

    private boolean isHazardousItem(OrderItem orderItem) {
        HazardLabel hazardLabel = orderItem.getItem().getHazardLabel();
        return hazardLabel != null && hazardLabel != HazardLabel.NONE;
    }

    private Deque<PendingItem> buildPendingQueue(List<OrderItem> sourceItems) {
        Deque<PendingItem> queue = new ArrayDeque<>();
        for (OrderItem orderItem : sourceItems) {
            Item item = orderItem.getItem();
            queue.addLast(new PendingItem(
                    item,
                    orderItem.getQuantity(),
                    item.getWeight(),
                    item.getVolume()
            ));
        }
        return queue;
    }

    private WarningLabel resolveWarningLabel(Item item) {
        if (item == null) {
            return null;
        }

        if (item.getCategory() == ItemCategory.CHEMICALS || item.getHazardLabel() == HazardLabel.HAZARDOUS) {
            return WarningLabel.CHEMICALS;
        }

        HazardLabel hazardLabel = item.getHazardLabel();
        if (hazardLabel == null || hazardLabel == HazardLabel.NONE || hazardLabel == HazardLabel.HAZARDOUS) {
            return null;
        }
        try {
            return WarningLabel.valueOf(hazardLabel.name());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static class PendingItem {
        private final Item preke;
        private int remainingKiekis;
        private final double unitSvoris;
        private final double unitTuris;

        private PendingItem(Item preke, int remainingKiekis, double unitSvoris, double unitTuris) {
            this.preke = preke;
            this.remainingKiekis = remainingKiekis;
            this.unitSvoris = unitSvoris;
            this.unitTuris = unitTuris;
        }

        private double totalTuris() {
            return unitTuris * remainingKiekis;
        }
    }

    private static class AssignmentContext {
        private final Long orderId;
        private Order order;
        private List<OrderItem> sourceItems = new ArrayList<>();
        private final List<OrderItem> hazardousItems = new ArrayList<>();
        private final List<OrderItem> regularItems = new ArrayList<>();
        private final List<ShipmentContainer> containers = new ArrayList<>();
        private final Map<ShipmentContainer, List<ShipmentItem>> itemsByContainer = new LinkedHashMap<>();

        private AssignmentContext(Long orderId) {
            this.orderId = orderId;
        }
    }
}