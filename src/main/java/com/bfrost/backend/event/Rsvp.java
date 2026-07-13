package com.bfrost.backend.event;

import com.bfrost.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rsvps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rsvp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private ClubEvent event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RsvpStatus status = RsvpStatus.ATTENDING;

    @Column(name = "responded_at", nullable = false)
    private Instant respondedAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean attended = false;

    @PrePersist
    void prePersist() { this.respondedAt = Instant.now(); }
}
