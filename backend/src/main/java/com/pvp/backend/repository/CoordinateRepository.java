package com.pvp.backend.repository;

import com.pvp.backend.model.Coordinate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CoordinateRepository extends JpaRepository<Coordinate, Long> {
    List<Coordinate> findByRouteSegmentIdOrderBySequenceNumber(Long segmentId);

    @Modifying
    @Transactional
    void deleteByRouteSegmentId(Long segmentId);
}
