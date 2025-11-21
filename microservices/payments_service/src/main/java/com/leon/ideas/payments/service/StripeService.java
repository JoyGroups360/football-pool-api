package com.leon.ideas.payments.service;

import com.leon.ideas.payments.model.PaymentIntentRequest;
import com.leon.ideas.payments.model.PaymentIntentResponse;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;
import com.stripe.param.RefundCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class StripeService {

    @Value("${stripe.api.key.secret}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
        log.info("✅ Stripe initialized with API key: {}", stripeSecretKey.substring(0, 12) + "***");
    }

    /**
     * Create a PaymentIntent
     * @param request PaymentIntentRequest with payment details
     * @return PaymentIntentResponse with client secret and payment details
     */
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) throws StripeException {
        log.info("Creating PaymentIntent: amount={}, currency={}, userId={}", 
                request.getAmount(), request.getCurrency(), request.getUserId());

        // Convert amount to cents (Stripe uses smallest currency unit)
        long amountInCents = Math.round(request.getAmount() * 100);

        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency(request.getCurrency().toLowerCase())
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );

        // Add metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("userId", request.getUserId());
        if (request.getGroupId() != null && !request.getGroupId().isEmpty()) {
            metadata.put("groupId", request.getGroupId());
        }
        if (request.getMetadata() != null) {
            request.getMetadata().forEach((key, value) -> 
                metadata.put(key, value != null ? value.toString() : "")
            );
        }
        if (!metadata.isEmpty()) {
            paramsBuilder.putAllMetadata(metadata);
        }

        // Set description
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            paramsBuilder.setDescription(request.getDescription());
        }

        // Set receipt email
        if (request.getReceiptEmail() != null && !request.getReceiptEmail().isEmpty()) {
            paramsBuilder.setReceiptEmail(request.getReceiptEmail());
        }

        PaymentIntentCreateParams params = paramsBuilder.build();
        PaymentIntent paymentIntent = PaymentIntent.create(params);

        log.info("PaymentIntent created: id={}, status={}", 
                paymentIntent.getId(), paymentIntent.getStatus());

        return mapToPaymentIntentResponse(paymentIntent, request.getUserId(), request.getGroupId());
    }

    /**
     * Retrieve a PaymentIntent by ID
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Retrieving PaymentIntent: id={}", paymentIntentId);
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Confirm a PaymentIntent
     */
    public PaymentIntent confirmPaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Confirming PaymentIntent: id={}", paymentIntentId);
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return paymentIntent.confirm();
    }

    /**
     * Cancel a PaymentIntent
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Canceling PaymentIntent: id={}", paymentIntentId);
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return paymentIntent.cancel();
    }

    /**
     * Create a refund
     * @param paymentIntentId PaymentIntent ID
     * @param amount Amount to refund (null for full refund)
     * @param reason Refund reason
     * @return Refund object
     */
    public Refund createRefund(String paymentIntentId, Double amount, String reason) throws StripeException {
        log.info("Creating refund: paymentIntentId={}, amount={}, reason={}", 
                paymentIntentId, amount, reason);

        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        
        RefundCreateParams.Builder paramsBuilder = RefundCreateParams.builder()
                .setPaymentIntent(paymentIntentId);

        if (amount != null && amount > 0) {
            // Convert to cents
            long amountInCents = Math.round(amount * 100);
            paramsBuilder.setAmount(amountInCents);
        }

        if (reason != null && !reason.isEmpty()) {
            RefundCreateParams.Reason refundReason;
            try {
                refundReason = RefundCreateParams.Reason.valueOf(reason.toUpperCase());
                paramsBuilder.setReason(refundReason);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid refund reason: {}, using REQUESTED_BY_CUSTOMER", reason);
                paramsBuilder.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER);
            }
        }

        Refund refund = Refund.create(paramsBuilder.build());
        log.info("Refund created: id={}, amount={}", refund.getId(), refund.getAmount() / 100.0);
        return refund;
    }

    /**
     * Map Stripe PaymentIntent to PaymentIntentResponse
     */
    private PaymentIntentResponse mapToPaymentIntentResponse(
            PaymentIntent paymentIntent, String userId, String groupId) {
        PaymentIntentResponse response = new PaymentIntentResponse();
        response.setPaymentIntentId(paymentIntent.getId());
        response.setClientSecret(paymentIntent.getClientSecret());
        response.setStatus(paymentIntent.getStatus());
        response.setAmount(paymentIntent.getAmount() / 100.0); // Convert from cents
        response.setCurrency(paymentIntent.getCurrency().toUpperCase());
        response.setUserId(userId);
        response.setGroupId(groupId);
        response.setDescription(paymentIntent.getDescription());
        response.setReceiptEmail(paymentIntent.getReceiptEmail());
        
        // Check if payment requires additional action
        response.setRequiresAction(paymentIntent.getStatus().equals("requires_action") ||
                                   paymentIntent.getStatus().equals("requires_payment_method"));
        
        if (paymentIntent.getNextAction() != null) {
            response.setNextActionType(paymentIntent.getNextAction().getType());
            // Convert NextAction to Map manually since toMap() doesn't exist
            Map<String, Object> nextActionMap = new HashMap<>();
            nextActionMap.put("type", paymentIntent.getNextAction().getType());
            // Add other fields if needed
            response.setNextAction(nextActionMap);
        }

        return response;
    }

    /**
     * Map Stripe PaymentIntent status to our Payment status
     */
    public static com.leon.ideas.payments.model.Payment.PaymentStatus mapStripeStatusToPaymentStatus(String stripeStatus) {
        return switch (stripeStatus.toLowerCase()) {
            case "requires_payment_method", "requires_confirmation" -> 
                com.leon.ideas.payments.model.Payment.PaymentStatus.PENDING;
            case "processing", "requires_action" -> 
                com.leon.ideas.payments.model.Payment.PaymentStatus.PROCESSING;
            case "succeeded" -> 
                com.leon.ideas.payments.model.Payment.PaymentStatus.SUCCEEDED;
            case "canceled" -> 
                com.leon.ideas.payments.model.Payment.PaymentStatus.CANCELED;
            default -> 
                com.leon.ideas.payments.model.Payment.PaymentStatus.FAILED;
        };
    }
}

