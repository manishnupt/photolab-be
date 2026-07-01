package com.photowah.photowah_be.service;

import com.photowah.photowah_be.dto.recognition.FaceTagResult;
import com.photowah.photowah_be.entity.FaceTag;
import com.photowah.photowah_be.entity.Photo;
import com.photowah.photowah_be.entity.PhotoFaceTag;
import com.photowah.photowah_be.entity.PhotoFaceTagId;
import com.photowah.photowah_be.enums.RecognitionStatus;
import com.photowah.photowah_be.repository.FaceTagRepository;
import com.photowah.photowah_be.repository.PhotoFaceTagRepository;
import com.photowah.photowah_be.repository.PhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class RecognitionTransactionHelper {

    private final PhotoRepository photoRepository;
    private final FaceTagRepository faceTagRepository;
    private final PhotoFaceTagRepository photoFaceTagRepository;
    private final ThumbnailService thumbnailService;

    record PhotoJobContext(UUID photoId, String s3KeyOriginal, UUID eventId) {}

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    PhotoJobContext markProcessing(UUID photoId) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));
        photo.setRecognitionStatus(RecognitionStatus.PROCESSING);
        photoRepository.saveAndFlush(photo);
        return new PhotoJobContext(photo.getId(), photo.getS3KeyOriginal(), photo.getEvent().getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void persistResults(UUID photoId, UUID eventId, List<FaceTagResult> results) {
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));

        if (results != null) {
            int tagCount = faceTagRepository.countByEventId(eventId);
            for (FaceTagResult result : results) {
                FaceTag faceTag;

                if (result.isNewTag()) {
                    tagCount++;
                    faceTag = FaceTag.builder()
                            .event(photo.getEvent())
                            .tagLabel("PERSON_" + tagCount)
                            .centroidEmbedding(result.getCentroidEmbedding())
                            .faceCount(1)
                            .build();
                    faceTag = faceTagRepository.save(faceTag);
                } else {
                    faceTag = faceTagRepository.findById(result.getTagId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "FaceTag not found for tagId: " + result.getTagId()));
                    updateCentroid(faceTag, result.getCentroidEmbedding());
                    faceTagRepository.save(faceTag);
                }

                photoFaceTagRepository.save(PhotoFaceTag.builder()
                        .id(new PhotoFaceTagId(photo.getId(), faceTag.getId()))
                        .photo(photo)
                        .faceTag(faceTag)
                        .similarityScore((double) result.getSimilarityScore())
                        .build());
            }
        }

        thumbnailService.generateAndUploadThumb(photo.getId(), photo.getS3KeyOriginal(), photo.getS3KeyThumb());
        photo.setThumbReady(true);
        photo.setRecognitionStatus(RecognitionStatus.DONE);
        photoRepository.save(photo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void markFailed(UUID photoId) {
        photoRepository.findById(photoId).ifPresent(photo -> {
            photo.setRecognitionStatus(RecognitionStatus.FAILED);
            photoRepository.save(photo);
        });
    }

    private void updateCentroid(FaceTag faceTag, float[] newEmbedding) {
        float[] old = faceTag.getCentroidEmbedding();
        int n = faceTag.getFaceCount();
        float[] updated = new float[old.length];
        for (int i = 0; i < old.length; i++) {
            updated[i] = (old[i] * n + newEmbedding[i]) / (n + 1);
        }
        faceTag.setCentroidEmbedding(updated);
        faceTag.setFaceCount(n + 1);
    }
}
