package com.ticketwave.payment.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class PaymentDto {
    private Long id;
    private Long bookingId;
    private Double amount;
    private String status;
    private String externalReference;
    private Instant createdAt;
}
