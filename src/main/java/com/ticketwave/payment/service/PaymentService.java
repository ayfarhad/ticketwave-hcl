package com.ticketwave.payment.service;

import com.ticketwave.payment.Payment;

public interface PaymentService {
    Payment createPaymentIntent(Long bookingId, Double amount);
    Payment confirmPayment(Long paymentId, String externalRef);
}
