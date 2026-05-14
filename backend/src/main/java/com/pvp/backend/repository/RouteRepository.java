package com.pvp.backend.repository;

import com.pvp.backend.model.Route;
import com.pvp.backend.model.RouteState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    List<Route> findByShipId(Long shipId);
    Optional<Route> findByShipIdAndState(Long shipId, RouteState state);
    List<Route> findByState(RouteState state);
}
