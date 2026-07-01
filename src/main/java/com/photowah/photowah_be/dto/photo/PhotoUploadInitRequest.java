package com.photowah.photowah_be.dto.photo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PhotoUploadInitRequest {

    @NotBlank
    private String filename;

    @NotBlank
    private String contentType;

    @NotNull
    @Min(1)
    private Long fileSizeKb;
}
