package com.ticketwave.payment.controller;

import com.ticketwave.common.ApiResponse;
import com.ticketwave.payment.Payment;
import com.ticketwave.payment.dto.PaymentDto;
import com.ticketwave.payment.service.PaymentMapper;
import com.ticketwave.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PaymentControllerTest {
    @Mock
    private PaymentService paymentService;
    @Mock
    private PaymentMapper mapper;
    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createIntentSuccess() {
        // arrange
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setAmount(100.0);

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setId(1L);

        when(paymentService.createPaymentIntent(1L, 100.0)).thenReturn(payment);
        when(mapper.toDto(payment)).thenReturn(paymentDto);

        // act
        ResponseEntity<ApiResponse<PaymentDto>> response = paymentController.createIntent(1L, 100.0);

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("created", response.getBody().getMessage());
    }

    @Test
    void confirmPaymentSuccess() {
        // arrange
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus("COMPLETED");

        PaymentDto paymentDto = new PaymentDto();
        paymentDto.setId(1L);

        when(paymentService.confirmPayment(1L, "ref123")).thenReturn(payment);
        when(mapper.toDto(payment)).thenReturn(paymentDto);

        // act
        ResponseEntity<ApiResponse<PaymentDto>> response = paymentController.confirm(1L, "ref123");

        // assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals("confirmed", response.getBody().getMessage());
    }
}
