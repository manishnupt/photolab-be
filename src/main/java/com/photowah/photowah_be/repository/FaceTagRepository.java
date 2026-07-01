package com.photowah.photowah_be.repository;

import com.photowah.photowah_be.entity.FaceTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FaceTagRepository extends JpaRepository<FaceTag, UUID> {
    List<FaceTag> findByEventId(UUID eventId);
    int countByEventId(UUID eventId);
}
