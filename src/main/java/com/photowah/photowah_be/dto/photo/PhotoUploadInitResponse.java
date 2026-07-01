package com.photowah.photowah_be.dto.photo;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PhotoUploadInitResponse {

    private UUID photoId;
    private String presignedUploadUrl;
    private String s3Key;
    private int expiresInSeconds;
}
