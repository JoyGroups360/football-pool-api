package com.leon.ideas.payments.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {
    private String paymentIntentId;
    private String clientSecret;
    private String status;
    private Double amount;
    private String currency;
    private String userId;
    private String groupId;
    private String description;
    private String receiptEmail;
    private Boolean requiresAction;
    private String nextActionType;
    private Map<String, Object> nextAction;
}

