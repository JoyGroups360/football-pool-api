package com.leon.ideas.payments.repository;

import com.leon.ideas.payments.model.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Optional<Payment> findByPaymentIntentId(String paymentIntentId);
    List<Payment> findByUserId(String userId);
    List<Payment> findByGroupId(String groupId);
    List<Payment> findByUserIdAndStatus(String userId, Payment.PaymentStatus status);
    List<Payment> findByGroupIdAndStatus(String groupId, Payment.PaymentStatus status);
}

