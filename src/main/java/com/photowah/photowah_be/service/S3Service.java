package com.photowah.photowah_be.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.bucket}")
    private String bucket;

    // -------------------------------------------------------------------------
    // Presigned URLs
    // -------------------------------------------------------------------------

    public String generatePresignedUploadUrl(String s3Key, String contentType, int expiryMinutes) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .putObjectRequest(putRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public String generatePresignedDownloadUrl(String s3Key, int expiryMinutes) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expiryMinutes))
                .getObjectRequest(getRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    // -------------------------------------------------------------------------
    // Hard delete
    // -------------------------------------------------------------------------

    public void deleteObject(String s3Key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .build());
    }

    // -------------------------------------------------------------------------
    // Key builder
    // -------------------------------------------------------------------------

    /**
     * Builds an S3 object key.
     * purpose must be one of: originals, thumbs, faces, selfies
     * selfies are stored at the root level (selfies/{filename}) since they are
     * not tied to a specific event's directory.
     */
    public String buildS3Key(UUID eventId, String purpose, String filename) {
        if ("selfies".equals(purpose)) {
            return "selfies/" + filename;
        }
        return "events/" + eventId + "/" + purpose + "/" + filename;
    }
}
