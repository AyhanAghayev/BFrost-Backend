package com.bfrost.backend.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StorageControllerIntegrationTest {

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

    @Test
    void uploadRequiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "data".getBytes());

        mockMvc.perform(multipart("/api/v1/upload/avatars").file(file))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void uploadRejectsEmptyFile() throws Exception {
        String username = "up" + UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndGetToken(username);
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/v1/upload/avatars").file(emptyFile).cookie(accessTokenCookie(token)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void uploadStoresFileAndReturnsUrl() throws Exception {
        String username = "up" + UUID.randomUUID().toString().substring(0, 8);
        String token = registerAndGetToken(username);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "data".getBytes());

        mockMvc.perform(multipart("/api/v1/upload/avatars").file(file).cookie(accessTokenCookie(token)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.containsString("/uploads/avatars/")));
    }

    private record RegisterPayload(String username, String email, String password, String displayName) {}
}
