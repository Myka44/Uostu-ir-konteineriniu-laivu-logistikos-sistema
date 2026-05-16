package com.pvp.backend.repository;

import com.pvp.backend.model.Stowage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StowageRepository extends JpaRepository<Stowage, Long> {
    List<Stowage> findByShipId(Long shipId);
}
