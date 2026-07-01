package com.photowah.photowah_be.repository;

import com.photowah.photowah_be.entity.Photographer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PhotographerRepository extends JpaRepository<Photographer, UUID> {

    Optional<Photographer> findByEmail(String email);

    boolean existsByEmail(String email);
}
