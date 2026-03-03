package com.ticketwave.booking.service;

import com.ticketwave.booking.Booking;
import com.ticketwave.booking.dto.BookingDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    @Mapping(target = "seatIds", expression = "java(booking.getItems().stream().map(i->i.getSeat().getId()).toList())")
    BookingDto toDto(Booking booking);
}
