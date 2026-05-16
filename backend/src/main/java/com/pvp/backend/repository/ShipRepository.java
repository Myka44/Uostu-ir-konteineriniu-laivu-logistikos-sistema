package com.pvp.backend.repository;

import com.pvp.backend.model.Ship;
import com.pvp.backend.model.ShipState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShipRepository extends JpaRepository<Ship, Long> {
    List<Ship> findByState(ShipState state);
}
