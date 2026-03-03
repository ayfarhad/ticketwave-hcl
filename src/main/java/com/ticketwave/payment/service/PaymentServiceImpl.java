package com.ticketwave.payment.service;

import com.ticketwave.booking.BookingRepository;
import com.ticketwave.payment.Payment;
import com.ticketwave.payment.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepo;
    private final BookingRepository bookingRepo;

    public PaymentServiceImpl(PaymentRepository paymentRepo, BookingRepository bookingRepo) {
        this.paymentRepo = paymentRepo;
        this.bookingRepo = bookingRepo;
    }

    @Override
    public Payment createPaymentIntent(Long bookingId, Double amount) {
        Payment p = new Payment();
        p.setBooking(bookingRepo.getReferenceById(bookingId));
        p.setAmount(amount);
        p.setStatus("PENDING");
        return paymentRepo.save(p);
    }

    @Override
    @Transactional
    public Payment confirmPayment(Long paymentId, String externalRef) {
        Payment p = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        p.setStatus("COMPLETED");
        p.setExternalReference(externalRef);
        return paymentRepo.save(p);
    }
}
