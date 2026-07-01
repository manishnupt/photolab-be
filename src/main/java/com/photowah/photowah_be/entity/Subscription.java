package com.photowah.photowah_be.entity;

import com.photowah.photowah_be.enums.Plan;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscription")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "agency")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan;

    @Column(nullable = false)
    private Integer eventsUsed;

    @Column(nullable = false)
    private Integer eventsLimit;

    @Column(nullable = false)
    private Long storageLimitMb;

    @Column(nullable = false)
    private LocalDateTime renewsAt;
}
