package com.ticketwave.payment.service.refund;

import com.ticketwave.payment.Refund;

public interface RefundService {
    Refund createRefund(Long paymentId, Double amount, String reason);
}
