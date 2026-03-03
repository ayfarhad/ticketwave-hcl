package com.ticketwave.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    Optional<Route> findByOriginAndDestinationAndType(String origin, String destination, String type);
}
