package com.bfrost.backend.user;

import com.bfrost.backend.user.dto.UpdateProfileRequest;
import com.bfrost.backend.user.dto.UserProfileDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bfrost.backend.auth.BFrostUserDetails;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    
    @GetMapping("/me")
    public UserProfileDto getMe(@AuthenticationPrincipal BFrostUserDetails principal) {
        return userService.getProfile(principal.user().getUsername(), principal.userId());
    }

    @GetMapping("/{username}")
    public UserProfileDto getProfile(@PathVariable String username,
                                     @AuthenticationPrincipal BFrostUserDetails principal) {
        return userService.getProfile(username, principal != null ? principal.userId() : null);
    }

    @PatchMapping("/{userId}")
    public UserProfileDto updateProfile(@PathVariable UUID userId,
                                        @Valid @RequestBody UpdateProfileRequest req,
                                        @AuthenticationPrincipal BFrostUserDetails principal) {
        return userService.updateProfile(userId, principal.userId(), req);
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) {}

    @PostMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@RequestBody ChangePasswordRequest req,
                               @AuthenticationPrincipal BFrostUserDetails principal) {
        userService.changePassword(principal.userId(), req.currentPassword(), req.newPassword());
    }

    public record ChangeEmailRequest(String newEmail, String currentPassword) {}

    @PostMapping("/me/email")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeEmail(@RequestBody ChangeEmailRequest req,
                            @AuthenticationPrincipal BFrostUserDetails principal) {
        userService.changeEmail(principal.userId(), req.newEmail(), req.currentPassword());
    }

    public record DeleteAccountRequest(String currentPassword) {}

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@RequestBody DeleteAccountRequest req,
                              @AuthenticationPrincipal BFrostUserDetails principal) {
        userService.deleteAccount(principal.userId(), req.currentPassword());
    }

    @GetMapping("/{userId}/followers")
    public List<UserProfileDto> followers(@PathVariable UUID userId,
                                          @AuthenticationPrincipal BFrostUserDetails principal) {
        return userService.getFollowers(userId, principal != null ? principal.userId() : null);
    }

    @GetMapping("/{userId}/following")
    public List<UserProfileDto> following(@PathVariable UUID userId,
                                          @AuthenticationPrincipal BFrostUserDetails principal) {
        return userService.getFollowing(userId, principal != null ? principal.userId() : null);
    }

    @PostMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable UUID userId,
                       @AuthenticationPrincipal BFrostUserDetails principal) {
        userService.follow(principal.userId(), userId);
    }

    @DeleteMapping("/{userId}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable UUID userId,
                         @AuthenticationPrincipal BFrostUserDetails principal) {
        userService.unfollow(principal.userId(), userId);
    }

    @GetMapping("/me/friends")
    public List<UserProfileDto> friends(@AuthenticationPrincipal BFrostUserDetails principal) {
        return userService.getFriends(principal.userId());
    }

    @GetMapping("/search")
    public List<UserProfileDto> search(@RequestParam String q,
                                       @AuthenticationPrincipal BFrostUserDetails principal) {
        return userService.search(q, principal != null ? principal.userId() : null);
    }
}