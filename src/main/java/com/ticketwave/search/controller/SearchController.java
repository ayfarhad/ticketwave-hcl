package com.ticketwave.search.controller;

import com.ticketwave.common.ApiResponse;
import com.ticketwave.search.dto.ScheduleDto;
import com.ticketwave.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@Tag(name = "Search", description = "Search for schedules and available trips")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/api/search")
    @Operation(summary = "Search schedules by route and date")
    public ResponseEntity<ApiResponse<Page<ScheduleDto>>> search(
            @RequestParam String origin,
            @RequestParam String destination,
            @RequestParam(required = false) Instant date,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ScheduleDto> result = searchService.search(origin, destination, date, type, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Success", result));
    }
}
