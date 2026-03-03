package com.ticketwave.search.service;

import com.ticketwave.inventory.Schedule;
import com.ticketwave.search.dto.ScheduleDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SearchMapper {
    ScheduleDto toDto(Schedule schedule);
}
