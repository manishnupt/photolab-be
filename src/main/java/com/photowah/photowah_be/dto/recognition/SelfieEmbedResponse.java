package com.photowah.photowah_be.dto.recognition;

import lombok.Getter;

import java.util.List;

@Getter
public class SelfieEmbedResponse {

    private boolean faceDetected;
    private List<Float> embedding;
}
