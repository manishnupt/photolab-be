package com.photowah.photowah_be.controller;

import com.photowah.photowah_be.dto.selfie.SelfieSearchRequest;
import com.photowah.photowah_be.dto.selfie.SelfieSearchResponse;
import com.photowah.photowah_be.service.SelfieSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/selfie")
@RequiredArgsConstructor
@Tag(name = "Selfie Search", description = "Guest selfie photo search — no authentication required")
public class SelfieSearchController {

    private final SelfieSearchService selfieSearchService;

    @PostMapping("/search")
    @Operation(summary = "Find photos matching a guest selfie within a public event")
    public ResponseEntity<SelfieSearchResponse> search(@Valid @RequestBody SelfieSearchRequest request) {
        return ResponseEntity.ok(selfieSearchService.search(request));
    }
}
