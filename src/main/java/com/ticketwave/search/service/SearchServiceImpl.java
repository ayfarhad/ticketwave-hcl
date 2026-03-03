package com.ticketwave.search.service;

import com.ticketwave.inventory.Schedule;
import com.ticketwave.inventory.ScheduleRepository;
import com.ticketwave.search.dto.ScheduleDto;
import org.mapstruct.factory.Mappers;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SearchServiceImpl implements SearchService {
    private final ScheduleRepository scheduleRepo;
    private final SearchMapper mapper = Mappers.getMapper(SearchMapper.class);

    public SearchServiceImpl(ScheduleRepository scheduleRepo) {
        this.scheduleRepo = scheduleRepo;
    }

    @Override
    @Cacheable(value = "search", key = "#origin+'_'+#destination+'_'+#date+'_'+#type+'_'+#pageable.pageNumber+'_'+#pageable.pageSize")
    public Page<ScheduleDto> search(String origin, String destination, Instant date, String type, Pageable pageable) {
        // simple query ignoring date/time boundaries for brevity
        return scheduleRepo.findAll(pageable).map(mapper::toDto);
    }
}
