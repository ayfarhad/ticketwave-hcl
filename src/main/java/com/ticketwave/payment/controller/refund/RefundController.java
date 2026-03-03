package com.ticketwave.payment.controller.refund;

import com.ticketwave.common.ApiResponse;
import com.ticketwave.payment.Refund;
import com.ticketwave.payment.service.refund.RefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/refunds")
public class RefundController {
    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/create")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Refund>> create(@RequestParam Long paymentId,
                                                    @RequestParam Double amount,
                                                    @RequestParam(required = false) String reason) {
        Refund r = refundService.createRefund(paymentId, amount, reason);
        return ResponseEntity.ok(new ApiResponse<>(true, "created", r));
    }
}
