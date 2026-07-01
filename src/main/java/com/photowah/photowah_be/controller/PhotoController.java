package com.photowah.photowah_be.controller;

import com.photowah.photowah_be.dto.photo.PhotoListResponse;
import com.photowah.photowah_be.dto.photo.PhotoUploadInitRequest;
import com.photowah.photowah_be.dto.photo.PhotoUploadInitResponse;
import com.photowah.photowah_be.security.SecurityUtils;
import com.photowah.photowah_be.service.PhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Photos", description = "Upload and retrieve event photos")
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping("/{eventId}/photos/initiate")
    @Operation(summary = "Initiate a photo upload — returns a presigned S3 PUT URL",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PhotoUploadInitResponse> initiateUpload(
            @PathVariable UUID eventId,
            @Valid @RequestBody PhotoUploadInitRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                photoService.initiateUpload(eventId, SecurityUtils.getCurrentPhotographerId(), request)
        );
    }

    @PostMapping("/{eventId}/photos/{photoId}/confirm")
    @Operation(summary = "Confirm a completed S3 upload — triggers async face recognition",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> confirmUpload(
            @PathVariable UUID eventId,
            @PathVariable UUID photoId) {

        photoService.confirmUpload(photoId, SecurityUtils.getCurrentPhotographerId());
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/{eventId}/photos")
    @Operation(summary = "List all photos for an owned event",
               security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<PhotoListResponse> getPhotosForEvent(@PathVariable UUID eventId) {
        return ResponseEntity.ok(
                photoService.getPhotosForEvent(eventId, SecurityUtils.getCurrentPhotographerId())
        );
    }

    @GetMapping("/public/{token}/photos")
    @Operation(summary = "List photos for a public event by shareable token — no authentication required")
    public ResponseEntity<PhotoListResponse> getPhotosForPublicEvent(@PathVariable String token) {
        return ResponseEntity.ok(photoService.getPhotosForPublicEvent(token));
    }
}
