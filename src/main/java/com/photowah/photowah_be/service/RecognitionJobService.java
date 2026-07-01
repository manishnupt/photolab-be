package com.photowah.photowah_be.service;

import com.photowah.photowah_be.dto.recognition.ExistingTagDto;
import com.photowah.photowah_be.dto.recognition.FaceTagResult;
import com.photowah.photowah_be.dto.recognition.RecognizeResponse;
import com.photowah.photowah_be.repository.FaceTagRepository;
import com.photowah.photowah_be.service.RecognitionTransactionHelper.PhotoJobContext;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RecognitionJobService {

    private final RecognitionTransactionHelper txHelper;
    private final FaceTagRepository faceTagRepository;

    @Value("${python.service.url:http://localhost:8000}")
    private String pythonServiceUrl;

    private RestClient restClient;
    private RestTemplate restTemplate;

    @PostConstruct
    private void init() {
        this.restClient = RestClient.create(pythonServiceUrl);
        this.restTemplate = new RestTemplate();
    }

    record RecognizeRequest(UUID photoId, String s3KeyOriginal, UUID eventId, List<ExistingTagDto> existingTags) {}

    @Async("recognitionExecutor")
    public void enqueueRecognitionJob(UUID photoId) {
        PhotoJobContext ctx;
        try {
            ctx = txHelper.markProcessing(photoId);
        } catch (Exception e) {
            log.error("Failed to mark photo {} as PROCESSING: {}", photoId, e.getMessage(), e);
            return;
        }

        try {
            List<ExistingTagDto> existingTags = faceTagRepository.findByEventId(ctx.eventId())
                    .stream()
                    .map(tag -> new ExistingTagDto(
                            tag.getId(),
                            tag.getTagLabel(),
                            tag.getCentroidEmbedding(),
                            tag.getFaceCount()))
                    .toList();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RecognizeRequest> entity = new HttpEntity<>(
                    new RecognizeRequest(ctx.photoId(), ctx.s3KeyOriginal(), ctx.eventId(), existingTags),
                    headers
            );
            ResponseEntity<RecognizeResponse> httpResponse = restTemplate.postForEntity(
                    pythonServiceUrl + "/recognize", entity, RecognizeResponse.class
            );
            RecognizeResponse response = httpResponse.getBody();

            List<FaceTagResult> results = (response != null) ? response.getFaces() : null;
            txHelper.persistResults(ctx.photoId(), ctx.eventId(), results);

        } catch (Exception e) {
            log.error("Face recognition failed for photo {}: {}", photoId, e.getMessage(), e);
            try {
                txHelper.markFailed(photoId);
            } catch (Exception ex) {
                log.error("Failed to mark photo {} as FAILED: {}", photoId, ex.getMessage(), ex);
            }
        }
    }
}
