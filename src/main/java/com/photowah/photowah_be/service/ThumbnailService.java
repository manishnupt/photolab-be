package com.photowah.photowah_be.service;

import com.photowah.photowah_be.entity.Photo;
import com.photowah.photowah_be.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ThumbnailService {

    private final S3Client s3Client;
    private final PhotoRepository photoRepository;

    @Value("${app.aws.bucket}")
    private String bucket;

    public void generateAndUploadThumb(UUID photoId, String s3KeyOriginal, String s3KeyThumb) {
        byte[] thumbBytes = generateThumb(s3KeyOriginal);
        uploadThumb(s3KeyThumb, thumbBytes);

        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));
        photo.setThumbReady(true);
        photoRepository.save(photo);
    }

    private byte[] generateThumb(String s3KeyOriginal) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(s3KeyOriginal)
                .build();
        try (ResponseInputStream<GetObjectResponse> s3Stream = s3Client.getObject(getRequest);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Thumbnails.of(s3Stream)
                    .size(400, 400)
                    .outputFormat("jpg")
                    .outputQuality(0.85)
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate thumbnail for " + s3KeyOriginal, e);
        }
    }

    private void uploadThumb(String s3KeyThumb, byte[] thumbBytes) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3KeyThumb)
                .contentType("image/jpeg")
                .contentLength((long) thumbBytes.length)
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(thumbBytes));
    }
}
