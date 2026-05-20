package com.pvp.backend.repository;

import com.pvp.backend.model.Container;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ContainerRepository extends JpaRepository<Container, Long> {
    List<Container> findByShipId(Long shipId);

    List<Container> findByShipIsNull();

    List<Container> findByShipIdOrShipIsNull(Long shipId);

    List<Container> findByOrderId(Long orderId);

    @Modifying
    @Transactional
    void deleteByOrderId(Long orderId);
}
