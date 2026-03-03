package com.ticketwave.inventory.service;

import com.ticketwave.inventory.Seat;
import com.ticketwave.inventory.SeatRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {
    private final SeatRepository seatRepo;
    private final RedisTemplate<String, Object> redis;
    private static final Duration HOLD_TTL = Duration.ofMinutes(5);

    public InventoryServiceImpl(SeatRepository seatRepo, RedisTemplate<String, Object> redis) {
        this.seatRepo = seatRepo;
        this.redis = redis;
    }

    @Override
    public List<Seat> availableSeats(Long scheduleId) {
        return seatRepo.findByScheduleIdAndStatus(scheduleId, "AVAILABLE");
    }

    @Override
    public boolean holdSeats(Long scheduleId, List<Long> seatIds, String sessionId) {
        List<Seat> seats = seatRepo.findAllById(seatIds);
        if (seats.stream().anyMatch(s -> !"AVAILABLE".equals(s.getStatus()))) {
            return false;
        }
        seats.forEach(s -> s.setStatus("HELD"));
        seatRepo.saveAll(seats);
        redis.opsForValue().set("hold:" + sessionId, seatIds, HOLD_TTL);
        return true;
    }

    @Override
    public void releaseHold(String sessionId) {
        Object val = redis.opsForValue().get("hold:" + sessionId);
        if (val instanceof List<?> ids) {
            List<Long> seatIds = ids.stream().map(o -> (Long)o).collect(Collectors.toList());
            seatRepo.findAllById(seatIds).forEach(s -> s.setStatus("AVAILABLE"));
            seatRepo.saveAll(seatRepo.findAllById(seatIds));
        }
        redis.delete("hold:" + sessionId);
    }
}
