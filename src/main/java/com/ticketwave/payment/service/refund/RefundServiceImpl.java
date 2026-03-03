package com.ticketwave.payment.service.refund;

import com.ticketwave.payment.PaymentRepository;
import com.ticketwave.payment.Refund;
import com.ticketwave.payment.RefundRepository;
import org.springframework.stereotype.Service;

@Service
public class RefundServiceImpl implements RefundService {
    private final RefundRepository refundRepo;
    private final PaymentRepository paymentRepo;

    public RefundServiceImpl(RefundRepository refundRepo, PaymentRepository paymentRepo) {
        this.refundRepo = refundRepo;
        this.paymentRepo = paymentRepo;
    }

    @Override
    public Refund createRefund(Long paymentId, Double amount, String reason) {
        var payment = paymentRepo.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        // simple prorate logic omitted
        Refund r = new Refund();
        r.setPayment(payment);
        r.setAmount(amount);
        r.setReason(reason);
        return refundRepo.save(r);
    }
}
