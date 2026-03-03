package com.ticketwave.inventory.service;

import com.ticketwave.inventory.Seat;

import java.util.List;

public interface InventoryService {
    List<Seat> availableSeats(Long scheduleId);
    boolean holdSeats(Long scheduleId, List<Long> seatIds, String sessionId);
    void releaseHold(String sessionId);
}
