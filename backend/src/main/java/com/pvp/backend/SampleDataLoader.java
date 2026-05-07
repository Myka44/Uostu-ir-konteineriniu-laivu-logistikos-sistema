package com.pvp.backend;

import com.pvp.backend.model.Order;
import com.pvp.backend.model.UzsakymoBusena;
import com.pvp.backend.repository.OrderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class SampleDataLoader implements CommandLineRunner {

    private final OrderRepository orderRepository;

    public SampleDataLoader(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (orderRepository.count() > 0) {
            return;
        }

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
    }
}
