package com.ticketwave.booking.controller;

import com.ticketwave.booking.Booking;
import com.ticketwave.booking.dto.BookingDto;
import com.ticketwave.booking.service.BookingService;
import com.ticketwave.booking.service.BookingMapper;
import com.ticketwave.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking", description = "Ticket booking management APIs")
public class BookingController {
    private final BookingService bookingService;
    private final BookingMapper mapper;

    public BookingController(BookingService bookingService, BookingMapper mapper) {
        this.bookingService = bookingService;
        this.mapper = mapper;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate a booking")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingDto>> initiate(@RequestParam Long userId,
                                                             @RequestParam Long scheduleId,
                                                             @RequestBody @NotNull List<Long> seatIds) {
        var b = bookingService.createBooking(userId, scheduleId, seatIds);
        return ResponseEntity.ok(new ApiResponse<>(true, "initiated", mapper.toDto(b)));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm a booking")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<BookingDto>> confirm(@PathVariable Long id) {
        var b = bookingService.confirmBooking(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "confirmed", mapper.toDto(b)));
    }
}
