package com.ticketwave.payment.service;

import com.ticketwave.booking.Booking;
import com.ticketwave.booking.BookingRepository;
import com.ticketwave.payment.Payment;
import com.ticketwave.payment.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentServiceImplTest {
    @Mock
    private PaymentRepository paymentRepo;
    @Mock
    private BookingRepository bookingRepo;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createPaymentIntentSuccess() {
        // arrange
        Booking booking = new Booking();
        booking.setId(1L);
        booking.setStatus("CONFIRMED");

        Payment payment = new Payment();
        payment.setId(1L);
        payment.setAmount(100.0);

        when(bookingRepo.getReferenceById(1L)).thenReturn(booking);
        when(paymentRepo.save(any(Payment.class))).thenReturn(payment);

        // act
        Payment result = paymentService.createPaymentIntent(1L, 100.0);

        // assert
        assertNotNull(result);
        assertEquals(100.0, result.getAmount());
        verify(paymentRepo).save(any(Payment.class));
    }

    @Test
    void createPaymentIntentBookingNotFound() {
        when(bookingRepo.getReferenceById(1L)).thenThrow(new IllegalArgumentException("Booking not found"));

        assertThrows(IllegalArgumentException.class, 
                () -> paymentService.createPaymentIntent(1L, 100.0));
    }

    @Test
    void confirmPaymentSuccess() {
        // arrange
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus("PENDING");

        when(paymentRepo.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepo.save(any(Payment.class))).thenReturn(payment);

        // act
        Payment result = paymentService.confirmPayment(1L, "ref123");

        // assert
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        verify(paymentRepo).save(any(Payment.class));
    }

    @Test
    void confirmPaymentNotFound() {
        when(paymentRepo.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, 
                () -> paymentService.confirmPayment(1L, "ref123"));
    }
}

