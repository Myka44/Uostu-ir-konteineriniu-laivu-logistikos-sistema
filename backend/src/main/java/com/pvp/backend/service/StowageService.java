package com.pvp.backend.service;

import com.pvp.backend.dto.StowageCreateRequest;
import com.pvp.backend.dto.StowagePlanResponse;
import com.pvp.backend.model.*;
import com.pvp.backend.repository.ContainerRepository;
import com.pvp.backend.repository.PortRepository;
import com.pvp.backend.repository.ShipRepository;
import com.pvp.backend.repository.StowageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StowageService {
    private final StowageRepository stowageRepository;
    private final ShipRepository shipRepository;
    private final PortRepository portRepository;
    private final ContainerRepository containerRepository;

    public StowageService(
            StowageRepository stowageRepository,
            ShipRepository shipRepository,
            PortRepository portRepository,
            ContainerRepository containerRepository
    ) {
        this.stowageRepository = stowageRepository;
        this.shipRepository = shipRepository;
        this.portRepository = portRepository;
        this.containerRepository = containerRepository;
    }

    public List<Stowage> getStowagePlans() {
        return stowageRepository.findAll();
    }

    public Stowage getStowagePlan(Long id) {
        return stowageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stowage plan not found"));
    }

    public List<Ship> getLoadShips() {
        return shipRepository.findAll();
    }

    public Ship getShip(Long shipId) {
        return shipRepository.findById(shipId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ship not found"));
    }

    public Port getPort(Long portId) {
        return portRepository.findById(portId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Port not found"));
    }

    public List<Port> getPorts() {
        return portRepository.findAll();
    }

    /**
     * Sequence-diagram method name kept.
     * The important change is that stowage creation no longer pulls every container in the system.
     * Empty containerIds means: use every container assigned to the selected ship.
     * Non-empty containerIds means: use only the selected subset, but each selected container must
     * already be assigned to that ship.
     */
    public List<Container> getContainersForStowage(Long shipId, List<Long> containerIds) {
        getShip(shipId); // validates that the ship exists

        List<Container> assignedContainers = containerRepository.findByShipId(shipId);
        if (containerIds == null || containerIds.isEmpty()) {
            if (assignedContainers.isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No containers are assigned to the selected ship"
                );
            }
            return assignedContainers;
        }

        Set<Long> requestedIds = new LinkedHashSet<>(containerIds);
        List<Container> selectedContainers = assignedContainers.stream()
                .filter(container -> requestedIds.contains(container.getId()))
                .toList();

        if (selectedContainers.size() != requestedIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "All selected containers must be assigned to the selected ship"
            );
        }

        return selectedContainers;
    }

    public List<Container> getContainersForShip(Long shipId) {
        getShip(shipId);
        return containerRepository.findByShipId(shipId);
    }

    public List<Container> getAssignableContainers(Long shipId) {
        getShip(shipId);
        return containerRepository.findByShipIdOrShipIsNull(shipId);
    }

    @Transactional
    public List<Container> assignContainerToShip(Long shipId, List<Long> containerIds) {
        Ship ship = getShip(shipId);
        if (containerIds == null || containerIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one container must be selected");
        }

        List<Container> containers = containerRepository.findAllById(containerIds);
        Set<Long> foundIds = containers.stream()
                .map(Container::getId)
                .collect(Collectors.toSet());

        List<Long> missingIds = containerIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();
        if (!missingIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Containers not found: " + missingIds.stream().map(String::valueOf).collect(Collectors.joining(", "))
            );
        }

        for (Container container : containers) {
            if (container.getShip() != null && !Objects.equals(container.getShip().getId(), shipId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Container " + container.getId() + " is already assigned to another ship"
                );
            }
            container.setShip(ship);
        }

        return containerRepository.saveAll(containers);
    }

    @Transactional
    public Container unassignContainerFromShip(Long shipId, Long containerId) {
        getShip(shipId);
        Container container = containerRepository.findById(containerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Container not found"));

        if (container.getShip() == null || !Objects.equals(container.getShip().getId(), shipId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Container is not assigned to the selected ship"
            );
        }

        container.setShip(null);
        return containerRepository.save(container);
    }

    @Transactional
    public StowagePlanResponse submitStowageCreate(StowageCreateRequest request) {
        Ship ship = getShip(request.getShipId());
        Port port = getPort(request.getPortId());
        List<Container> containers = getContainersForStowage(request.getShipId(), request.getContainerIds());
        List<String> messages = new ArrayList<>();
        messages.add(InitialSuccessMessage());

        Stowage stowage = new Stowage();
        stowage.setData(LocalDate.now());
        stowage.setShip(ship);
        stowage.setPort(port);
        stowage.setStowageType(request.getStowageType() == null ? StowageType.PAKROVIMAS : request.getStowageType());
        stowage.setStowageStatus(
                stowage.getStowageType() == StowageType.ISKROVIMAS
                        ? StowageStatus.LAUKIA_ISKROVIMO
                        : StowageStatus.LAUKIA_PAKROVIMO
        );

        Dimensions dimensions = Dimensions.from(ship, port);
        List<ContainerCoordinate> coordinates = createInitialCoordinates(containers, stowage, dimensions);

        boolean dangerousOk = checkDangerousContainerCorrectness(coordinates);
        List<ContainerCoordinate> dangerousWrong = dangerousOk ? new ArrayList<>() : getWronglyPlacedContainers(coordinates);
        addToMessage(messages, dangerousWrong, "Dangerous container separation rule violated");

        List<Bay> bays = divideStowageBay(dimensions);
        List<ContainerCoordinate> weightWrong = new ArrayList<>();
        List<Double> bayWeights = bays.stream()
                .map(bay -> canculateBayWeight(getContainersInBay(coordinates, bay)))
                .toList();

        if (!checkBayWeightDifference(bayWeights, totalWeight(coordinates))) {
            Bay heaviestBay = bays.get(indexOfMax(bayWeights));
            weightWrong.addAll(getHeaviest(getContainersInBay(coordinates, heaviestBay), dimensions.width));
            addToMessage(messages, weightWrong, "Bay weight difference is higher than 20%");
        }

        boolean unloadingOk = checkUnloadingOrderCorrectness(coordinates);
        List<ContainerCoordinate> unloadingWrong = unloadingOk
                ? new ArrayList<>()
                : getWronglyPlacedContainersByUnloadingOrder(coordinates);
        addToMessage(messages, unloadingWrong, "Unloading order rule violated");

        List<ContainerCoordinate> allWrong = getAllWronglyPlacedContainers(dangerousWrong, weightWrong, unloadingWrong);
        if (checkWronglyPlacedContainersAmount(allWrong)) {
            for (ContainerCoordinate wrongCoordinate : allWrong) {
                List<Position> viablePositions = findViablePositions(wrongCoordinate, coordinates, dimensions);
                if (checkViablePositionAmount(viablePositions)) {
                    Position alternate = chooseAlternatePossition(viablePositions);
                    swap(wrongCoordinate, alternate);
                } else {
                    addToMessage(messages, List.of(wrongCoordinate), "No viable alternate position found");
                }
            }
        }

        Stowage savedPlan = saveStowagePlan(stowage, coordinates);
        messages.add("Stowage plan saved");
        return sendMessage(true, messages, savedPlan);
    }

    @Transactional
    public StowagePlanResponse selectPlan(Long planId, Long shipId) {
        Stowage stowage = getStowagePlan(planId);
        Ship ship = getShip(shipId);

        if (!checkPlan(stowage, ship)) {
            return sendMessage(false, List.of("Plan does not match the selected ship"), stowage);
        }

        stowage.setStowageStatus(StowageStatus.PAKRAUTAS);
        shipRepository.save(ship);
        Stowage saved = stowageRepository.save(stowage);
        return sendMessage(true, List.of("Plan commencement message: ship loading can start"), saved);
    }

    public boolean checkPlan(Stowage stowage, Ship ship) {
        return stowage.getShip() != null
                && ship != null
                && Objects.equals(stowage.getShip().getId(), ship.getId())
                && stowage.getStowageType() == StowageType.PAKROVIMAS;
    }

    private List<ContainerCoordinate> createInitialCoordinates(List<Container> containers, Stowage stowage, Dimensions dimensions) {
        List<ContainerCoordinate> coordinates = new ArrayList<>();
        Set<Position> occupied = new HashSet<>();
        List<Container> sortedContainers = containers.stream()
                .sorted(Comparator
                        .comparing((Container container) -> container.getWarningLabel() != null).reversed()
                        .thenComparing(Container::getWeight, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int n = sortedContainers.size();
        for (int i = 0; i < n; i++) {
            Container container = sortedContainers.get(i);
            Position position = checkForUnoccupiedCoordinate(occupied, dimensions);
            if (position == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ship or port does not have enough free coordinates");
            }
            ContainerCoordinate coordinate = assignCoordinate(container, stowage, position);
            coordinates.add(coordinate);
            occupied.add(position);
        }
        return coordinates;
    }

    public ContainerCoordinate assignCoordinate(Container container, Stowage stowage, Position position) {
        ContainerCoordinate coordinate = new ContainerCoordinate();
        coordinate.setContainer(container);
        coordinate.setStowage(stowage);
        coordinate.setLengthPosition(position.lengthPosition());
        coordinate.setWidthPosition(position.widthPosition());
        coordinate.setHeightPosition(position.heightPosition());
        return coordinate;
    }

    public Position checkForUnoccupiedCoordinate(Set<Position> occupied, Dimensions dimensions) {
        for (int height = 0; height < dimensions.height; height++) {
            for (int length = 0; length < dimensions.length; length++) {
                for (int width = 0; width < dimensions.width; width++) {
                    Position position = new Position(length, width, height);
                    if (!occupied.contains(position)) {
                        return position;
                    }
                }
            }
        }
        return null;
    }

    public boolean checkDangerousContainerCorrectness(List<ContainerCoordinate> coordinates) {
        return getWronglyPlacedContainers(coordinates).isEmpty();
    }

    public List<ContainerCoordinate> getWronglyPlacedContainers(List<ContainerCoordinate> coordinates) {
        Set<ContainerCoordinate> wronglyPlaced = new LinkedHashSet<>();
        for (int i = 0; i < coordinates.size(); i++) {
            ContainerCoordinate first = coordinates.get(i);
            if (!isDangerous(first)) {
                continue;
            }
            for (int j = i + 1; j < coordinates.size(); j++) {
                ContainerCoordinate second = coordinates.get(j);
                if (isDangerous(second) && areAdjacent(first, second)) {
                    wronglyPlaced.add(first);
                    wronglyPlaced.add(second);
                }
            }
        }
        return new ArrayList<>(wronglyPlaced);
    }

    public List<Bay> divideStowageBay(Dimensions dimensions) {
        List<Bay> bays = new ArrayList<>();
        int baseSize = Math.max(1, dimensions.length / 3);
        int start = 0;
        for (int i = 0; i < 3; i++) {
            int end = (i == 2) ? dimensions.length - 1 : Math.min(dimensions.length - 1, start + baseSize - 1);
            bays.add(new Bay(start, end));
            start = end + 1;
        }
        return bays;
    }

    public List<Position> getBayCoordinates(Bay bay, Dimensions dimensions) {
        List<Position> positions = new ArrayList<>();
        for (int length = bay.startLength(); length <= bay.endLength(); length++) {
            for (int width = 0; width < dimensions.width; width++) {
                for (int height = 0; height < dimensions.height; height++) {
                    positions.add(new Position(length, width, height));
                }
            }
        }
        return positions;
    }

    public List<ContainerCoordinate> getContainersInBay(List<ContainerCoordinate> coordinates, Bay bay) {
        return coordinates.stream()
                .filter(coordinate -> coordinate.getLengthPosition() >= bay.startLength()
                        && coordinate.getLengthPosition() <= bay.endLength())
                .toList();
    }

    public double canculateBayWeight(List<ContainerCoordinate> coordinates) {
        return totalWeight(coordinates);
    }

    public boolean checkBayWeightDifference(List<Double> bayWeights, double totalWeight) {
        if (bayWeights.isEmpty() || totalWeight <= 0) {
            return true;
        }
        double max = bayWeights.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        double min = bayWeights.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        return (max - min) <= totalWeight * 0.20;
    }

    public List<ContainerCoordinate> getHeaviest(List<ContainerCoordinate> coordinates, int amount) {
        return coordinates.stream()
                .sorted(Comparator.comparing(
                        (ContainerCoordinate coordinate) -> safeWeight(coordinate.getContainer()),
                        Comparator.reverseOrder()
                ))
                .limit(Math.max(1, amount))
                .toList();
    }

    public boolean checkUnloadingOrderCorrectness(List<ContainerCoordinate> coordinates) {
        return getWronglyPlacedContainersByUnloadingOrder(coordinates).isEmpty();
    }

    public List<ContainerCoordinate> getWronglyPlacedContainersByUnloadingOrder(List<ContainerCoordinate> coordinates) {
        // Orders/routes are not connected to containers in the current codebase yet.
        // The method is intentionally present to match the sequence diagram and can be extended when order-port metadata exists.
        return new ArrayList<>();
    }

    public boolean checkWronglyPlacedContainersAmount(List<ContainerCoordinate> wronglyPlacedContainers) {
        return wronglyPlacedContainers != null && !wronglyPlacedContainers.isEmpty();
    }

    public String InitialSuccessMessage() {
        return "Initial stowage layout generated by LIFO, weight and container type rules";
    }

    @SafeVarargs
    public final List<ContainerCoordinate> getAllWronglyPlacedContainers(List<ContainerCoordinate>... groups) {
        return Arrays.stream(groups)
                .flatMap(Collection::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    public boolean isPositionViable(ContainerCoordinate moving, Position position, List<ContainerCoordinate> allCoordinates) {
        boolean occupied = allCoordinates.stream()
                .anyMatch(coordinate -> !coordinate.equals(moving) && samePosition(coordinate, position));
        if (occupied) {
            return false;
        }
        ContainerCoordinate assumed = cloneAt(moving, position);
        List<ContainerCoordinate> assumedCoordinates = allCoordinates.stream()
                .map(coordinate -> coordinate.equals(moving) ? assumed : coordinate)
                .toList();
        return checkDangerousContainerCorrectness(assumedCoordinates);
    }

    public boolean checkViablePositionAmount(List<Position> viablePositions) {
        return viablePositions != null && !viablePositions.isEmpty();
    }

    public void addToMessage(List<String> messages, List<ContainerCoordinate> coordinates, String message) {
        if (coordinates == null || coordinates.isEmpty()) {
            return;
        }
        String ids = coordinates.stream()
                .map(ContainerCoordinate::getContainer)
                .filter(Objects::nonNull)
                .map(Container::getId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        messages.add(message + (ids.isBlank() ? "" : ": containers " + ids));
    }

    public Position chooseAlternatePossition(List<Position> viablePositions) {
        return viablePositions.stream()
                .min(Comparator
                        .comparing(Position::heightPosition)
                        .thenComparing(Position::lengthPosition)
                        .thenComparing(Position::widthPosition))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No viable alternate position"));
    }

    public void swap(ContainerCoordinate coordinate, Position position) {
        coordinate.setLengthPosition(position.lengthPosition());
        coordinate.setWidthPosition(position.widthPosition());
        coordinate.setHeightPosition(position.heightPosition());
    }

    public StowagePlanResponse sendMessage(boolean success, List<String> messages, Stowage stowagePlan) {
        String mainMessage = messages == null || messages.isEmpty() ? "" : messages.get(messages.size() - 1);
        return new StowagePlanResponse(success, mainMessage, messages == null ? new ArrayList<>() : messages, stowagePlan);
    }

    public Stowage saveStowagePlan(Stowage stowage, List<ContainerCoordinate> coordinates) {
        stowage.getCoordinates().clear();
        for (ContainerCoordinate coordinate : coordinates) {
            coordinate.setStowage(stowage);
            stowage.getCoordinates().add(coordinate);
        }
        return stowageRepository.save(stowage);
    }

    private List<Position> findViablePositions(ContainerCoordinate moving, List<ContainerCoordinate> coordinates, Dimensions dimensions) {
        List<Position> viable = new ArrayList<>();
        for (int length = 0; length < dimensions.length; length++) {
            for (int width = 0; width < dimensions.width; width++) {
                for (int height = 0; height < dimensions.height; height++) {
                    Position position = new Position(length, width, height);
                    if (isPositionViable(moving, position, coordinates)) {
                        viable.add(position);
                    }
                }
            }
        }
        return viable;
    }

    private boolean isDangerous(ContainerCoordinate coordinate) {
        return coordinate != null
                && coordinate.getContainer() != null
                && coordinate.getContainer().getWarningLabel() != null;
    }

    private boolean areAdjacent(ContainerCoordinate first, ContainerCoordinate second) {
        int distance = Math.abs(first.getLengthPosition() - second.getLengthPosition())
                + Math.abs(first.getWidthPosition() - second.getWidthPosition())
                + Math.abs(first.getHeightPosition() - second.getHeightPosition());
        return distance <= 1;
    }

    private boolean samePosition(ContainerCoordinate coordinate, Position position) {
        return Objects.equals(coordinate.getLengthPosition(), position.lengthPosition())
                && Objects.equals(coordinate.getWidthPosition(), position.widthPosition())
                && Objects.equals(coordinate.getHeightPosition(), position.heightPosition());
    }

    private ContainerCoordinate cloneAt(ContainerCoordinate original, Position position) {
        ContainerCoordinate clone = new ContainerCoordinate();
        clone.setContainer(original.getContainer());
        clone.setStowage(original.getStowage());
        clone.setLengthPosition(position.lengthPosition());
        clone.setWidthPosition(position.widthPosition());
        clone.setHeightPosition(position.heightPosition());
        return clone;
    }

    private double totalWeight(List<ContainerCoordinate> coordinates) {
        return coordinates.stream()
                .map(ContainerCoordinate::getContainer)
                .mapToDouble(this::safeWeight)
                .sum();
    }

    private double safeWeight(Container container) {
        return container == null || container.getWeight() == null ? 0.0 : container.getWeight();
    }

    private int indexOfMax(List<Double> values) {
        int index = 0;
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) > max) {
                max = values.get(i);
                index = i;
            }
        }
        return index;
    }

    public record Position(Integer lengthPosition, Integer widthPosition, Integer heightPosition) {
    }

    public record Bay(Integer startLength, Integer endLength) {
    }

    public static class Dimensions {
        private final int length;
        private final int width;
        private final int height;

        private Dimensions(int length, int width, int height) {
            this.length = Math.max(1, length);
            this.width = Math.max(1, width);
            this.height = Math.max(1, height);
        }

        public static Dimensions from(Ship ship, Port port) {
            int length = firstPositive(ship.getLength(), port == null ? null : port.getLength(), 10);
            int width = firstPositive(ship.getWidth(), port == null ? null : port.getWidth(), 6);
            int height = firstPositive(ship.getHeight(), port == null ? null : port.getHeight(), 4);
            return new Dimensions(length, width, height);
        }

        private static int firstPositive(Integer first, Integer second, int fallback) {
            if (first != null && first > 0) {
                return first;
            }
            if (second != null && second > 0) {
                return second;
            }
            return fallback;
        }
    }
}
