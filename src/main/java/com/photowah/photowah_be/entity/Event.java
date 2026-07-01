package com.photowah.photowah_be.entity;

import com.photowah.photowah_be.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "photographer")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photographer_id", nullable = false)
    private Photographer photographer;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID shareableToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void assignShareableToken() {
        if (shareableToken == null) {
            shareableToken = UUID.randomUUID();
        }
    }
}
