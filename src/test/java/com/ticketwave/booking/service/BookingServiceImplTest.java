package com.ticketwave.booking.service;

import com.ticketwave.booking.Booking;
import com.ticketwave.booking.BookingItem;
import com.ticketwave.booking.BookingItemRepository;
import com.ticketwave.booking.BookingRepository;
import com.ticketwave.common.util.PnrGenerator;
import com.ticketwave.inventory.Schedule;
import com.ticketwave.inventory.Seat;
import com.ticketwave.inventory.SeatRepository;
import com.ticketwave.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class BookingServiceImplTest {
    @Mock
    private BookingRepository bookingRepo;
    @Mock
    private BookingItemRepository itemRepo;
    @Mock
    private SeatRepository seatRepo;
    @Mock
    private PnrGenerator pnrGenerator;
    @InjectMocks
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createBookingSuccess() {
        // arrange
        User user = new User();
        user.setId(1L);
        
        Schedule schedule = new Schedule();
        schedule.setBasePrice(100.0);

        Seat seat = new Seat();
        seat.setId(1L);
        seat.setStatus("HELD");
        seat.setSchedule(schedule);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setPnr("PNR123");
        booking.setStatus("INITIATED");

        when(seatRepo.findAllById(List.of(1L))).thenReturn(List.of(seat));
        when(pnrGenerator.generate()).thenReturn("PNR123");
        when(bookingRepo.save(any(Booking.class))).thenReturn(booking);
        when(itemRepo.saveAll(anyList())).thenReturn(List.of());

        // act
        Booking result = bookingService.createBooking(1L, 1L, List.of(1L));

        // assert
        assertNotNull(result);
        assertEquals("PNR123", result.getPnr());
        assertEquals("INITIATED", result.getStatus());
        verify(bookingRepo).save(any(Booking.class));
    }

    @Test
    void createBookingSeatsNotHeld() {
        // arrange
        Seat seat = new Seat();
        seat.setStatus("AVAILABLE"); // not held

        when(seatRepo.findAllById(anyList())).thenReturn(List.of(seat));

        // act & assert
        assertThrows(IllegalStateException.class, 
                () -> bookingService.createBooking(1L, 1L, List.of(1L)));
    }

    @Test
    void confirmBookingSuccess() {
        // arrange
        Seat seat = new Seat();
        seat.setId(1L);

        BookingItem item = new BookingItem();
        item.setSeat(seat);

        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus("INITIATED");
        booking.setItems(List.of(item));

        when(bookingRepo.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepo.save(any(Booking.class))).thenReturn(booking);

        // act
        Booking result = bookingService.confirmBooking(1L);

        // assert
        assertNotNull(result);
        assertEquals("CONFIRMED", result.getStatus());
        ArgumentCaptor<Seat> seatCaptor = ArgumentCaptor.forClass(Seat.class);
        verify(seatRepo).save(seatCaptor.capture());
        assertEquals("BOOKED", seatCaptor.getValue().getStatus());
    }

    @Test
    void confirmBookingNotFound() {
        when(bookingRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, 
                () -> bookingService.confirmBooking(1L));
    }
}
