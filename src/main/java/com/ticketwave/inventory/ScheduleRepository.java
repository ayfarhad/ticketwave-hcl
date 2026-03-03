package com.ticketwave.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findByRouteIdAndDepartureTimeBetween(Long routeId, Instant start, Instant end);
}
