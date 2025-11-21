package com.leon.ideas.payments.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    private String id;
    
    private String userId;
    private String groupId;
    private String paymentIntentId; // Stripe PaymentIntent ID
    
    private Double amount; // Amount in dollars
    private String currency; // e.g., "usd", "eur", "mxn"
    
    private PaymentStatus status; // PENDING, PROCESSING, SUCCEEDED, FAILED, CANCELED, REFUNDED
    private String statusMessage;
    
    private String stripeCustomerId;
    private String paymentMethodId;
    
    private String description;
    private String receiptEmail;
    private String receiptUrl;
    
    private Date createdAt;
    private Date updatedAt;
    private Date paidAt;
    
    // Refund information
    private Boolean refunded;
    private Double refundAmount;
    private Date refundedAt;
    private String refundReason;
    
    // Metadata
    private java.util.Map<String, String> metadata;

    public enum PaymentStatus {
        PENDING,
        PROCESSING,
        SUCCEEDED,
        FAILED,
        CANCELED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
}

