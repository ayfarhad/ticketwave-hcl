package com.ticketwave.booking.controller;

import com.ticketwave.booking.Booking;
import com.ticketwave.booking.dto.BookingDto;
import com.ticketwave.booking.service.BookingMapper;
import com.ticketwave.booking.service.BookingService;
import com.ticketwave.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class BookingControllerTest {
    @Mock
    private BookingService bookingService;
    @Mock
    private BookingMapper mapper;
    @InjectMocks
    private BookingController bookingController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void initiateBookingSuccess() {
        // arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setPnr("PNR123");

        BookingDto bookingDto = new BookingDto();
        bookingDto.setId(1L);

        when(bookingService.createBooking(1L, 1L, List.of(1L))).thenReturn(booking);
        when(mapper.toDto(booking)).thenReturn(bookingDto);

        // act
        ResponseEntity<ApiResponse<BookingDto>> response = bookingController.initiate(1L, 1L, List.of(1L));

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("initiated", response.getBody().getMessage());
    }

    @Test
    void confirmBookingSuccess() {
        // arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus("CONFIRMED");

        BookingDto bookingDto = new BookingDto();
        bookingDto.setId(1L);

        when(bookingService.confirmBooking(1L)).thenReturn(booking);
        when(mapper.toDto(booking)).thenReturn(bookingDto);

        // act
        ResponseEntity<ApiResponse<BookingDto>> response = bookingController.confirm(1L);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("confirmed", response.getBody().getMessage());
    }
}
