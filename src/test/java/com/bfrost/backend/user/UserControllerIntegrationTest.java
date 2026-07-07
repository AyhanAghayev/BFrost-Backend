package com.bfrost.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
@ActiveProfiles("dev")
@Transactional
class UserControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String username) throws Exception {
        String body = objectMapper.writeValueAsString(new RegisterPayload(
            username, username + "@example.com", "password123", "Display " + username));
        var result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andReturn();
        return result.getResponse().getCookie("accessToken").getValue();
    }

    private Cookie accessTokenCookie(String token) {
        return new Cookie("accessToken", token);
    }

    @Test
    void getMeRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getMeReturnsOwnProfileIncludingEmailWithValidToken() throws Exception {
        String username = "me" + UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndGetToken(username);

        mockMvc.perform(get("/api/v1/users/me").cookie(accessTokenCookie(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(username))
            .andExpect(jsonPath("$.email").value(username + "@example.com"));
    }

    @Test
    void getPublicProfileHidesEmailFromStrangers() throws Exception {
        String username = "pub" + UUID.randomUUID().toString().substring(0, 8);
        registerAndGetToken(username);

        mockMvc.perform(get("/api/v1/users/" + username))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(username))
            .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void getProfileForUnknownUsernameReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/does-not-exist-" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    void followingYourselfIsRejected() throws Exception {
        String username = "self" + UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndGetToken(username);
        String me = objectMapper.readTree(mockMvc.perform(get("/api/v1/users/me")
                .cookie(accessTokenCookie(token)))
            .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/users/" + me + "/follow").cookie(accessTokenCookie(token)))
            .andExpect(status().isBadRequest());
    }

    private record RegisterPayload(String username, String email, String password, String displayName) {}
}
