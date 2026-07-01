package com.photowah.photowah_be.dto.agency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgencyResponse {

    private UUID id;
    private String name;
    private String email;
    private String plan;
    private long storageUsedMb;
    private long storageLimitMb;
}
