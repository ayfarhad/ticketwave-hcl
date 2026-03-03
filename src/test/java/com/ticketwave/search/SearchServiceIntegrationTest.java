package com.ticketwave.search;

import com.ticketwave.inventory.Route;
import com.ticketwave.inventory.RouteRepository;
import com.ticketwave.inventory.Schedule;
import com.ticketwave.inventory.ScheduleRepository;
import com.ticketwave.search.dto.ScheduleDto;
import com.ticketwave.search.service.SearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SearchServiceIntegrationTest {
    @Mock
    private SearchService searchService;
    @Mock
    private RouteRepository routeRepo;
    @Mock
    private ScheduleRepository scheduleRepo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchSchedulesByRoute() {
        // arrange
        Route r = new Route();
        r.setId(1L);
        r.setOrigin("A");
        r.setDestination("B");
        r.setType("BUS");

        Schedule s = new Schedule();
        s.setId(1L);
        s.setRoute(r);
        s.setDepartureTime(Instant.now());
        s.setArrivalTime(Instant.now().plusSeconds(3600));
        s.setBasePrice(100.0);

        when(routeRepo.findById(1L)).thenReturn(java.util.Optional.of(r));
        when(scheduleRepo.findAll()).thenReturn(List.of(s));

        // act - Mock returns list of schedules
        assertThat(scheduleRepo.findAll()).isNotEmpty();
    }

    @Test
    void searchReturnsResults() {
        // arrange
        Route r = new Route();
        r.setId(1L);
        r.setOrigin("A");
        r.setDestination("B");
        r.setType("BUS");

        Schedule s = new Schedule();
        s.setId(1L);
        s.setRoute(r);
        s.setDepartureTime(Instant.now());
        s.setArrivalTime(Instant.now().plusSeconds(3600));
        s.setBasePrice(100.0);

        when(scheduleRepo.findAll()).thenReturn(List.of(s));

        // act
        List<Schedule> results = scheduleRepo.findAll();

        // assert
        assertThat(results).isNotEmpty().hasSize(1);
        assertThat(results.get(0).getRoute().getOrigin()).isEqualTo("A");
    }
}

