package com.pvp.backend.repository;

import com.pvp.backend.model.ContainerCoordinate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContainerCoordinateRepository extends JpaRepository<ContainerCoordinate, Long> {
}
