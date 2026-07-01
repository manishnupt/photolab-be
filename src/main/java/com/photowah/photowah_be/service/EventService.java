package com.photowah.photowah_be.service;

import com.photowah.photowah_be.dto.event.CreateEventRequest;
import com.photowah.photowah_be.dto.event.EventResponse;
import com.photowah.photowah_be.dto.event.EventSummaryResponse;
import com.photowah.photowah_be.entity.Event;
import com.photowah.photowah_be.entity.Photographer;
import com.photowah.photowah_be.entity.Subscription;
import com.photowah.photowah_be.enums.EventStatus;
import com.photowah.photowah_be.exception.EventLimitExceededException;
import com.photowah.photowah_be.exception.EventNotFoundException;
import com.photowah.photowah_be.repository.EventRepository;
import com.photowah.photowah_be.repository.PhotoRepository;
import com.photowah.photowah_be.repository.PhotographerRepository;
import com.photowah.photowah_be.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final PhotoRepository photoRepository;
    private final PhotographerRepository photographerRepository;
    private final SubscriptionRepository subscriptionRepository;

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @Transactional
    public EventResponse createEvent(CreateEventRequest req, UUID photographerId, UUID agencyId) {
        Subscription subscription = subscriptionRepository.findByAgencyId(agencyId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found for agency: " + agencyId));

        if (subscription.getEventsUsed() >= subscription.getEventsLimit()) {
            throw new EventLimitExceededException(subscription.getEventsLimit());
        }

        Photographer photographer = photographerRepository.findById(photographerId)
                .orElseThrow(() -> new EntityNotFoundException("Photographer not found: " + photographerId));

        // saveAndFlush forces the INSERT immediately so @CreationTimestamp is populated
        // before we map the entity to a response DTO
        Event event = eventRepository.saveAndFlush(Event.builder()
                .photographer(photographer)
                .name(req.getName())
                .eventDate(req.getEventDate())
                .status(EventStatus.ACTIVE)
                .build());

        subscription.setEventsUsed(subscription.getEventsUsed() + 1);
        subscriptionRepository.save(subscription);

        return toEventResponse(event, 0L);
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<EventSummaryResponse> getMyEvents(UUID photographerId) {
        List<Event> events = eventRepository.findAllByPhotographerIdOrderByCreatedAtDesc(photographerId);
        if (events.isEmpty()) {
            return List.of();
        }

        List<UUID> eventIds = events.stream().map(Event::getId).toList();
        Map<UUID, Long> photoCounts = batchPhotoCountsByEventId(eventIds);

        return events.stream()
                .map(e -> toEventSummaryResponse(e, photoCounts.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID eventId, UUID photographerId) {
        Event event = eventRepository.findByIdAndPhotographerId(eventId, photographerId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        long photoCount = photoRepository.countByEventId(eventId);
        return toEventResponse(event, photoCount);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventByShareableToken(String token) {
        UUID tokenUuid;
        try {
            tokenUuid = UUID.fromString(token);
        } catch (IllegalArgumentException e) {
            throw new EventNotFoundException(token);
        }
        Event event = eventRepository.findByShareableToken(tokenUuid)
                .orElseThrow(() -> new EventNotFoundException(token));
        long photoCount = photoRepository.countByEventId(event.getId());
        return toEventResponse(event, photoCount);
    }

    // -------------------------------------------------------------------------
    // Archive
    // -------------------------------------------------------------------------

    @Transactional
    public void archiveEvent(UUID eventId, UUID photographerId) {
        Event event = eventRepository.findByIdAndPhotographerId(eventId, photographerId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
        event.setStatus(EventStatus.ARCHIVED);
        eventRepository.save(event);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<UUID, Long> batchPhotoCountsByEventId(List<UUID> eventIds) {
        return photoRepository.findPhotoCountsByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    private EventResponse toEventResponse(Event event, long photoCount) {
        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .eventDate(event.getEventDate())
                .shareableToken(event.getShareableToken().toString())
                .status(event.getStatus().name())
                .photoCount((int) photoCount)
                .createdAt(event.getCreatedAt())
                .build();
    }

    private EventSummaryResponse toEventSummaryResponse(Event event, long photoCount) {
        return EventSummaryResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .eventDate(event.getEventDate())
                .shareableToken(event.getShareableToken().toString())
                .status(event.getStatus().name())
                .photoCount((int) photoCount)
                .build();
    }
}
