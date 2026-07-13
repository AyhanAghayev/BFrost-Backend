package com.bfrost.backend.event;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event_questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private ClubEvent event;

    @Column(nullable = false, length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestionType type;

    @Column(nullable = false)
    @Builder.Default
    private boolean required = false;

    @Column(nullable = false)
    @Builder.Default
    private int position = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "event_question_options", joinColumns = @JoinColumn(name = "question_id"))
    @OrderColumn(name = "position")
    @Column(name = "opt", length = 255)
    @Builder.Default
    private List<String> options = new ArrayList<>();
}
