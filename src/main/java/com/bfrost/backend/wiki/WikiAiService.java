package com.bfrost.backend.wiki;

import com.bfrost.backend.club.MemberRole;
import com.bfrost.backend.club.MembershipRepository;
import com.bfrost.backend.common.exception.ForbiddenException;
import com.bfrost.backend.wiki.dto.AiDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class WikiAiService {

    private final MembershipRepository membershipRepository;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final RestClient client;

    public WikiAiService(MembershipRepository membershipRepository,
                         ObjectMapper objectMapper,
                         @Value("${openai.api-key}") String apiKey,
                         @Value("${openai.model}") String model,
                         @Value("${openai.base-url}") String baseUrl) {
        this.membershipRepository = membershipRepository;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public boolean isEnabled() {
        return StringUtils.hasText(apiKey); // Note for deploy: we will be using gpt 4o mini
    }

    public AiDto.DraftResponse draft(UUID clubId, UUID userId, String topic) {
        requireModerator(clubId, userId);
        String system = "You write concise, helpful wiki articles for a university club's "
                + "knowledge base. Respond ONLY with a JSON object with keys \"summary\" (plain "
                + "text, at most 280 characters) and \"body\" (GitHub-flavored markdown, no top-level "
                + "H1 heading). Keep it practical and well-structured.";
        JsonNode json = completeAsJson(system, "Topic: " + topic, 900);
        return new AiDto.DraftResponse(json.path("summary").asText(""), json.path("body").asText(""));
    }

    public AiDto.SummaryResponse summarize(UUID clubId, UUID userId, String body) {
        requireModerator(clubId, userId);
        String trimmed = body.length() > 6000 ? body.substring(0, 6000) : body;
        String system = "Summarize the following club wiki article in at most 60 words of plain "
                + "text. No markdown, no preamble.";
        String summary = completeText(system, trimmed, 160).strip();
        return new AiDto.SummaryResponse(summary);
    }

    private String completeText(String system, String user, int maxTokens) {
        JsonNode resp = call(buildBody(system, user, maxTokens, false));
        return resp.path("choices").path(0).path("message").path("content").asText("");
    }

    private JsonNode completeAsJson(String system, String user, int maxTokens) {
        JsonNode resp = call(buildBody(system, user, maxTokens, true));
        String content = resp.path("choices").path(0).path("message").path("content").asText("{}");
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI returned an unexpected response");
        }
    }

    private Map<String, Object> buildBody(String system, String user, int maxTokens, boolean jsonMode) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("max_tokens", maxTokens);
        body.put("messages", List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        ));
        if (jsonMode) body.put("response_format", Map.of("type", "json_object"));
        return body;
    }

    private JsonNode call(Map<String, Object> body) {
        if (!isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI features are not configured");
        }
        try {
            return client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Couldn't generate right now, please try again");
        }
    }

    private void requireModerator(UUID clubId, UUID userId) {
        boolean canManage = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .map(m -> m.getRole() != MemberRole.MEMBER)
                .orElse(false);
        if (!canManage) throw new ForbiddenException("Moderator or owner required");
    }
}
