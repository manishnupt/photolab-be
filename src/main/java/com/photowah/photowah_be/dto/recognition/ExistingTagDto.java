package com.photowah.photowah_be.dto.recognition;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ExistingTagDto {
    private UUID tagId;
    private String tagLabel;
    private float[] centroidEmbedding;
    private int faceCount;
}
