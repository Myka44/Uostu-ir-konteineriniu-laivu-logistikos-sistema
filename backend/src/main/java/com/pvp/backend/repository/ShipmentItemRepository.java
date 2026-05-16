package com.pvp.backend.repository;

import com.pvp.backend.model.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
    List<ShipmentItem> findByShipmentContainerId(Long containerId);

    @Modifying
    @Transactional
    void deleteByShipmentContainerId(Long containerId);
}