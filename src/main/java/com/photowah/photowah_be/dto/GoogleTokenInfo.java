package com.photowah.photowah_be.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subset of fields returned by https://oauth2.googleapis.com/tokeninfo
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleTokenInfo {

    private String sub;
    private String email;
    private String name;
}
