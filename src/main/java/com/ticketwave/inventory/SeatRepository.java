package com.ticketwave.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByScheduleIdAndStatus(Long scheduleId, String status);
}
