package com.leon.ideas.payments.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String id;
    private String userId;
    private String groupId;
    private String paymentIntentId;
    private Double amount;
    private String currency;
    private String status;
    private String statusMessage;
    private String description;
    private String receiptEmail;
    private String receiptUrl;
    private Date createdAt;
    private Date updatedAt;
    private Date paidAt;
    private Boolean refunded;
    private Double refundAmount;
    private Date refundedAt;
    private String refundReason;
    private Map<String, String> metadata;
}

