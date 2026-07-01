package com.photowah.photowah_be.dto.recognition;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SelfieEmbedRequest {

    private String selfieBase64;
}
