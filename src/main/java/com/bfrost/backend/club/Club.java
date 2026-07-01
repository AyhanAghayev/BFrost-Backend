package com.bfrost.backend.club;



import com.bfrost.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "clubs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @ElementCollection
    @CollectionTable(name = "club_tags", joinColumns = @JoinColumn(name = "club_id"))
    @Column(name = "tag", length = 50)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() { this.createdAt = Instant.now(); }
}
