package com.photowah.photowah_be.repository;

import com.photowah.photowah_be.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    Optional<Subscription> findByAgencyId(UUID agencyId);
}
