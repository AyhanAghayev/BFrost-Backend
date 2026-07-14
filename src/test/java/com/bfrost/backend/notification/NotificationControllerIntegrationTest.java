package com.bfrost.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String username) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload(
            username, username + "@example.com", "password123", "Display " + username));
        var result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return result.getResponse().getCookie("accessToken").getValue();
    }

    private Cookie accessTokenCookie(String token) {
        return new Cookie("accessToken", token);
    }

    private String currentUserId(String token) throws Exception {
        String content = mockMvc.perform(get("/api/v1/users/me").cookie(accessTokenCookie(token)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(content).get("id").asText();
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void newUserHasNoNotifications() throws Exception {
        String token = registerAndGetToken("empty" + UUID.randomUUID().toString().substring(0, 8));

        mockMvc.perform(get("/api/v1/notifications").cookie(accessTokenCookie(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/notifications/unread-count").cookie(accessTokenCookie(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void followingAUserCreatesAFollowNotificationForTheFollowee() throws Exception {
        String followerUsername = "follower" + UUID.randomUUID().toString().substring(0, 8);
        String followeeUsername = "followee" + UUID.randomUUID().toString().substring(0, 8);
        String followerToken = registerAndGetToken(followerUsername);
        String followeeToken = registerAndGetToken(followeeUsername);
        String followeeId = currentUserId(followeeToken);

        mockMvc.perform(post("/api/v1/users/" + followeeId + "/follow").cookie(accessTokenCookie(followerToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications").cookie(accessTokenCookie(followeeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].type").value("FOLLOW"))
            .andExpect(jsonPath("$[0].actorUsername").value(followerUsername))
            .andExpect(jsonPath("$[0].read").value(false));
        mockMvc.perform(get("/api/v1/notifications/unread-count").cookie(accessTokenCookie(followeeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void markAllReadClearsTheUnreadCount() throws Exception {
        String followerUsername = "follower" + UUID.randomUUID().toString().substring(0, 8);
        String followeeUsername = "followee" + UUID.randomUUID().toString().substring(0, 8);
        String followerToken = registerAndGetToken(followerUsername);
        String followeeToken = registerAndGetToken(followeeUsername);
        String followeeId = currentUserId(followeeToken);
        mockMvc.perform(post("/api/v1/users/" + followeeId + "/follow").cookie(accessTokenCookie(followerToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/notifications/mark-all-read").cookie(accessTokenCookie(followeeToken)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/notifications/unread-count").cookie(accessTokenCookie(followeeToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count").value(0));
    }

    private record RegisterPayload(String username, String email, String password, String displayName) {}
}
