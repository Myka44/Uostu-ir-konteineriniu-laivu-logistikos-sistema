package com.pvp.backend.repository;

import com.pvp.backend.model.ShipmentContainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ShipmentContainerRepository extends JpaRepository<ShipmentContainer, Long> {
    List<ShipmentContainer> findByOrderId(Long orderId);

    @Modifying
    @Transactional
    void deleteByOrderId(Long orderId);
}