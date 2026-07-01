package com.photowah.photowah_be.entity;

import com.photowah.photowah_be.enums.RecognitionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "photo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "event")
public class Photo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private String s3KeyOriginal;

    @Column(nullable = false)
    private String s3KeyThumb;

    @Column(nullable = false)
    private Long fileSizeKb;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RecognitionStatus recognitionStatus = RecognitionStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private boolean thumbReady = false;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime uploadedAt;
}
