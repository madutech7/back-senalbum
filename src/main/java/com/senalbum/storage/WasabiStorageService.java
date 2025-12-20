package com.senalbum.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@Primary
public class WasabiStorageService implements StorageService {

  @Autowired
  private S3Client s3Client;

  @Autowired
  private S3Presigner s3Presigner;

  @Value("${wasabi.bucket}")
  private String bucketName;

  @Override
  public String saveOriginal(MultipartFile file, String albumId) throws IOException {
    String key = "albums/" + albumId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentType(file.getContentType())
        .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    return key;
  }

  @Override
  public String savePreview(MultipartFile file, String albumId, boolean watermarkEnabled, String watermarkText)
      throws IOException {
    // Note: Watermarking is not implemented here as we assume direct upload or
    // pre-processed files
    // If server-side processing is needed, we'd need to download, process, and
    // re-upload.
    // For now, simple upload similar to saveOriginal
    String key = "previews/" + albumId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

    PutObjectRequest putObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(key)
        .contentType(file.getContentType())
        .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    return key;
  }

  @Override
  public byte[] getOriginalFile(String path) throws IOException {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(path)
        .build();

    return s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
  }

  @Override
  public byte[] getPreviewFile(String path) throws IOException {
    return getOriginalFile(path);
  }

  @Override
  public void deleteFile(String path) throws IOException {
    DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
        .bucket(bucketName)
        .key(path)
        .build();

    s3Client.deleteObject(deleteObjectRequest);
  }

  @Override
  public String generatePresignedUploadUrl(String objectKey, String contentType) {
    PutObjectRequest objectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .contentType(contentType)
        .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(15)) // URL valid for 15 minutes
        .putObjectRequest(objectRequest)
        .build();

    return s3Presigner.presignPutObject(presignRequest).url().toString();
  }

  @Override
  public String generatePresignedDownloadUrl(String objectKey) {
    GetObjectRequest objectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build();

    GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(60)) // URL valid for 60 minutes
        .getObjectRequest(objectRequest)
        .build();

    return s3Presigner.presignGetObject(presignRequest).url().toString();
  }
}
