package com.photowah.photowah_be.repository;

import com.photowah.photowah_be.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PhotoRepository extends JpaRepository<Photo, UUID> {

    long countByEventId(UUID eventId);

    List<Photo> findAllByEventId(UUID eventId);

    Optional<Photo> findByIdAndEventPhotographerId(UUID id, UUID photographerId);

    @Query("SELECT p.event.id, COUNT(p) FROM Photo p WHERE p.event.id IN :eventIds GROUP BY p.event.id")
    List<Object[]> findPhotoCountsByEventIds(@Param("eventIds") List<UUID> eventIds);
}
