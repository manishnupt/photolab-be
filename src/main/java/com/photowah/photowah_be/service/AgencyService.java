package com.photowah.photowah_be.service;

import com.photowah.photowah_be.dto.agency.AgencyResponse;
import com.photowah.photowah_be.dto.agency.StorageStatsResponse;
import com.photowah.photowah_be.entity.Agency;
import com.photowah.photowah_be.entity.Subscription;
import com.photowah.photowah_be.repository.AgencyRepository;
import com.photowah.photowah_be.repository.SubscriptionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AgencyResponse getMyAgency(UUID agencyId) {
        Agency agency = findAgency(agencyId);
        return toAgencyResponse(agency);
    }

    public StorageStatsResponse getStorageStats(UUID agencyId) {
        Agency agency = findAgency(agencyId);
        Subscription subscription = subscriptionRepository.findByAgencyId(agencyId)
                .orElseThrow(() -> new EntityNotFoundException("Subscription not found for agency: " + agencyId));

        double percent = agency.getStorageLimitMb() > 0
                ? (agency.getStorageUsedMb() * 100.0) / agency.getStorageLimitMb()
                : 0.0;

        return StorageStatsResponse.builder()
                .storageUsedMb(agency.getStorageUsedMb())
                .storageLimitMb(agency.getStorageLimitMb())
                .storageUsedPercent(Math.round(percent * 100.0) / 100.0)
                .eventsUsed(subscription.getEventsUsed())
                .eventsLimit(subscription.getEventsLimit())
                .build();
    }

    private Agency findAgency(UUID agencyId) {
        return agencyRepository.findById(agencyId)
                .orElseThrow(() -> new EntityNotFoundException("Agency not found: " + agencyId));
    }

    private AgencyResponse toAgencyResponse(Agency agency) {
        return AgencyResponse.builder()
                .id(agency.getId())
                .name(agency.getName())
                .email(agency.getEmail())
                .plan(agency.getPlan().name())
                .storageUsedMb(agency.getStorageUsedMb())
                .storageLimitMb(agency.getStorageLimitMb())
                .build();
    }
}
