package com.bfrost.backend.event;

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

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ClubControllerIntegrationTest {

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

    private String createClub(String token, String slug, boolean isPublic) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "name", "Club " + slug, "slug", slug, "description", "desc",
            "category", "Games", "isPublic", isPublic));
        String response = mockMvc.perform(post("/api/v1/clubs")
                .cookie(accessTokenCookie(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    void listClubsIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/clubs"))
            .andExpect(status().isOk());
    }

    @Test
    void createRequiresAuthentication() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "name", "X", "slug", "x-" + UUID.randomUUID(), "category", "Games", "isPublic", true));

        mockMvc.perform(post("/api/v1/clubs").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }
}
