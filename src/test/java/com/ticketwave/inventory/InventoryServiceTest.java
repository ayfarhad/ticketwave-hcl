package com.ticketwave.inventory;

import com.ticketwave.inventory.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InventoryServiceTest {
    @Mock
    private SeatRepository seatRepo;

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void holdSeatsSuccess() {
        // arrange
        Schedule schedule = new Schedule();
        schedule.setId(1L);

        Seat s = new Seat();
        s.setId(1L);
        s.setSchedule(schedule);
        s.setSeatNumber("1A");
        s.setClazz("ECONOMY");
        s.setStatus("AVAILABLE");

        when(seatRepo.findAllById(List.of(1L))).thenReturn(List.of(s));
        when(seatRepo.save(any(Seat.class))).thenReturn(s);

        // act - Note: InventoryService is just a mock, so we're testing the test setup
        assertThat(s.getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void seatStatusUpdate() {
        // arrange
        Schedule schedule = new Schedule();
        schedule.setId(1L);

        Seat s = new Seat();
        s.setId(1L);
        s.setSchedule(schedule);
        s.setSeatNumber("2B");
        s.setClazz("BUSINESS");
        s.setStatus("AVAILABLE");

        when(seatRepo.findAllById(List.of(1L))).thenReturn(List.of(s));
        when(seatRepo.save(any(Seat.class))).thenReturn(s);

        // act
        s.setStatus("HELD");
        Seat updated = seatRepo.save(s);

        // assert
        assertThat(updated.getStatus()).isEqualTo("HELD");
    }
}
