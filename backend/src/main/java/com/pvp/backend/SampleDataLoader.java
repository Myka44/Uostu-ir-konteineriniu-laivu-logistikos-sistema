package com.pvp.backend;

import com.pvp.backend.model.Order;
import com.pvp.backend.model.Port;
import com.pvp.backend.model.UzsakymoBusena;
import com.pvp.backend.model.OrderItem;
import com.pvp.backend.repository.OrderItemRepository;
import com.pvp.backend.repository.OrderRepository;
import com.pvp.backend.repository.PortRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SampleDataLoader implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final PortRepository portRepository;
    private final com.pvp.backend.repository.ItemRepository itemRepository;
    private final OrderItemRepository orderItemRepository;

    public SampleDataLoader(OrderRepository orderRepository, PortRepository portRepository, com.pvp.backend.repository.ItemRepository itemRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.portRepository = portRepository;
        this.itemRepository = itemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (portRepository.count() == 0) {
            Port p1 = new Port();
            p1.setName("Klaipėda");
            Port p2 = new Port();
            p2.setName("Riga");
            Port p3 = new Port();
            p3.setName("Tallinn");
            Port p4 = new Port();
            p4.setName("Gdansk");
            portRepository.save(p1);
            portRepository.save(p2);
            portRepository.save(p3);
            portRepository.save(p4);
        }

        // ensure items exist before creating orders with items
        if (itemRepository.count() == 0) {
            com.pvp.backend.model.Item pA = new com.pvp.backend.model.Item();
            pA.setName("Electronics Box");
            pA.setCategory(com.pvp.backend.model.ItemCategory.ELECTRONICS);
            pA.setWeight(2.5);
            pA.setLength(0.5);
            pA.setHeight(0.3);
            pA.setWidth(0.4);
            pA.setVolume(0.5 * 0.3 * 0.4);
            pA.setHazardLabel(com.pvp.backend.model.HazardLabel.NONE);

            com.pvp.backend.model.Item pB = new com.pvp.backend.model.Item();
            pB.setName("Chemical Drum");
            pB.setCategory(com.pvp.backend.model.ItemCategory.CHEMICALS);
            pB.setWeight(10.0);
            pB.setLength(1.2);
            pB.setHeight(0.8);
            pB.setWidth(0.6);
            pB.setVolume(1.2 * 0.8 * 0.6);
            pB.setHazardLabel(com.pvp.backend.model.HazardLabel.HAZARDOUS);

            com.pvp.backend.model.Item pC = new com.pvp.backend.model.Item();
            pC.setName("Wooden Pallet");
            pC.setCategory(com.pvp.backend.model.ItemCategory.FURNITURE);
            pC.setWeight(30.0);
            pC.setLength(1.2);
            pC.setHeight(0.2);
            pC.setWidth(1.0);
            pC.setVolume(1.2 * 0.2 * 1.0);
            pC.setHazardLabel(com.pvp.backend.model.HazardLabel.NONE);

            com.pvp.backend.model.Item pD = new com.pvp.backend.model.Item();
            pD.setName("Flammable Solvent");
            pD.setCategory(com.pvp.backend.model.ItemCategory.FLAMMABLES);
            pD.setWeight(8.0);
            pD.setLength(0.6);
            pD.setHeight(0.3);
            pD.setWidth(0.3);
            pD.setVolume(0.6 * 0.3 * 0.3);
            pD.setHazardLabel(com.pvp.backend.model.HazardLabel.FLAMMABLE);

            itemRepository.save(pA);
            itemRepository.save(pB);
            itemRepository.save(pC);
            itemRepository.save(pD);
        }

        if (orderRepository.count() == 0) {
            Order o1 = new Order();
            o1.setSukurimoData(LocalDate.now().minusDays(3));
            o1.setBusena(UzsakymoBusena.LAUKIAMA);
            o1.setIsvykimoUostas("Klaipėda");
            o1.setAtvykimoUostas("Riga");
            o1.setClientId(1L);

            Order o2 = new Order();
            o2.setSukurimoData(LocalDate.now().minusDays(10));
            o2.setBusena(UzsakymoBusena.VYKDOMA);
            o2.setIsvykimoUostas("Tallinn");
            o2.setAtvykimoUostas("Klaipėda");
            o2.setClientId(2L);

            Order o3 = new Order();
            o3.setSukurimoData(LocalDate.now().minusDays(20));
            o3.setBusena(UzsakymoBusena.ATLIKTA);
            o3.setIsvykimoUostas("Gdansk");
            o3.setAtvykimoUostas("Klaipėda");
            o3.setClientId(3L);

            orderRepository.save(o1);
            orderRepository.save(o2);
            orderRepository.save(o3);

            // attach some items to orders
            var items = itemRepository.findAll();
            if (items.size() >= 3) {
                com.pvp.backend.model.Item a = items.get(0);
                com.pvp.backend.model.Item b = items.get(1);
                com.pvp.backend.model.Item c = items.get(2);

                OrderItem oi1 = new OrderItem();
                oi1.setOrder(o1);
                oi1.setItem(a);
                oi1.setQuantity(2);
                oi1.setTotalWeight(a.getWeight() * 2);
                oi1.setTotalVolume(a.getVolume() * 2);
                orderItemRepository.save(oi1);

                OrderItem oi2 = new OrderItem();
                oi2.setOrder(o1);
                oi2.setItem(b);
                oi2.setQuantity(1);
                oi2.setTotalWeight(b.getWeight() * 1);
                oi2.setTotalVolume(b.getVolume() * 1);
                orderItemRepository.save(oi2);

                OrderItem oi3 = new OrderItem();
                oi3.setOrder(o2);
                oi3.setItem(c);
                oi3.setQuantity(3);
                oi3.setTotalWeight(c.getWeight() * 3);
                oi3.setTotalVolume(c.getVolume() * 3);
                orderItemRepository.save(oi3);
            }
        }
    }
}
