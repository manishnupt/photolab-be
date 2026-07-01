package com.photowah.photowah_be.dto.photo;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PhotoListResponse {

    private List<PhotoResponse> photos;
    private int totalCount;
}
