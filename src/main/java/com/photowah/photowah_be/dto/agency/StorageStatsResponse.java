package com.photowah.photowah_be.dto.agency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageStatsResponse {

    private long storageUsedMb;
    private long storageLimitMb;
    private double storageUsedPercent;
    private int eventsUsed;
    private int eventsLimit;
}
