package com.bfrost.backend.event;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "rsvp_answers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RsvpAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rsvp_id", nullable = false)
    private Rsvp rsvp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private EventQuestion question;

    @Column(columnDefinition = "TEXT")
    private String value;
}
