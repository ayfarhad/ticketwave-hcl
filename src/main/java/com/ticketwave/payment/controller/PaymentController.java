package com.ticketwave.payment.controller;

import com.ticketwave.payment.Payment;
import com.ticketwave.payment.dto.PaymentDto;
import com.ticketwave.payment.service.PaymentService;
import com.ticketwave.payment.service.PaymentMapper;
import com.ticketwave.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "Payment processing APIs")
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentMapper mapper;

    public PaymentController(PaymentService paymentService, PaymentMapper mapper) {
        this.paymentService = paymentService;
        this.mapper = mapper;
    }

    @PostMapping("/intent")
    @Operation(summary = "Create a payment intent")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentDto>> createIntent(@RequestParam Long bookingId,
                                                                 @RequestParam Double amount) {
        var p = paymentService.createPaymentIntent(bookingId, amount);
        return ResponseEntity.ok(new ApiResponse<>(true, "created", mapper.toDto(p)));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm a payment")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentDto>> confirm(@PathVariable Long id,
                                                            @RequestParam String ref) {
        var p = paymentService.confirmPayment(id, ref);
        return ResponseEntity.ok(new ApiResponse<>(true, "confirmed", mapper.toDto(p)));
    }
}
