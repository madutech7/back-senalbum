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

  @Async
  public void sendVerificationEmail(String to, String code) {
    // Log the code for easy development access (visible in Railway logs or
    // terminal)
    logger.info("=================================================");
    logger.info("CONFIRMATION CODE FOR " + to + " : " + code);
    logger.info("=================================================");

    String subject = "Code de confirmation - SenAlbum";
    String content = "<html>"
        + "<body style=\"font-family: Arial, sans-serif; color: #333;\">"
        + "<div style=\"max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;\">"
        + "<h2 style=\"color: #6366f1; text-align: center;\">Bienvenue sur SenAlbum !</h2>"
        + "<p>Merci de vous être inscrit. Pour finaliser votre inscription, veuillez utiliser le code de confirmation suivant :</p>"
        + "<div style=\"background-color: #f3f4f6; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0;\">"
        + "<span style=\"font-size: 32px; font-weight: bold; letter-spacing: 5px; color: #1f2937;\">" + code + "</span>"
        + "</div>"
        + "<p>Ce code expirera dans 15 minutes.</p>"
        + "<p>Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet e-mail.</p>"
        + "<hr style=\"border: 0; border-top: 1px solid #eee; margin: 20px 0;\">"
        + "<p style=\"font-size: 12px; color: #9ca3af; text-align: center;\">© 2024 SenAlbum. Tous droits réservés.</p>"
        + "</div>"
        + "</body>"
        + "</html>";

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(content, true);

      mailSender.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException("Echec de l'envoi de l'e-mail de confirmation", e);
    }
  }
}
