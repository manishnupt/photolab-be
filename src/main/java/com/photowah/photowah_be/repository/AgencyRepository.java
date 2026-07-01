package com.photowah.photowah_be.repository;

import com.photowah.photowah_be.entity.Agency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AgencyRepository extends JpaRepository<Agency, UUID> {
}
