package com.photowah.photowah_be.service;

import com.photowah.photowah_be.dto.photo.PhotoListResponse;
import com.photowah.photowah_be.dto.photo.PhotoResponse;
import com.photowah.photowah_be.dto.photo.PhotoUploadInitRequest;
import com.photowah.photowah_be.dto.photo.PhotoUploadInitResponse;
import com.photowah.photowah_be.entity.Agency;
import com.photowah.photowah_be.entity.Event;
import com.photowah.photowah_be.entity.Photo;
import com.photowah.photowah_be.entity.Photographer;
import com.photowah.photowah_be.enums.EventStatus;
import com.photowah.photowah_be.exception.EventArchivedException;
import com.photowah.photowah_be.exception.EventNotFoundException;
import com.photowah.photowah_be.exception.StorageLimitExceededException;
import com.photowah.photowah_be.repository.AgencyRepository;
import com.photowah.photowah_be.repository.EventRepository;
import com.photowah.photowah_be.repository.PhotoRepository;
import com.photowah.photowah_be.repository.PhotographerRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoService {

    private static final int UPLOAD_EXPIRY_MINUTES  = 15;
    private static final int DOWNLOAD_EXPIRY_MINUTES = 60;
    private static final int UPLOAD_EXPIRY_SECONDS   = UPLOAD_EXPIRY_MINUTES * 60;

    private final PhotoRepository photoRepository;
    private final EventRepository eventRepository;
    private final AgencyRepository agencyRepository;
    private final PhotographerRepository photographerRepository;
    private final S3Service s3Service;
    private final RecognitionJobService recognitionJobService;

    // -------------------------------------------------------------------------
    // Initiate upload — returns a presigned PUT URL; no bytes touch our server
    // -------------------------------------------------------------------------

    @Transactional
    public PhotoUploadInitResponse initiateUpload(UUID eventId,
                                                  UUID photographerId,
                                                  PhotoUploadInitRequest req) {

        Event event = eventRepository.findByIdAndPhotographerId(eventId, photographerId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Event " + eventId + " not found or does not belong to this photographer"));

        if (event.getStatus() == EventStatus.ARCHIVED) {
            throw new EventArchivedException(eventId);
        }

        Photographer photographer = photographerRepository.findById(photographerId)
                .orElseThrow(() -> new EntityNotFoundException("Photographer not found: " + photographerId));
        Agency agency = photographer.getAgency();

        long fileSizeMb = req.getFileSizeKb() / 1024;
        if (agency.getStorageUsedMb() + fileSizeMb > agency.getStorageLimitMb()) {
            throw new StorageLimitExceededException(agency.getStorageUsedMb(), agency.getStorageLimitMb());
        }

        String s3KeyOriginal = s3Service.buildS3Key(eventId, "originals", req.getFilename());
        String s3KeyThumb    = s3Service.buildS3Key(eventId, "thumbs",    req.getFilename());

        Photo photo = photoRepository.saveAndFlush(Photo.builder()
                .event(event)
                .s3KeyOriginal(s3KeyOriginal)
                .s3KeyThumb(s3KeyThumb)
                .fileSizeKb(req.getFileSizeKb())
                .build());

        agency.setStorageUsedMb(agency.getStorageUsedMb() + fileSizeMb);
        agencyRepository.save(agency);

        String presignedUrl = s3Service.generatePresignedUploadUrl(
                s3KeyOriginal, req.getContentType(), UPLOAD_EXPIRY_MINUTES);

        return PhotoUploadInitResponse.builder()
                .photoId(photo.getId())
                .presignedUploadUrl(presignedUrl)
                .s3Key(s3KeyOriginal)
                .expiresInSeconds(UPLOAD_EXPIRY_SECONDS)
                .build();
    }

    // -------------------------------------------------------------------------
    // Confirm upload — client calls this after S3 PUT completes
    // -------------------------------------------------------------------------

    @Transactional
    public void confirmUpload(UUID photoId, UUID photographerId) {
        photoRepository.findByIdAndEventPhotographerId(photoId, photographerId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Photo " + photoId + " not found or does not belong to this photographer"));

        recognitionJobService.enqueueRecognitionJob(photoId);
    }

    // -------------------------------------------------------------------------
    // Authenticated — list photos for an owned event
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PhotoListResponse getPhotosForEvent(UUID eventId, UUID photographerId) {
        eventRepository.findByIdAndPhotographerId(eventId, photographerId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Event " + eventId + " not found or does not belong to this photographer"));

        List<Photo> photos = photoRepository.findAllByEventId(eventId);
        return toPhotoListResponse(photos, eventId);
    }

    // -------------------------------------------------------------------------
    // Public — list photos by shareable token, no auth
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PhotoListResponse getPhotosForPublicEvent(String shareableToken) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(shareableToken);
        } catch (IllegalArgumentException e) {
            throw new EventNotFoundException(shareableToken);
        }

        Event event = eventRepository.findByShareableToken(tokenUuid)
                .orElseThrow(() -> new EventNotFoundException(shareableToken));

        List<Photo> photos = photoRepository.findAllByEventId(event.getId());
        return toPhotoListResponse(photos, event.getId());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private PhotoListResponse toPhotoListResponse(List<Photo> photos, UUID eventId) {
        List<PhotoResponse> responses = photos.stream()
                .map(p -> toPhotoResponse(p, eventId))
                .toList();
        return PhotoListResponse.builder()
                .photos(responses)
                .totalCount(responses.size())
                .build();
    }

    private PhotoResponse toPhotoResponse(Photo photo, UUID eventId) {
        String thumbUrl = photo.isThumbReady()
                ? s3Service.generatePresignedDownloadUrl(photo.getS3KeyThumb(), DOWNLOAD_EXPIRY_MINUTES)
                : null;
        return PhotoResponse.builder()
                .id(photo.getId())
                .eventId(eventId)
                .presignedThumbUrl(thumbUrl)
                .presignedOriginalUrl(
                        s3Service.generatePresignedDownloadUrl(photo.getS3KeyOriginal(), DOWNLOAD_EXPIRY_MINUTES))
                .recognitionStatus(photo.getRecognitionStatus().name())
                .uploadedAt(photo.getUploadedAt())
                .build();
    }
}
