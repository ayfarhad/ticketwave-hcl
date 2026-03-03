package com.ticketwave.payment;

import com.ticketwave.booking.Booking;
import com.ticketwave.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
public class Payment extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "external_reference", length = 100)
    private String externalReference;
}
