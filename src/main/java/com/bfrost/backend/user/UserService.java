package com.bfrost.backend.user;

import com.bfrost.backend.user.dto.UpdateProfileRequest;
import com.bfrost.backend.user.dto.UserProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username, UUID currentUserId) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new NoSuchElementException("User not found: " + username));
        return buildDto(user, currentUserId);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfileById(UUID userId, UUID currentUserId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
        return buildDto(user, currentUserId);
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UUID currentUserId, UpdateProfileRequest req) {
        if (!userId.equals(currentUserId)) throw new SecurityException("Cannot update another user's profile");
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NoSuchElementException("User not found"));
        if (req.username() != null && !req.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(req.username())) {
                throw new IllegalStateException("Username already taken: " + req.username());
            }
            user.setUsername(req.username());
        }
        if (req.displayName()       != null) user.setDisplayName(req.displayName());
        if (req.bio()               != null) user.setBio(req.bio());
        if (req.profilePictureUrl() != null) user.setProfilePictureUrl(req.profilePictureUrl());
        if (req.backgroundUrl()     != null) user.setBackgroundUrl(req.backgroundUrl());
        if (req.university()        != null) user.setUniversity(req.university());
        if (req.department()        != null) user.setDepartment(req.department());
        return buildDto(user, currentUserId);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> search(String query, UUID currentUserId) {
        return userRepository.searchByText(query, 20).stream()
            .map(u -> buildDto(u, currentUserId))
            .toList();
    }

    // Follower/following counts are 0 and isFollowed is false until the follow system exists.
    private UserProfileDto buildDto(User user, UUID currentUserId) {
        boolean isSelf = currentUserId != null && currentUserId.equals(user.getId());
        return UserProfileDto.from(user, 0L, 0L, false, isSelf);
    }
}
