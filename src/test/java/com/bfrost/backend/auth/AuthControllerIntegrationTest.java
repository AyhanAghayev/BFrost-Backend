package com.bfrost.backend.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void registerCreatesAccountAndReturnsAccessTokenAndRefreshTokenAsHttpOnlyCookies() throws Exception {
        String suffix = uniqueSuffix();
        String body = objectMapper.writeValueAsString(new RegisterPayload(
            "user" + suffix, "user" + suffix + "@example.com", "password123", "Test User"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andExpect(jsonPath("$.username").value("user" + suffix))
            .andExpect(cookie().exists("accessToken"))
            .andExpect(cookie().httpOnly("accessToken", true))
            .andExpect(cookie().exists("refreshToken"))
            .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void registerRejectsDuplicateEmail() throws Exception {
        String suffix = uniqueSuffix();
        String email = "dup" + suffix + "@example.com";
        String first = objectMapper.writeValueAsString(new RegisterPayload("dup1" + suffix, email, "password123", "First"));
        String second = objectMapper.writeValueAsString(new RegisterPayload("dup2" + suffix, email, "password123", "Second"));

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(first))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(second))
            .andExpect(status().isConflict());
    }

    @Test
    void registerRejectsInvalidPayload() throws Exception {
        String body = "{\"username\":\"a\",\"email\":\"not-an-email\",\"password\":\"short\",\"displayName\":\"\"}";

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isNotEmpty());
    }

    @Test
    void loginSucceedsWithCorrectCredentialsAndFailsWithWrongPassword() throws Exception {
        String suffix = uniqueSuffix();
        String email = "login" + suffix + "@example.com";
        String registerBody = objectMapper.writeValueAsString(new RegisterPayload("login" + suffix, email, "password123", "Login User"));
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
            .andExpect(status().isCreated());

        String loginBody = objectMapper.writeValueAsString(new LoginPayload(email, "password123"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(loginBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andExpect(cookie().exists("accessToken"))
            .andExpect(cookie().httpOnly("accessToken", true));

        String wrongBody = objectMapper.writeValueAsString(new LoginPayload(email, "wrong-password"));
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrongBody))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRejectsUnknownEmail() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginPayload("nobody-" + uniqueSuffix() + "@example.com", "password123"));

        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieFails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithValidCookieRotatesAccessTokenCookieWithoutExposingItInJson() throws Exception {
        String suffix = uniqueSuffix();
        String body = objectMapper.writeValueAsString(new RegisterPayload(
            "refresh" + suffix, "refresh" + suffix + "@example.com", "password123", "Refresh User"));

        var registerResult = mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn();
        var refreshCookie = registerResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(refreshCookie))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andExpect(cookie().exists("accessToken"))
            .andExpect(cookie().httpOnly("accessToken", true));
    }

    @Test
    void logoutWithoutTokenStillClearsCookies() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
            .andExpect(status().isNoContent())
            .andExpect(cookie().maxAge("accessToken", 0))
            .andExpect(cookie().maxAge("refreshToken", 0));
    }

    private record RegisterPayload(String username, String email, String password, String displayName) {}
    private record LoginPayload(String email, String password) {}
}
