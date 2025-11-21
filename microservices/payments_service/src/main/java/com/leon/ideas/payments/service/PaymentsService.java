package com.leon.ideas.payments.service;

import com.leon.ideas.payments.model.*;
import com.leon.ideas.payments.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class PaymentsService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private StripeService stripeService;

    /**
     * Create a payment intent
     */
    public PaymentIntentResponse createPaymentIntent(PaymentIntentRequest request) throws StripeException {
        log.info("Creating payment intent for user: {}", request.getUserId());

        // Create PaymentIntent in Stripe
        PaymentIntentResponse response = stripeService.createPaymentIntent(request);

        // Save payment record in database
        Payment payment = new Payment();
        payment.setUserId(request.getUserId());
        payment.setGroupId(request.getGroupId());
        payment.setPaymentIntentId(response.getPaymentIntentId());
        payment.setAmount(response.getAmount());
        payment.setCurrency(response.getCurrency().toLowerCase());
        payment.setStatus(StripeService.mapStripeStatusToPaymentStatus(response.getStatus()));
        payment.setDescription(request.getDescription());
        payment.setReceiptEmail(request.getReceiptEmail());
        payment.setMetadata(request.getMetadata());
        payment.setCreatedAt(new Date());
        payment.setUpdatedAt(new Date());
        payment.setRefunded(false);

        paymentRepository.save(payment);
        log.info("Payment record saved: id={}", payment.getId());

        return response;
    }

    /**
     * Get payment by ID
     */
    public PaymentResponse getPaymentById(String paymentId) {
        Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found: " + paymentId);
        }
        return mapToPaymentResponse(paymentOpt.get());
    }

    /**
     * Get payment by PaymentIntent ID
     */
    public PaymentResponse getPaymentByPaymentIntentId(String paymentIntentId) {
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentIntentId(paymentIntentId);
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found for PaymentIntent: " + paymentIntentId);
        }
        return mapToPaymentResponse(paymentOpt.get());
    }

    /**
     * Get all payments for a user
     */
    public List<PaymentResponse> getUserPayments(String userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all payments for a group
     */
    public List<PaymentResponse> getGroupPayments(String groupId) {
        List<Payment> payments = paymentRepository.findByGroupId(groupId);
        return payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update payment status from webhook or polling
     */
    public PaymentResponse updatePaymentStatus(String paymentIntentId) throws StripeException {
        log.info("Updating payment status: paymentIntentId={}", paymentIntentId);

        // Retrieve from Stripe
        PaymentIntent paymentIntent = stripeService.retrievePaymentIntent(paymentIntentId);

        // Find payment in database
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentIntentId(paymentIntentId);
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found: " + paymentIntentId);
        }

        Payment payment = paymentOpt.get();
        Payment.PaymentStatus newStatus = StripeService.mapStripeStatusToPaymentStatus(paymentIntent.getStatus());
        
        payment.setStatus(newStatus);
        payment.setStatusMessage(paymentIntent.getStatus());
        payment.setUpdatedAt(new Date());

        if (paymentIntent.getStatus().equals("succeeded") && payment.getPaidAt() == null) {
            payment.setPaidAt(new Date());
        }

        // Update receipt URL if available (Stripe API changed - using latest_charge field)
        try {
            if (paymentIntent.getLatestCharge() != null && !paymentIntent.getLatestCharge().toString().isEmpty()) {
                // Receipt URL is typically available in the Charge object, 
                // but for simplicity, we'll leave it null for now
                // You can retrieve the charge separately if needed
            }
        } catch (Exception e) {
            log.debug("Could not retrieve receipt URL: {}", e.getMessage());
        }

        paymentRepository.save(payment);
        log.info("Payment status updated: id={}, status={}", payment.getId(), newStatus);

        return mapToPaymentResponse(payment);
    }

    /**
     * Confirm a payment intent
     */
    public PaymentResponse confirmPaymentIntent(String paymentIntentId) throws StripeException {
        log.info("Confirming payment intent: id={}", paymentIntentId);
        stripeService.confirmPaymentIntent(paymentIntentId);
        return updatePaymentStatus(paymentIntentId);
    }

    /**
     * Cancel a payment intent
     */
    public PaymentResponse cancelPayment(String paymentIntentId) throws StripeException {
        log.info("Canceling payment: paymentIntentId={}", paymentIntentId);
        stripeService.cancelPaymentIntent(paymentIntentId);
        return updatePaymentStatus(paymentIntentId);
    }

    /**
     * Create a refund
     */
    public PaymentResponse createRefund(RefundRequest request) throws StripeException {
        log.info("Creating refund: paymentId={}, amount={}", request.getPaymentId(), request.getAmount());

        // Find payment
        Optional<Payment> paymentOpt = paymentRepository.findById(request.getPaymentId());
        if (paymentOpt.isEmpty()) {
            throw new RuntimeException("Payment not found: " + request.getPaymentId());
        }

        Payment payment = paymentOpt.get();
        if (payment.getPaymentIntentId() == null) {
            throw new RuntimeException("PaymentIntent ID not found for payment: " + request.getPaymentId());
        }

        // Create refund in Stripe
        Refund refund = stripeService.createRefund(
                payment.getPaymentIntentId(),
                request.getAmount(),
                request.getReason()
        );

        // Update payment record
        payment.setRefunded(true);
        if (request.getAmount() != null && request.getAmount() > 0) {
            payment.setRefundAmount(request.getAmount());
            if (request.getAmount() < payment.getAmount()) {
                payment.setStatus(Payment.PaymentStatus.PARTIALLY_REFUNDED);
            } else {
                payment.setStatus(Payment.PaymentStatus.REFUNDED);
            }
        } else {
            payment.setRefundAmount(payment.getAmount());
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
        }
        payment.setRefundedAt(new Date());
        payment.setRefundReason(request.getReason());
        payment.setUpdatedAt(new Date());

        paymentRepository.save(payment);
        log.info("Refund completed: paymentId={}, refundAmount={}", payment.getId(), payment.getRefundAmount());

        return mapToPaymentResponse(payment);
    }

    /**
     * Map Payment entity to PaymentResponse DTO
     */
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setUserId(payment.getUserId());
        response.setGroupId(payment.getGroupId());
        response.setPaymentIntentId(payment.getPaymentIntentId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setStatus(payment.getStatus().name());
        response.setStatusMessage(payment.getStatusMessage());
        response.setDescription(payment.getDescription());
        response.setReceiptEmail(payment.getReceiptEmail());
        response.setReceiptUrl(payment.getReceiptUrl());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        response.setPaidAt(payment.getPaidAt());
        response.setRefunded(payment.getRefunded());
        response.setRefundAmount(payment.getRefundAmount());
        response.setRefundedAt(payment.getRefundedAt());
        response.setRefundReason(payment.getRefundReason());
        response.setMetadata(payment.getMetadata());
        return response;
    }
}

