package com.leon.ideas.payments.controller;

import com.leon.ideas.payments.model.*;
import com.leon.ideas.payments.service.JwtService;
import com.leon.ideas.payments.service.PaymentsService;
import com.stripe.exception.StripeException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/football-pool/v1/api/payments")
public class PaymentsController {

    @Autowired
    private PaymentsService paymentsService;

    @Autowired
    private JwtService jwtService;

    /**
     * Create a payment intent
     * POST /payments/intent
     */
    @PostMapping("/intent")
    public ResponseEntity<?> createPaymentIntent(
            @Valid @RequestBody PaymentIntentRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            // Extract userId from JWT token
            String userId = extractUserIdFromAuth(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized", "Valid JWT token required"));
            }

            // Set userId from token (security: prevent user from making payments as another user)
            request.setUserId(userId);

            PaymentIntentResponse response = paymentsService.createPaymentIntent(request);
            return ResponseEntity.ok(createSuccessResponse("Payment intent created successfully", response));
        } catch (StripeException e) {
            log.error("Stripe error creating payment intent", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Payment intent creation failed", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating payment intent", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Get payment by ID
     * GET /payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<?> getPayment(
            @PathVariable String paymentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            String userId = extractUserIdFromAuth(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized", "Valid JWT token required"));
            }

            PaymentResponse payment = paymentsService.getPaymentById(paymentId);
            
            // Security: Only allow users to view their own payments
            if (!payment.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Forbidden", "You can only view your own payments"));
            }

            return ResponseEntity.ok(createSuccessResponse("Payment retrieved successfully", payment));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Payment not found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Get payment by PaymentIntent ID
     * GET /payments/intent/{paymentIntentId}
     */
    @GetMapping("/intent/{paymentIntentId}")
    public ResponseEntity<?> getPaymentByIntentId(
            @PathVariable String paymentIntentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            String userId = extractUserIdFromAuth(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized", "Valid JWT token required"));
            }

            PaymentResponse payment = paymentsService.getPaymentByPaymentIntentId(paymentIntentId);
            
            if (!payment.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Forbidden", "You can only view your own payments"));
            }

            return ResponseEntity.ok(createSuccessResponse("Payment retrieved successfully", payment));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Payment not found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrieving payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Get all payments for the authenticated user
     * GET /payments/user
     */
    @GetMapping("/user")
    public ResponseEntity<?> getUserPayments(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            String userId = extractUserIdFromAuth(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized", "Valid JWT token required"));
            }

            List<PaymentResponse> payments = paymentsService.getUserPayments(userId);
            return ResponseEntity.ok(createSuccessResponse("User payments retrieved successfully", payments));
        } catch (Exception e) {
            log.error("Error retrieving user payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Get all payments for a group
     * GET /payments/group/{groupId}
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<?> getGroupPayments(
            @PathVariable String groupId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            String userId = extractUserIdFromAuth(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized", "Valid JWT token required"));
            }

            List<PaymentResponse> payments = paymentsService.getGroupPayments(groupId);
            return ResponseEntity.ok(createSuccessResponse("Group payments retrieved successfully", payments));
        } catch (Exception e) {
            log.error("Error retrieving group payments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Update payment status (for webhooks or polling)
     * POST /payments/{paymentIntentId}/status
     */
    @PostMapping("/{paymentIntentId}/status")
    public ResponseEntity<?> updatePaymentStatus(@PathVariable String paymentIntentId) {
        try {
            PaymentResponse payment = paymentsService.updatePaymentStatus(paymentIntentId);
            return ResponseEntity.ok(createSuccessResponse("Payment status updated successfully", payment));
        } catch (StripeException e) {
            log.error("Stripe error updating payment status", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Failed to update payment status", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Payment not found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating payment status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Confirm a payment intent
     * POST /payments/{paymentIntentId}/confirm
     */
    @PostMapping("/{paymentIntentId}/confirm")
    public ResponseEntity<?> confirmPayment(
            @PathVariable String paymentIntentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            String userId = extractUserIdFromAuth(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized", "Valid JWT token required"));
            }

            PaymentResponse payment = paymentsService.confirmPaymentIntent(paymentIntentId);
            
            if (!payment.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Forbidden", "You can only confirm your own payments"));
            }

            return ResponseEntity.ok(createSuccessResponse("Payment confirmed successfully", payment));
        } catch (StripeException e) {
            log.error("Stripe error confirming payment", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Failed to confirm payment", e.getMessage()));
        } catch (Exception e) {
            log.error("Error confirming payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Cancel a payment
     * POST /payments/{paymentIntentId}/cancel
     */
    @PostMapping("/{paymentIntentId}/cancel")
    public ResponseEntity<?> cancelPayment(
            @PathVariable String paymentIntentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            String userId = extractUserIdFromAuth(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized", "Valid JWT token required"));
            }

            PaymentResponse payment = paymentsService.cancelPayment(paymentIntentId);
            
            if (!payment.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Forbidden", "You can only cancel your own payments"));
            }

            return ResponseEntity.ok(createSuccessResponse("Payment canceled successfully", payment));
        } catch (StripeException e) {
            log.error("Stripe error canceling payment", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Failed to cancel payment", e.getMessage()));
        } catch (Exception e) {
            log.error("Error canceling payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Create a refund
     * POST /payments/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<?> createRefund(
            @Valid @RequestBody RefundRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        
        try {
            String userId = extractUserIdFromAuth(authHeader);
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized", "Valid JWT token required"));
            }

            PaymentResponse payment = paymentsService.createRefund(request);
            
            // Security: Only allow refunds for user's own payments (or admin check can be added)
            if (!payment.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Forbidden", "You can only refund your own payments"));
            }

            return ResponseEntity.ok(createSuccessResponse("Refund created successfully", payment));
        } catch (StripeException e) {
            log.error("Stripe error creating refund", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("Failed to create refund", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Payment not found", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Internal server error", e.getMessage()));
        }
    }

    /**
     * Health check endpoint
     * GET /payments/health
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "payments_service");
        return ResponseEntity.ok(response);
    }

    /**
     * Extract user ID from Authorization header
     */
    private String extractUserIdFromAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        try {
            String token = authHeader.substring(7);
            if (jwtService.validateToken(token)) {
                return jwtService.getUserIdFromToken(token);
            }
        } catch (Exception e) {
            log.warn("Failed to extract userId from token", e);
        }
        return null;
    }

    /**
     * Create success response
     */
    private Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    /**
     * Create error response
     */
    private Map<String, Object> createErrorResponse(String error, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", error);
        response.put("message", message);
        return response;
    }
}

