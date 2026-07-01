package com.photowah.photowah_be.dto.selfie;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class SelfieSearchRequest {

    @NotBlank
    private String eventToken;

    @NotBlank
    private String selfieBase64;
}
