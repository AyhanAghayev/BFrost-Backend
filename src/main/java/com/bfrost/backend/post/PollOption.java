package com.bfrost.backend.post;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "poll_options")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PollOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "option_text", nullable = false, length = 255)
    private String optionText;

    @Column(name = "vote_count", nullable = false)
    @Builder.Default
    private int voteCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int position = 0;
}
