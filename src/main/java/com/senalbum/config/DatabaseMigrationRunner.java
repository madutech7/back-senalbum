package com.senalbum.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class DatabaseMigrationRunner implements CommandLineRunner {

  private static final Logger logger = Logger.getLogger(DatabaseMigrationRunner.class.getName());

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Override
  public void run(String... args) throws Exception {
    logger.info("Vérification et migration automatique de la base de données...");
    try {
      // Ajouter la colonne enabled si elle n'existe pas
      jdbcTemplate.execute("ALTER TABLE photographers ADD COLUMN IF NOT EXISTS enabled BOOLEAN DEFAULT FALSE NOT NULL");

      // Ajouter la colonne verification_code si elle n'existe pas
      jdbcTemplate.execute("ALTER TABLE photographers ADD COLUMN IF NOT EXISTS verification_code VARCHAR(255)");

      // Ajouter la colonne verification_code_expires_at si elle n'existe pas
      jdbcTemplate.execute("ALTER TABLE photographers ADD COLUMN IF NOT EXISTS verification_code_expires_at TIMESTAMP");

      // Ajouter les colonnes de réinitialisation de mot de passe
      jdbcTemplate.execute("ALTER TABLE photographers ADD COLUMN IF NOT EXISTS reset_password_code VARCHAR(255)");
      jdbcTemplate
          .execute("ALTER TABLE photographers ADD COLUMN IF NOT EXISTS reset_password_code_expires_at TIMESTAMP");

      logger.info("Migration de la base de données terminée avec succès.");
    } catch (Exception e) {
      logger.warning("Erreur lors de la migration automatique : " + e.getMessage());
      // On ne bloque pas le démarrage, Hibernate essaiera de son côté
    }
  }
}
