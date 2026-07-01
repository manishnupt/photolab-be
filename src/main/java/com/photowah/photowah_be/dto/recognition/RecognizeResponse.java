package com.photowah.photowah_be.dto.recognition;

import lombok.Data;

import java.util.List;

@Data
public class RecognizeResponse {
    private String photoId;
    private List<FaceTagResult> faces;
}
