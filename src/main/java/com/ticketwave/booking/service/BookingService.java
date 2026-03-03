package com.ticketwave.booking.service;

import com.ticketwave.booking.Booking;

public interface BookingService {
    Booking createBooking(Long userId, Long scheduleId, java.util.List<Long> seatIds);
    Booking confirmBooking(Long bookingId);
}
