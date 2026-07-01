package com.photowah.photowah_be.controller;

import com.photowah.photowah_be.dto.agency.AgencyResponse;
import com.photowah.photowah_be.dto.agency.StorageStatsResponse;
import com.photowah.photowah_be.security.SecurityUtils;
import com.photowah.photowah_be.service.AgencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agency")
@RequiredArgsConstructor
@Tag(name = "Agency", description = "Agency profile and storage information")
@SecurityRequirement(name = "bearerAuth")
public class AgencyController {

    private final AgencyService agencyService;

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated agency's profile")
    public ResponseEntity<AgencyResponse> getMyAgency() {
        return ResponseEntity.ok(agencyService.getMyAgency(SecurityUtils.getCurrentAgencyId()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get storage and event usage statistics for the authenticated agency")
    public ResponseEntity<StorageStatsResponse> getStorageStats() {
        return ResponseEntity.ok(agencyService.getStorageStats(SecurityUtils.getCurrentAgencyId()));
    }
}
