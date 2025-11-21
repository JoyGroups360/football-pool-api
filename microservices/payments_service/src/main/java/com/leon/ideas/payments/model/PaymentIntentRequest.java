package com.leon.ideas.payments.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentRequest {
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount; // Amount in dollars (e.g., 10.50)

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^(usd|eur|mxn|cad|gbp)$", message = "Currency must be one of: usd, eur, mxn, cad, gbp")
    private String currency; // Currency code (lowercase)

    @NotBlank(message = "User ID is required")
    private String userId;

    private String groupId; // Optional: for group payments

    private String description;

    private String receiptEmail;

    // Metadata for additional information
    private Map<String, String> metadata;

    // Payment method configuration
    private Boolean automaticPaymentMethods;
    private String paymentMethodType; // card, us_bank_account, etc.
}

