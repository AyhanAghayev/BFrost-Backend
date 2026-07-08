package com.bfrost.backend.post;

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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PostControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String registerAndGetToken(String username) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "username", username, "email", username + "@example.com",
            "password", "password123", "displayName", "Display " + username));
        var result = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andReturn();
        return result.getResponse().getCookie("accessToken").getValue();
    }

    private Cookie accessTokenCookie(String token) {
        return new Cookie("accessToken", token);
    }

    private String userId(String token) throws Exception {
        String response = mockMvc.perform(get("/api/v1/users/me").cookie(accessTokenCookie(token)))
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void createTextPostSucceeds() throws Exception {
        String token = registerAndGetToken("poster" + UUID.randomUUID().toString().substring(0, 8));
        String uid = userId(token);
        String body = objectMapper.writeValueAsString(Map.of(
            "targetType", "USER_PAGE", "targetId", uid, "postType", "TEXT", "body", "Hello world"));

        mockMvc.perform(post("/api/v1/posts").cookie(accessTokenCookie(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.body").value("Hello world"));
    }

    @Test
    void createPostRejectsBlankBody() throws Exception {
        String token = registerAndGetToken("blank" + UUID.randomUUID().toString().substring(0, 8));
        String uid = userId(token);
        String body = objectMapper.writeValueAsString(Map.of(
            "targetType", "USER_PAGE", "targetId", uid, "postType", "TEXT", "body", ""));

        mockMvc.perform(post("/api/v1/posts").cookie(accessTokenCookie(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "targetType", "USER_PAGE", "targetId", UUID.randomUUID().toString(),
            "postType", "TEXT", "body", "hi"));

        mockMvc.perform(post("/api/v1/posts").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }
}
