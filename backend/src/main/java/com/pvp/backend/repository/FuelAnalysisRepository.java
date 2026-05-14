package com.pvp.backend.repository;

import com.pvp.backend.model.FuelAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface FuelAnalysisRepository extends JpaRepository<FuelAnalysis, Long> {
    Optional<FuelAnalysis> findByRouteSegmentId(Long segmentId);

    @Modifying
    @Transactional
    void deleteByRouteSegmentId(Long segmentId);
}
