package com.bfrost.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query(value = "SELECT * FROM users WHERE search_vector @@ plainto_tsquery('english', :query) ORDER BY ts_rank(search_vector, plainto_tsquery('english', :query)) DESC LIMIT :limit", nativeQuery = true)
    List<User> searchByText(String query, int limit);
}
