package com.bfrost.backend.user;

import com.bfrost.backend.user.dto.UpdateProfileRequest;
import com.bfrost.backend.user.dto.UserProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    @GetMapping("/me")
    public UserProfileDto getMe(@RequestHeader("X-User-Id") UUID currentUserId) {
        return userService.getProfileById(currentUserId, currentUserId);
    }

    @GetMapping("/{username}")
    public UserProfileDto getProfile(@PathVariable String username,
                                     @RequestHeader(value = "X-User-Id", required = false) UUID currentUserId) {
        return userService.getProfile(username, currentUserId);
    }

    @PatchMapping("/{userId}")
    public UserProfileDto updateProfile(@PathVariable UUID userId,
                                        @Valid @RequestBody UpdateProfileRequest req,
                                        @RequestHeader("X-User-Id") UUID currentUserId) {
        return userService.updateProfile(userId, currentUserId, req);
    }

    @GetMapping("/search")
    public List<UserProfileDto> search(@RequestParam String q,
                                       @RequestHeader(value = "X-User-Id", required = false) UUID currentUserId) {
        return userService.search(q, currentUserId);
    }
}
