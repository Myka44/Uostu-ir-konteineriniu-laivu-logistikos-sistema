package com.pvp.backend;

import com.pvp.backend.model.*;
import com.pvp.backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SampleDataLoader implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final PortRepository portRepository;
    private final ItemRepository itemRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipRepository shipRepository;

    public SampleDataLoader(OrderRepository orderRepository,
                            PortRepository portRepository,
                            ItemRepository itemRepository,
                            OrderItemRepository orderItemRepository,
                            ShipRepository shipRepository) {
        this.orderRepository = orderRepository;
        this.portRepository = portRepository;
        this.itemRepository = itemRepository;
        this.orderItemRepository = orderItemRepository;
        this.shipRepository = shipRepository;
    }

    @Override
    public void run(String... args) {
        seedPorts();
        seedItems();
        seedOrders();
        seedShips();
    }

    private void seedPorts() {
        if (portRepository.count() > 0) return;
        Port[] ports = {
                port("Klaipėda",  Country.LIETUVA,          5, 200, 300, 60, 20, 55.7033, 21.1396),
                port("Riga",      Country.MAZOJI_LIETUVA,   4, 150, 250, 55, 18, 56.9496, 24.1052),
                port("Tallinn",   Country.NAUJOJI_LIETUVA,  3, 100, 200, 50, 15, 59.4370, 24.7536),
                port("Gdansk",    Country.VALDIJA,           6, 250, 350, 70, 22, 54.3520, 18.6466)
        };
        for (Port p : ports) portRepository.save(p);
    }

    private Port port(String name, Country country, int docks, int containers,
                      int length, int width, int height, double lat, double lon) {
        Port p = new Port();
        p.setName(name); p.setCountry(country); p.setDockCount(docks);
        p.setContainerCapacity(containers); p.setLength(length);
        p.setWidth(width); p.setHeight(height); p.setOpen(true);
        p.setLatitude(lat); p.setLongitude(lon);
        return p;
    }

    private void seedItems() {
        if (itemRepository.count() > 0) return;
        Item[] items = {
                item("Electronics Box", ItemCategory.ELECTRONICS, 2.5, 0.5, 0.3, 0.4, HazardLabel.NONE),
                item("Chemical Drum", ItemCategory.CHEMICALS, 10.0, 1.2, 0.8, 0.6, HazardLabel.HAZARDOUS),
                item("Wooden Pallet", ItemCategory.FURNITURE, 30.0, 1.2, 0.2, 1.0, HazardLabel.NONE),
                item("Flammable Solvent", ItemCategory.FLAMMABLES, 8.0, 0.6, 0.3, 0.3, HazardLabel.FLAMMABLE)
        };
        for (Item i : items) itemRepository.save(i);
    }

    private Item item(String name, ItemCategory cat, double weight,
                      double length, double height, double width, HazardLabel label) {
        Item i = new Item();
        i.setName(name);
        i.setCategory(cat);
        i.setWeight(weight);
        i.setLength(length);
        i.setHeight(height);
        i.setWidth(width);
        i.setVolume(length * height * width);
        i.setHazardLabel(label);
        return i;
    }

    private void seedOrders() {
        if (orderRepository.count() > 0) return;
        Order o1 = new Order();
        o1.setSukurimoData(LocalDate.now().minusDays(3));
        o1.setBusena(UzsakymoBusena.LAUKIAMA);
        o1.setIsvykimoUostas("Klaipėda");
        o1.setAtvykimoUostas("Riga");
        o1.setClientId(1L);
        orderRepository.save(o1);

        Order o2 = new Order();
        o2.setSukurimoData(LocalDate.now().minusDays(10));
        o2.setBusena(UzsakymoBusena.VYKDOMA);
        o2.setIsvykimoUostas("Tallinn");
        o2.setAtvykimoUostas("Klaipėda");
        o2.setClientId(2L);
        orderRepository.save(o2);

        var allItems = itemRepository.findAll();
        if (allItems.size() >= 2) {
            Item a = allItems.get(0), b = allItems.get(1);
            orderItem(o1, a, 2);
            orderItem(o1, b, 1);
            orderItem(o2, allItems.size() > 2 ? allItems.get(2) : a, 3);
        }
    }

    private void orderItem(Order order, Item item, int qty) {
        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setItem(item);
        oi.setQuantity(qty);
        oi.setTotalWeight(item.getWeight() * qty);
        oi.setTotalVolume(item.getVolume() * qty);
        orderItemRepository.save(oi);
    }

    private void seedShips() {
        if (shipRepository.count() > 0) return;
        var ports = portRepository.findAll();
        Port klaipeda = ports.stream().filter(p -> p.getName().equals("Klaipėda")).findFirst().orElse(null);
        Port riga = ports.stream().filter(p -> p.getName().equals("Riga")).findFirst().orElse(null);

        Ship s1 = new Ship();
        s1.setName("LDK Gediminas");
        s1.setType(ShipType.LDK);
        s1.setCountry(Country.LIETUVA);
        s1.setWeight(5000.0);
        s1.setCapacity(200);
        s1.setState(ShipState.ARRIVED);
        s1.setBaseFuelConsumption(0.15);
        s1.setFuelAmount(1500.0);
        s1.setLength(180);
        s1.setWidth(28);
        s1.setHeight(35);
        s1.setPort(klaipeda);
        shipRepository.save(s1);

        Ship s2 = new Ship();
        s2.setName("Klaipėda Star");
        s2.setType(ShipType.KLAIPEDA);
        s2.setCountry(Country.LIETUVA);
        s2.setWeight(3000.0);
        s2.setCapacity(120);
        s2.setState(ShipState.ARRIVED);
        s2.setBaseFuelConsumption(0.10);
        s2.setFuelAmount(800.0);
        s2.setLength(150);
        s2.setWidth(24);
        s2.setHeight(30);
        s2.setPort(riga);
        shipRepository.save(s2);

        Ship s3 = new Ship();
        s3.setName("Baltic Voyager");
        s3.setType(ShipType.VALDAS);
        s3.setCountry(Country.MAZOJI_LIETUVA);
        s3.setWeight(7000.0);
        s3.setCapacity(300);
        s3.setState(ShipState.DEPARTED);
        s3.setBaseFuelConsumption(0.20);
        s3.setFuelAmount(2000.0);
        s3.setLength(220);
        s3.setWidth(32);
        s3.setHeight(40);
        shipRepository.save(s3);
    }
}
