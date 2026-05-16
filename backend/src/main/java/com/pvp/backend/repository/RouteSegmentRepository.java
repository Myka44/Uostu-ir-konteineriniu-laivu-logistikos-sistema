package com.pvp.backend.repository;

import com.pvp.backend.model.RouteSegment;
import com.pvp.backend.model.RouteSegmentState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface RouteSegmentRepository extends JpaRepository<RouteSegment, Long> {
    List<RouteSegment> findByRouteIdOrderBySequenceNumber(Long routeId);
    List<RouteSegment> findByRouteId(Long routeId);

    @Modifying
    @Transactional
    void deleteByRouteId(Long routeId);

    Optional<RouteSegment> findFirstByRouteIdAndStateOrderBySequenceNumber(Long routeId, RouteSegmentState state);
    Optional<RouteSegment> findFirstByRouteIdAndStateNotOrderBySequenceNumber(Long routeId, RouteSegmentState state);
}
