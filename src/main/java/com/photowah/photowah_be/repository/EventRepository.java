package com.photowah.photowah_be.repository;

import com.photowah.photowah_be.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findAllByPhotographerIdOrderByCreatedAtDesc(UUID photographerId);

    Optional<Event> findByIdAndPhotographerId(UUID id, UUID photographerId);

    Optional<Event> findByShareableToken(UUID shareableToken);
}
