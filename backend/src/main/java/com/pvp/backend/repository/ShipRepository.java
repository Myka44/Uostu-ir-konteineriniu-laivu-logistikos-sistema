package com.pvp.backend.repository;

import com.pvp.backend.model.Ship;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipRepository extends JpaRepository<Ship, Long> {
}
