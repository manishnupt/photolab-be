package com.photowah.photowah_be.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryResponse {

    private UUID id;
    private String name;
    private LocalDate eventDate;
    private String shareableToken;
    private String status;
    private int photoCount;
}
