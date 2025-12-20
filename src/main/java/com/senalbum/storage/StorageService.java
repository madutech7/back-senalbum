package com.senalbum.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Interface abstraite pour le stockage des fichiers
 * Permet de basculer facilement vers S3 plus tard
 */
public interface StorageService {
    /**
     * Sauvegarde un fichier original (HD)
     * 
     * @param file    Le fichier à sauvegarder
     * @param albumId L'ID de l'album
     * @return Le chemin relatif du fichier sauvegardé
     */
    String saveOriginal(MultipartFile file, String albumId) throws IOException;

    /**
     * Sauvegarde une version preview compressée
     * 
     * @param file    Le fichier à sauvegarder
     * @param albumId L'ID de l'album
     * @return Le chemin relatif du fichier sauvegardé
     */
    String savePreview(MultipartFile file, String albumId, boolean watermarkEnabled, String watermarkText)
            throws IOException;

    /**
     * Récupère le fichier original
     * 
     * @param path Le chemin du fichier
     * @return Les bytes du fichier
     */
    byte[] getOriginalFile(String path) throws IOException;

    /**
     * Récupère le fichier preview
     * 
     * @param path Le chemin du fichier
     * @return Les bytes du fichier
     */
    byte[] getPreviewFile(String path) throws IOException;

    /**
     * Supprime un fichier
     * 
     * @param path Le chemin du fichier
     */
    void deleteFile(String path) throws IOException;

    /**
     * Génère une URL pré-signée pour l'upload direct (PUT)
     * 
     * @param objectKey   La clé (chemin) où le fichier sera stocké
     * @param contentType Le type MIME du fichier
     * @return L'URL pré-signée
     */
    String generatePresignedUploadUrl(String objectKey, String contentType);

    /**
     * Génère une URL pré-signée pour le téléchargement/visualisation (GET)
     * 
     * @param objectKey La clé (chemin) du fichier
     * @return L'URL pré-signée
     */
    String generatePresignedDownloadUrl(String objectKey);
}
