package com.photowah.photowah_be.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "selfie_search")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"event", "matchedTag"})
public class SelfieSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = true)
    private String s3KeySelfie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_tag_id")
    private FaceTag matchedTag;

    private Double matchScore;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime searchedAt;
}
