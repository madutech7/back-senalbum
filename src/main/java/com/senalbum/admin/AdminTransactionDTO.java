package com.senalbum.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminTransactionDTO {
  private UUID id;
  private String userEmail;
  private String userName;
  private String type; // subscription, credits, refund
  private String planTarget;
  private Double amount;
  private String status; // PENDING, COMPLETED, FAILED
  private String paymentMethod;
  private LocalDateTime createdAt;
}
