package com.photowah.photowah_be.service;

import com.photowah.photowah_be.dto.photo.PhotoResponse;
import com.photowah.photowah_be.dto.recognition.SelfieEmbedRequest;
import com.photowah.photowah_be.dto.recognition.SelfieEmbedResponse;
import com.photowah.photowah_be.dto.selfie.SelfieSearchRequest;
import com.photowah.photowah_be.dto.selfie.SelfieSearchResponse;
import com.photowah.photowah_be.entity.Event;
import com.photowah.photowah_be.entity.FaceTag;
import com.photowah.photowah_be.entity.Photo;
import com.photowah.photowah_be.entity.PhotoFaceTag;
import com.photowah.photowah_be.entity.SelfieSearch;
import com.photowah.photowah_be.exception.EventNotFoundException;
import com.photowah.photowah_be.repository.EventRepository;
import com.photowah.photowah_be.repository.FaceTagRepository;
import com.photowah.photowah_be.repository.PhotoFaceTagRepository;
import com.photowah.photowah_be.repository.SelfieSearchRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelfieSearchService {

    private final EventRepository eventRepository;
    private final FaceTagRepository faceTagRepository;
    private final PhotoFaceTagRepository photoFaceTagRepository;
    private final SelfieSearchRepository selfieSearchRepository;
    private final S3Service s3Service;

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    private RestTemplate restTemplate;

    @PostConstruct
    private void init() {
        this.restTemplate = new RestTemplate();
    }

    @Transactional
    public SelfieSearchResponse search(SelfieSearchRequest req) {
        Event event = resolveEvent(req.getEventToken());

        SelfieEmbedResponse embedResponse = callEmbedService(req.getSelfieBase64());

        if (embedResponse == null || !embedResponse.isFaceDetected()) {
            selfieSearchRepository.save(SelfieSearch.builder()
                    .event(event)
                    .matchedTag(null)
                    .matchScore(0.0)
                    .build());
            return SelfieSearchResponse.builder()
                    .matchScore(0f)
                    .photos(List.of())
                    .message("No face detected")
                    .build();
        }

        List<FaceTag> faceTags = faceTagRepository.findByEventId(event.getId());
        if (faceTags.isEmpty()) {
            return SelfieSearchResponse.builder()
                    .matchScore(0f)
                    .photos(List.of())
                    .message("No match found")
                    .build();
        }

        float[] selfieEmbedding = toFloatArray(embedResponse.getEmbedding());
        FaceTag bestTag = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (FaceTag tag : faceTags) {
            double score = cosineSimilarity(selfieEmbedding, tag.getCentroidEmbedding());
            if (score > bestScore) {
                bestScore = score;
                bestTag = tag;
            }
        }

        if (bestScore >= 0.5) {
            List<PhotoResponse> photos = buildPhotoResponses(bestTag.getId(), event.getId());
            selfieSearchRepository.save(SelfieSearch.builder()
                    .event(event)
                    .matchedTag(bestTag)
                    .matchScore(bestScore)
                    .build());
            return SelfieSearchResponse.builder()
                    .matchedTagId(bestTag.getId())
                    .matchedTagLabel(bestTag.getTagLabel())
                    .matchScore((float) bestScore)
                    .photos(photos)
                    .message("Match found")
                    .build();
        }

        selfieSearchRepository.save(SelfieSearch.builder()
                .event(event)
                .matchedTag(null)
                .matchScore(bestScore)
                .build());
        return SelfieSearchResponse.builder()
                .matchScore((float) bestScore)
                .photos(List.of())
                .message("No match found")
                .build();
    }

    private Event resolveEvent(String token) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new EventNotFoundException(token);
        }
        return eventRepository.findByShareableToken(tokenUuid)
                .orElseThrow(() -> new EventNotFoundException(token));
    }

    private SelfieEmbedResponse callEmbedService(String selfieBase64) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SelfieEmbedRequest> entity = new HttpEntity<>(
                new SelfieEmbedRequest(selfieBase64), headers);
        ResponseEntity<SelfieEmbedResponse> response = restTemplate.postForEntity(
                pythonServiceUrl + "/embed", entity, SelfieEmbedResponse.class);
        return response.getBody();
    }

    private List<PhotoResponse> buildPhotoResponses(UUID faceTagId, UUID eventId) {
        List<PhotoFaceTag> pfts = photoFaceTagRepository.findByFaceTagId(faceTagId);
        return pfts.stream().map(pft -> {
            Photo photo = pft.getPhoto();
            String originalUrl = s3Service.generatePresignedDownloadUrl(photo.getS3KeyOriginal(), 60);
            String thumbUrl = photo.isThumbReady()
                    ? s3Service.generatePresignedDownloadUrl(photo.getS3KeyThumb(), 60)
                    : null;
            return PhotoResponse.builder()
                    .id(photo.getId())
                    .eventId(eventId)
                    .presignedThumbUrl(thumbUrl)
                    .presignedOriginalUrl(originalUrl)
                    .recognitionStatus(photo.getRecognitionStatus().name())
                    .uploadedAt(photo.getUploadedAt())
                    .build();
        }).toList();
    }

    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, magA = 0, magB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            magA += a[i] * a[i];
            magB += b[i] * b[i];
        }
        return dot / (Math.sqrt(magA) * Math.sqrt(magB));
    }

    private float[] toFloatArray(List<Float> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }
}
