package com.photowah.photowah_be.dto.photo;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class PhotoResponse {

    private UUID id;
    private UUID eventId;
    private String presignedThumbUrl;
    private String presignedOriginalUrl;
    private String recognitionStatus;
    private LocalDateTime uploadedAt;
}
