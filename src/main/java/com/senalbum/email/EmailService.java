package com.senalbum.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class EmailService {

  private static final Logger logger = Logger.getLogger(EmailService.class.getName());

  @Autowired
  private JavaMailSender mailSender;

  @Value("${spring.mail.from}")
  private String fromEmail;

  @Value("${app.frontend.url}")
  private String frontendUrl;

  private static final String PRIMARY_COLOR = "#000000"; // Sleek Black
  private static final String ACCENT_COLOR = "#6366f1"; // Indigo
  private static final String BG_COLOR = "#f9fafb"; // Light Gray background

  @Async
  public void sendVerificationEmail(String to, String code) {
    logger.info("=================================================");
    logger.info("CONFIRMATION CODE FOR " + to + " : " + code);
    logger.info("=================================================");

    String subject = "Activez votre compte SenAlbum";
    String content = getEmailTemplate(
        "Bienvenue sur SenAlbum",
        "Merci de nous rejoindre. Votre aventure commence ici. Pour valider votre inscription et accéder à votre studio, veuillez utiliser le code de confirmation ci-dessous :",
        code,
        "Ce code est valable pendant 5 minutes. Si vous n'avez pas créé de compte, vous pouvez ignorer cet e-mail.");

    sendHtmlEmail(to, subject, content);
  }

  @Async
  public void sendPasswordResetEmail(String to, String code) {
    logger.info("=================================================");
    logger.info("PASSWORD RESET CODE FOR " + to + " : " + code);
    logger.info("=================================================");

    String subject = "Réinitialisez votre mot de passe - SenAlbum";
    String content = getEmailTemplate(
        "Récupération de compte",
        "Nous avons reçu une demande de réinitialisation de mot de passe pour votre compte. Utilisez le code de sécurité suivant pour procéder au changement :",
        code,
        "Ce code expirera dans 15 minutes. Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet e-mail.");

    sendHtmlEmail(to, subject, content);
  }

  private void sendHtmlEmail(String to, String subject, String htmlContent) {
    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(htmlContent, true);

      mailSender.send(message);
    } catch (MessagingException e) {
      logger.severe("Echec de l'envoi de l'e-mail à " + to + ": " + e.getMessage());
    }
  }

  private String getEmailTemplate(String title, String message, String code, String footerNote) {
    String logoUrl = frontendUrl + "/assets/logo.png";
    return "<!DOCTYPE html>"
        + "<html>"
        + "<head>"
        + "<meta charset='UTF-8'>"
        + "<meta name='viewport' content='width=device-width, initial-scale=1.0'>"
        + "</head>"
        + "<body style='margin: 0; padding: 0; font-family: \"Helvetica Neue\", Helvetica, Arial, sans-serif; background-color: "
        + BG_COLOR + "; color: #1f2937; line-height: 1.6;'>"
        + "  <table width='100%' border='0' cellspacing='0' cellpadding='0' style='padding: 40px 20px;'>"
        + "    <tr>"
        + "      <td align='center'>"
        + "        <table width='100%' border='0' cellspacing='0' cellpadding='0' style='max-width: 600px; background-color: #ffffff; border-radius: 24px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);'>"
        + "          <!-- Header -->"
        + "          <tr>"
        + "            <td style='padding: 40px 40px 30px 40px; text-align: center;'>"
        + "              <img src='" + logoUrl
        + "' alt='SenAlbum' style='height: 40px; width: auto; margin-bottom: 10px;'>"
        + "            </td>"
        + "          </tr>"
        + "          <!-- Content -->"
        + "          <tr>"
        + "            <td style='padding: 0 40px 40px 40px;'>"
        + "              <h2 style='margin: 0 0 20px 0; font-size: 20px; font-weight: 600; color: #111827;'>" + title
        + "</h2>"
        + "              <p style='margin: 0 0 30px 0; font-size: 16px; color: #4b5563;'>" + message + "</p>"
        + "              "
        + "              <div style='background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%); padding: 35px; border-radius: 20px; text-align: center; margin-bottom: 30px;'>"
        + "                <span style='font-size: 32px; font-weight: 800; letter-spacing: 12px; color: "
        + PRIMARY_COLOR + "; font-family: monospace;'>" + code + "</span>"
        + "              </div>"
        + "              "
        + "              <p style='margin: 0; font-size: 14px; text-align: center; color: #9ca3af; font-style: italic;'>"
        + footerNote + "</p>"
        + "            </td>"
        + "          </tr>"
        + "          <!-- Support CTA -->"
        + "          <tr>"
        + "            <td style='padding: 0 40px 40px 40px; border-top: 1px solid #f3f4f6;'>"
        + "              <table width='100%' border='0' cellspacing='0' cellpadding='0' style='margin-top: 30px; font-size: 13px; color: #6b7280;'>"
        + "                <tr>"
        + "                  <td>Besoin d'aide ? <a href='#' style='color: " + ACCENT_COLOR
        + "; text-decoration: none; font-weight: 500;'>Contactez notre support</a></td>"
        + "                </tr>"
        + "              </table>"
        + "            </td>"
        + "          </tr>"
        + "        </table>"
        + "        <!-- Footer -->"
        + "        <table width='100%' border='0' cellspacing='0' cellpadding='0' style='max-width: 600px; margin-top: 30px;'>"
        + "          <tr>"
        + "            <td align='center' style='font-size: 12px; color: #9ca3af;'>"
        + "              <p style='margin: 0 0 10px 0;'>&copy; 2024 SenAlbum. La plateforme des photographes d'exception.</p>"
        + "              <div style='margin-top: 10px;'>"
        + "                <a href='#' style='color: #6b7280; text-decoration: none; margin: 0 10px;'>Confidentialité</a>"
        + "                <a href='#' style='color: #6b7280; text-decoration: none; margin: 0 10px;'>Conditions</a>"
        + "              </div>"
        + "            </td>"
        + "          </tr>"
        + "        </table>"
        + "      </td>"
        + "    </tr>"
        + "  </table>"
        + "</body>"
        + "</html>";
  }
}
