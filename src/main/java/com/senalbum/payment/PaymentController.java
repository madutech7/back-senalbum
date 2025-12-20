package com.senalbum.payment;

import com.senalbum.photographer.Photographer;
import com.senalbum.photographer.PhotographerRepository;
import com.senalbum.photographer.SubscriptionPlan;
import com.senalbum.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

  @Autowired
  private PayDunyaService payDunyaService;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private PhotographerRepository photographerRepository;

  @Autowired
  private SecurityUtils securityUtils;

  @PostMapping("/initiate/pro")
  public ResponseEntity<?> initiateProPayment() {
    UUID photographerId = securityUtils.getCurrentPhotographerId();
    if (photographerId == null) {
      throw new RuntimeException("Unable to get photographer ID");
    }
    Photographer photographer = photographerRepository.findById(photographerId)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));

    if (photographer.getSubscriptionPlan() == SubscriptionPlan.PRO) {
      return ResponseEntity.badRequest().body(Map.of("message", "Already verified PRO"));
    }

    // Create initial transaction record
    PaymentTransaction transaction = new PaymentTransaction();
    transaction.setPhotographer(photographer);
    transaction.setAmount(10000.0);
    transaction.setPlanTarget(SubscriptionPlan.PRO);
    transaction.setStatus("PENDING");

    transaction = paymentRepository.save(transaction); // Save to get ID if needed, though we use token

    // Call PayDunya
    String description = "Abonnement PRO SenAlbum - " + photographer.getEmail();
    try {
      String checkoutUrl = payDunyaService.initializePayment("SenAlbum PRO", 10000.0, description,
          transaction.getId().toString());

      // Sauvegarder la transaction avec l'URL
      paymentRepository.save(transaction);

      return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
    }
  }

  @PostMapping("/initiate/studio")
  public ResponseEntity<?> initiateStudioPayment() {
    UUID photographerId = securityUtils.getCurrentPhotographerId();
    Photographer photographer = photographerRepository.findById(photographerId)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));

    if (photographer.getSubscriptionPlan() == SubscriptionPlan.STUDIO) {
      return ResponseEntity.badRequest().body(Map.of("message", "Already verified STUDIO"));
    }

    // Create initial transaction record
    PaymentTransaction transaction = new PaymentTransaction();
    transaction.setPhotographer(photographer);
    transaction.setAmount(25000.0); // Prix pour Studio
    transaction.setPlanTarget(SubscriptionPlan.STUDIO);
    transaction.setStatus("PENDING");

    transaction = paymentRepository.save(transaction);

    // Call PayDunya
    String description = "Abonnement STUDIO SenAlbum - " + photographer.getEmail();
    try {
      String checkoutUrl = payDunyaService.initializePayment("SenAlbum STUDIO", 25000.0, description,
          transaction.getId().toString());

      paymentRepository.save(transaction);

      return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
    }

  }

  @PostMapping("/initiate/credits")
  public ResponseEntity<?> initiateCreditPayment() {
    UUID photographerId = securityUtils.getCurrentPhotographerId();
    Photographer photographer = photographerRepository.findById(photographerId)
        .orElseThrow(() -> new RuntimeException("Photographer not found"));

    // Create transaction for 1 credit
    PaymentTransaction transaction = new PaymentTransaction();
    transaction.setPhotographer(photographer);
    transaction.setAmount(1500.0);
    transaction.setCreditsQuantity(1);
    transaction.setStatus("PENDING");

    transaction = paymentRepository.save(transaction);

    // Call PayDunya
    String description = "Pack 1 Album Supplémentaire - " + photographer.getEmail();
    try {
      String checkoutUrl = payDunyaService.initializePayment("SenAlbum Credits", 1500.0, description,
          transaction.getId().toString());

      paymentRepository.save(transaction);

      return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    } catch (Exception e) {
      return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
    }
  }

  @PostMapping("/callback")
  public ResponseEntity<?> handleCallback(@RequestBody Map<String, Object> payload) {
    logger.info("Received PayDunya callback: {}", payload);

    try {
      // Extraire les données de la réponse PayDunya
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) payload.get("data");

      if (data == null) {
        logger.error("No data in callback payload");
        return ResponseEntity.ok("No data");
      }

      String status = (String) data.get("status");

      @SuppressWarnings("unchecked")
      Map<String, Object> invoice = (Map<String, Object>) data.get("invoice");
      String invoiceToken = invoice != null ? (String) invoice.get("token") : null;

      @SuppressWarnings("unchecked")
      Map<String, Object> customData = (Map<String, Object>) data.get("custom_data");
      String transactionId = customData != null ? (String) customData.get("transaction_id") : null;

      logger.info("Payment status: {}, token: {}, transactionId: {}", status, invoiceToken, transactionId);

      // Vérifier que le paiement est complété
      if ("completed".equalsIgnoreCase(status) && transactionId != null) {
        // Trouver la transaction
        Optional<PaymentTransaction> transactionOpt = paymentRepository.findById(UUID.fromString(transactionId));

        if (transactionOpt.isPresent()) {
          PaymentTransaction transaction = transactionOpt.get();

          // Mettre à jour le statut de la transaction
          transaction.setStatus("COMPLETED");
          transaction.setTransactionToken(invoiceToken);
          paymentRepository.save(transaction);

          // Recharger le photographe pour assurer la persistance
          UUID photographerId = transaction.getPhotographer().getId();
          Photographer photographer = photographerRepository.findById(photographerId)
              .orElseThrow(() -> new RuntimeException("Photographer not found " + photographerId));

          if (transaction.getCreditsQuantity() != null && transaction.getCreditsQuantity() > 0) {
            // C'est un achat de crédits
            int currentCredits = photographer.getExtraAlbumCredits() != null ? photographer.getExtraAlbumCredits() : 0;
            photographer.setExtraAlbumCredits(currentCredits + transaction.getCreditsQuantity());
            logger.info("Added {} credits to photographer {}", transaction.getCreditsQuantity(),
                photographer.getEmail());
          } else if (transaction.getPlanTarget() != null) {
            // C'est un abonnement
            photographer.setSubscriptionPlan(transaction.getPlanTarget());
            logger.info("Successfully upgraded photographer {} to plan {}",
                photographer.getEmail(), photographer.getSubscriptionPlan());
          }

          photographerRepository.save(photographer);

          return ResponseEntity.ok(Map.of("message", "Payment processed successfully"));
        } else {
          logger.error("Transaction not found: {}", transactionId);
        }
      } else {
        logger.warn("Payment not completed or missing transaction ID. Status: {}", status);
      }

      return ResponseEntity.ok(Map.of("message", "Callback received"));

    } catch (Exception e) {
      logger.error("Error processing callback", e);
      return ResponseEntity.ok(Map.of("message", "Error: " + e.getMessage()));
    }
  }

  // Endpoint pour vérifier manuellement le statut d'un paiement (utile pour le
  // return_url)
  @GetMapping("/verify/{transactionId}")
  public ResponseEntity<?> verifyPayment(@PathVariable String transactionId) {
    try {
      UUID photographerId = securityUtils.getCurrentPhotographerId();

      Optional<PaymentTransaction> transactionOpt = paymentRepository.findById(UUID.fromString(transactionId));

      if (transactionOpt.isPresent()) {
        PaymentTransaction transaction = transactionOpt.get();

        // Vérifier que la transaction appartient bien à l'utilisateur connecté
        if (!transaction.getPhotographer().getId().equals(photographerId)) {
          return ResponseEntity.status(403).body(Map.of("message", "Unauthorized"));
        }

        return ResponseEntity.ok(Map.of(
            "status", transaction.getStatus(),
            "plan", transaction.getPlanTarget() != null ? transaction.getPlanTarget() : "CREDITS",
            "amount", transaction.getAmount()));
      }

      return ResponseEntity.notFound().build();

    } catch (Exception e) {
      logger.error("Error verifying payment", e);
      return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
    }
  }

  // Endpoint pour simuler la complétion d'un paiement en mode test
  // (car PayDunya ne peut pas atteindre localhost pour le callback IPN)
  @PostMapping("/complete-test/{transactionId}")
  public ResponseEntity<?> completeTestPayment(@PathVariable String transactionId) {
    try {
      logger.info("Simulating payment completion for transaction: {}", transactionId);

      Optional<PaymentTransaction> transactionOpt = paymentRepository.findById(UUID.fromString(transactionId));

      if (transactionOpt.isPresent()) {
        PaymentTransaction transaction = transactionOpt.get();

        // Mettre à jour le statut de la transaction
        transaction.setStatus("COMPLETED");
        paymentRepository.save(transaction);

        // RECHARGER le photographe pour éviter les problèmes d'entité détachée ou de
        // cache
        UUID photographerId = transaction.getPhotographer().getId();
        Photographer photographer = photographerRepository.findById(photographerId)
            .orElseThrow(() -> new RuntimeException("Photographer not found " + photographerId));

        logger.info("Current plan before update: {}", photographer.getSubscriptionPlan());

        if (transaction.getCreditsQuantity() != null && transaction.getCreditsQuantity() > 0) {
          int currentCredits = photographer.getExtraAlbumCredits() != null ? photographer.getExtraAlbumCredits() : 0;
          photographer.setExtraAlbumCredits(currentCredits + transaction.getCreditsQuantity());
        } else if (transaction.getPlanTarget() != null) {
          photographer.setSubscriptionPlan(transaction.getPlanTarget());
        }

        photographerRepository.save(photographer);

        // Force flush if possible via repo or just trust save
        // Ideally we are in a transactional context

        logger.info("Successfully processed transaction for photographer {} (TEST MODE)", photographer.getEmail());

        return ResponseEntity.ok(Map.of(
            "message", "Payment completed successfully",
            "plan", transaction.getPlanTarget() != null ? transaction.getPlanTarget() : "CREDITS",
            "status", "COMPLETED"));
      }

      return ResponseEntity.notFound().build();

    } catch (Exception e) {
      logger.error("Error completing test payment", e);
      return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
    }
  }
}
