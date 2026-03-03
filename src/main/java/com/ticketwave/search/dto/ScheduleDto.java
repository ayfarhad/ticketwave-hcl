package com.ticketwave.search.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ScheduleDto {
    private Long id;
    private String origin;
    private String destination;
    private Instant departureTime;
    private Instant arrivalTime;
    private Double basePrice;
}
