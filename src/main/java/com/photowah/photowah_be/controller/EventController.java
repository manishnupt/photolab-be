package com.photowah.photowah_be.controller;

import com.photowah.photowah_be.dto.event.CreateEventRequest;
import com.photowah.photowah_be.dto.event.EventResponse;
import com.photowah.photowah_be.dto.event.EventSummaryResponse;
import com.photowah.photowah_be.security.SecurityUtils;
import com.photowah.photowah_be.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Create and manage photo events")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "Create a new event", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                eventService.createEvent(
                        request,
                        SecurityUtils.getCurrentPhotographerId(),
                        SecurityUtils.getCurrentAgencyId()
                )
        );
    }

    @GetMapping
    @Operation(summary = "List all events for the authenticated photographer", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<EventSummaryResponse>> getMyEvents() {
        return ResponseEntity.ok(eventService.getMyEvents(SecurityUtils.getCurrentPhotographerId()));
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Get a single event by ID (must belong to the authenticated photographer)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID eventId) {
        return ResponseEntity.ok(
                eventService.getEventById(eventId, SecurityUtils.getCurrentPhotographerId())
        );
    }

    @PatchMapping("/{eventId}/archive")
    @Operation(summary = "Archive an event", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> archiveEvent(@PathVariable UUID eventId) {
        eventService.archiveEvent(eventId, SecurityUtils.getCurrentPhotographerId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/public/{token}")
    @Operation(summary = "Public event lookup by shareable token — no authentication required")
    public ResponseEntity<EventResponse> getEventByShareableToken(@PathVariable String token) {
        return ResponseEntity.ok(eventService.getEventByShareableToken(token));
    }
}
