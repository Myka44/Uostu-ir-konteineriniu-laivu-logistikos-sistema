package com.pvp.backend;

import com.pvp.backend.model.Ship;
import com.pvp.backend.model.ShipState;
import com.pvp.backend.model.ShipType;
import com.pvp.backend.repository.PortRepository;
import com.pvp.backend.repository.ShipRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StowageSampleDataLoader implements CommandLineRunner {
    private final ShipRepository shipRepository;
    private final PortRepository portRepository;

    public StowageSampleDataLoader(ShipRepository shipRepository, PortRepository portRepository) {
        this.shipRepository = shipRepository;
        this.portRepository = portRepository;
    }

    @Override
    public void run(String... args) {
        if (shipRepository.count() > 0) {
            return;
        }

        Ship ship = new Ship();
        ship.setName("Klaipeda");
        ship.setType(ShipType.KLAIPEDA);
        ship.setState(ShipState.ARRIVED);
        ship.setRegistrationCountry("LIETUVA");
        ship.setWeight(20000.0);
        ship.setCapacity(240);
        ship.setLength(12);
        ship.setWidth(8);
        ship.setHeight(4);
        ship.setFuelAmount(12000.0);
        ship.setBaseFuelConsumption(150.0);
        portRepository.findAll().stream().findFirst().ifPresent(ship::setPort);
        shipRepository.save(ship);
    }
}
