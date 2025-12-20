package com.senalbum.payment;

import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.SubscriptionPlan;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payment_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne
  @JoinColumn(name = "photographer_id", nullable = false)
  private Photographer photographer;

  @Column(nullable = false)
  private Double amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "plan_target")
  private SubscriptionPlan planTarget; // The plan being purchased (PRO)

  @Column(name = "credits_quantity")
  private Integer creditsQuantity; // Number of credits purchased (e.g. 1)

  @Column(name = "transaction_token")
  private String transactionToken; // PayDunya token

  @Column(name = "payment_status")
  private String status; // PENDING, COMPLETED, FAILED

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
