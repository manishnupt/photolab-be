package com.photowah.photowah_be.repository;

import com.photowah.photowah_be.entity.SelfieSearch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SelfieSearchRepository extends JpaRepository<SelfieSearch, UUID> {
}
