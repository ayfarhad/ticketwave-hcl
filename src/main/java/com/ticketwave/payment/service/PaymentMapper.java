package com.ticketwave.payment.service;

import com.ticketwave.payment.Payment;
import com.ticketwave.payment.dto.PaymentDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(target = "bookingId", expression = "java(payment.getBooking().getId())")
    PaymentDto toDto(Payment payment);
}
