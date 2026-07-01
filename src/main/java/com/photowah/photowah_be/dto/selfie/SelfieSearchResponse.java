package com.photowah.photowah_be.dto.selfie;

import com.photowah.photowah_be.dto.photo.PhotoResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class SelfieSearchResponse {

    private UUID matchedTagId;
    private String matchedTagLabel;
    private float matchScore;
    private List<PhotoResponse> photos;
    private String message;
}
