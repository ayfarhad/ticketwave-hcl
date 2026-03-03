package com.ticketwave.booking.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class BookingDto {
    private Long id;
    private String pnr;
    private String status;
    private Instant createdAt;
    private List<Long> seatIds;
}
