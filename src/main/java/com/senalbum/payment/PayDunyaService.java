package com.senalbum.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class PayDunyaService {

  @Value("${paydunya.master-key}")
  private String masterKey;

  @Value("${paydunya.public-key}")
  private String publicKey;

  @Value("${paydunya.private-key}")
  private String privateKey;

  @Value("${paydunya.token}")
  private String token;

  @Value("${paydunya.mode}") // "test" or "live"
  private String mode;

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PayDunyaService.class);
  private final RestTemplate restTemplate = new RestTemplate();

  public String initializePayment(String itemName, Double totalAmount, String description, String transactionId) {
    String url = "live".equals(mode)
        ? "https://app.paydunya.com/api/v1/checkout-invoice/create"
        : "https://app.paydunya.com/sandbox-api/v1/checkout-invoice/create";

    logger.info("Initializing PayDunya payment at URL: {}", url);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("PAYDUNYA-MASTER-KEY", masterKey);
    headers.set("PAYDUNYA-PUBLIC-KEY", publicKey);
    headers.set("PAYDUNYA-PRIVATE-KEY", privateKey);
    headers.set("PAYDUNYA-TOKEN", this.token);

    Map<String, Object> invoice = new HashMap<>();

    // Store info
    Map<String, Object> store = new HashMap<>();
    store.put("name", "SenAlbum");
    invoice.put("store", store);

    // Invoice data
    Map<String, Object> invoiceData = new HashMap<>();
    invoiceData.put("total_amount", totalAmount);
    invoiceData.put("description", description);
    invoice.put("invoice", invoiceData);

    // Actions (URLs de retour)
    Map<String, String> actions = new HashMap<>();
    actions.put("cancel_url", "http://localhost:4200/dashboard?status=cancelled&txn=" + transactionId);
    actions.put("return_url", "http://localhost:4200/dashboard?status=success&txn=" + transactionId);
    actions.put("callback_url", "http://localhost:8080/api/payments/callback");
    invoice.put("actions", actions);

    // Custom data pour lier à notre transaction
    Map<String, String> customData = new HashMap<>();
    customData.put("transaction_id", transactionId);
    invoice.put("custom_data", customData);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(invoice, headers);

    try {
      ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        logger.info("PayDunya Response: {}", body);

        String responseCode = (String) body.get("response_code");
        if ("00".equals(responseCode)) {
          // PayDunya retourne l'URL complète dans response_text
          String invoiceUrl = (String) body.get("response_text");
          logger.info("Invoice URL: {}", invoiceUrl);
          return invoiceUrl;
        } else {
          String errorMsg = (String) body.get("response_text");
          logger.error("PayDunya error: {}", errorMsg);
          throw new RuntimeException("PayDunya returned error: " + errorMsg);
        }
      }
    } catch (Exception e) {
      logger.error("PayDunya request failed", e);
      throw new RuntimeException("PayDunya Error: " + e.getMessage());
    }

    throw new RuntimeException("PayDunya initialization failed: Invalid response");
  }

  public boolean verifyPayment(String invoiceToken) {
    // Implementation to confirm status from PayDunya if needed
    return true; // Simplification for now, rely on IPN
  }
}
