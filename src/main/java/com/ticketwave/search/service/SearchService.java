package com.ticketwave.search.service;

import com.ticketwave.search.dto.ScheduleDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface SearchService {
    Page<ScheduleDto> search(String origin, String destination, Instant date, String type, Pageable pageable);
}
