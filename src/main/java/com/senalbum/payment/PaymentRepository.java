package com.senalbum.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentTransaction, UUID> {
  Optional<PaymentTransaction> findByTransactionToken(String transactionToken);

  // Admin statistics
  @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(p.amount), 0) FROM PaymentTransaction p WHERE p.status = 'COMPLETED' AND p.createdAt >= :startDate")
  Double getRevenueAfterDate(
      @org.springframework.data.repository.query.Param("startDate") java.time.LocalDateTime startDate);
}
