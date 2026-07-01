package com.photowah.photowah_be.dto.recognition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class FaceTagResult {

    private UUID tagId;
    private String tagLabel;
    private float similarityScore;
    @JsonProperty("isNewTag")
    private boolean isNewTag;
    private float[] centroidEmbedding;
}
