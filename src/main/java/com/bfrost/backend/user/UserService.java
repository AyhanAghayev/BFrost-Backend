package com.bfrost.backend.user;

import com.bfrost.backend.user.dto.UpdateProfileRequest;
import com.bfrost.backend.user.dto.UserProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bfrost.backend.common.exception.ConflictException;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.common.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void changeEmail(UUID userId, String newEmail, String currentPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        if (newEmail == null || !newEmail.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        if (newEmail.equalsIgnoreCase(user.getEmail())) return;
        if (userRepository.existsByEmail(newEmail)) {
            throw new ConflictException("Email already in use: " + newEmail);
        }
        user.setEmail(newEmail);
        user.setEmailVerified(false);
    }

    @Transactional
    public void deleteAccount(UUID userId, String currentPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username, UUID currentUserId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        return buildDto(user, currentUserId);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfileById(UUID userId, UUID currentUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildDto(user, currentUserId);
    }

    @Transactional
    public UserProfileDto updateProfile(UUID userId, UUID currentUserId, UpdateProfileRequest req) {
        if (!userId.equals(currentUserId)) throw new ForbiddenException("Cannot update another user's profile");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (req.username() != null && !req.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(req.username())) {
                throw new ConflictException("Username already taken: " + req.username());
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

    @Transactional
    public void follow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) throw new IllegalArgumentException("Cannot follow yourself");
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw new ConflictException("Already following this user");
        }
        User follower = userRepository.getReferenceById(followerId);
        User followee = userRepository.getReferenceById(followeeId);
        followRepository.save(Follow.builder().follower(follower).followee(followee).build());
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followeeId) {
        Follow follow = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Not following this user"));
        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public List<UserProfileDto> search(String query, UUID currentUserId) {
        return userRepository.searchByText(query, 20).stream()
            .map(u -> buildDto(u, currentUserId))
            .toList();
    }

    private UserProfileDto buildDto(User user, UUID currentUserId) {
        boolean isSelf = currentUserId != null && currentUserId.equals(user.getId());
        return UserProfileDto.from(user, 0L, 0L, false, isSelf);
    }
}
