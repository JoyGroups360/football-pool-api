package com.leon.ideas.payments.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequest {
    @NotBlank(message = "Payment ID is required")
    private String paymentId;

    @Positive(message = "Amount must be positive")
    private Double amount; // Optional: if not provided, full refund

    private String reason; // duplicate, fraudulent, requested_by_customer

    private String refundApplicationFee; // boolean as string
    private String reverseTransfer; // boolean as string
}

