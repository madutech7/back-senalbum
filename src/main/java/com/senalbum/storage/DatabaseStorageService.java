package com.senalbum.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
public class DatabaseStorageService implements StorageService {

  @Autowired
  private FileContentRepository fileContentRepository;

  private static final int PREVIEW_MAX_WIDTH = 1200;

  @Override
  public String saveOriginal(MultipartFile file, String albumId) throws IOException {
    FileContent content = new FileContent();
    content.setData(file.getBytes());
    content.setMimeType(file.getContentType());

    content = fileContentRepository.save(content);
    return content.getId().toString();
  }

  @Override
  public String savePreview(MultipartFile file, String albumId, boolean watermarkEnabled, String watermarkText)
      throws IOException {
    // Simple resizing logic (similar to LocalStorageService)
    if (file == null || file.isEmpty()) {
      throw new IOException("File is empty or null");
    }

    BufferedImage originalImage = ImageIO.read(file.getInputStream());
    if (originalImage == null) {
      throw new IOException("Invalid image file");
    }

    int originalWidth = originalImage.getWidth();
    int originalHeight = originalImage.getHeight();
    int newWidth = originalWidth;
    int newHeight = originalHeight;

    if (originalWidth > PREVIEW_MAX_WIDTH) {
      newWidth = PREVIEW_MAX_WIDTH;
      newHeight = (int) ((double) originalHeight * PREVIEW_MAX_WIDTH / originalWidth);
    }

    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = resizedImage.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);

    // Watermark Logic
    if (watermarkEnabled && watermarkText != null && !watermarkText.trim().isEmpty()) {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      // Font setup: proportional to width
      int fontSize = Math.max(20, newWidth / 25);
      Font font = new Font("Serif", Font.BOLD, fontSize); // Serif looks more "premium" often, or SansSerif
      g.setFont(font);

      FontMetrics metrics = g.getFontMetrics(font);
      int textWidth = metrics.stringWidth(watermarkText);
      int textHeight = metrics.getHeight();

      // Position: Bottom Right with padding
      int x = newWidth - textWidth - 30;
      int y = newHeight - 30;

      // Ensure it stays within bounds
      if (x < 10)
        x = 10;
      if (y < textHeight)
        y = textHeight + 10;

      // Draw Shadow/Outline for visibility on all backgrounds
      g.setColor(new Color(0, 0, 0, 100)); // Semi-transparent black
      g.drawString(watermarkText, x + 2, y + 2);

      // Draw Text
      g.setColor(new Color(255, 255, 255, 180)); // Semi-transparent white
      g.drawString(watermarkText, x, y);
    }

    g.dispose();

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(resizedImage, "jpg", baos); // Default to jpg for preview

    FileContent content = new FileContent();
    content.setData(baos.toByteArray());
    content.setMimeType("image/jpeg");

    content = fileContentRepository.save(content);
    return content.getId().toString();
  }

  @Override
  public byte[] getOriginalFile(String path) throws IOException {
    UUID id = UUID.fromString(path);
    return fileContentRepository.findById(id)
        .map(FileContent::getData)
        .orElseThrow(() -> new IOException("File not found: " + path));
  }

  @Override
  public byte[] getPreviewFile(String path) throws IOException {
    return getOriginalFile(path);
  }

  @Override
  public void deleteFile(String path) throws IOException {
    try {
      UUID id = UUID.fromString(path);
      fileContentRepository.deleteById(id);
    } catch (IllegalArgumentException e) {
      // Check if it's a legacy file path (local storage) and ignore or handle
    }
  }

  @Override
  public String generatePresignedUploadUrl(String objectKey, String contentType) {
    throw new UnsupportedOperationException("Database storage does not support presigned URLs");
  }

  @Override
  public String generatePresignedDownloadUrl(String objectKey) {
    throw new UnsupportedOperationException("Database storage does not support presigned URLs");
  }
}
