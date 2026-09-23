package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideModelGateway;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.ExperienceStructuredState;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideModelResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AliyunChatModelGateway implements VoiceGuideModelGateway {

    private static final Logger log = LoggerFactory.getLogger(AliyunChatModelGateway.class);
    private static final Pattern FENCED_JSON_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?})\\s*```");

    private final RestClient restClient;
    private final VoiceGuideProperties properties;
    private final ObjectMapper objectMapper;

    public AliyunChatModelGateway(
            RestClient restClient,
            VoiceGuideProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public VoiceGuideModelResult generate(
            String systemPrompt,
            String userPrompt,
            VoiceGuideExperienceType experienceType
    ) {
        VoiceGuideProperties.AliyunLlmProperties llm = properties.getLlm().getAliyun();
        if (llm.getApiKey() == null || llm.getApiKey().isBlank()) {
            throw new IllegalStateException("Aliyun LLM apiKey is missing");
        }

        Map<String, Object> request = Map.of(
                "model", llm.getModel(),
                "temperature", llm.getTemperature(),
                "max_tokens", llm.getMaxTokens(),
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        int maxAttempts = Math.max(1, llm.getMaxAttempts());
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startedAt = System.nanoTime();
            try {
                log.info("调用阿里云大模型 | experienceType={} | model={} | baseUrl={} | attempt={}/{}",
                        experienceType, llm.getModel(), llm.getBaseUrl(), attempt, maxAttempts);
                String responseBody = restClient.post()
                        .uri("/chat/completions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + llm.getApiKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(String.class);
                VoiceGuideModelResult result = parseResult(responseBody, experienceType);
                log.info("阿里云大模型处理完成 | experienceType={} | attempt={} | elapsedMs={}",
                        experienceType, attempt, elapsedMillis(startedAt));
                return result;
            } catch (RuntimeException ex) {
                lastFailure = ex;
                boolean retryable = isRetryable(ex);
                log.warn("阿里云大模型调用失败 | experienceType={} | attempt={}/{} | retryable={} | elapsedMs={} | reason={}",
                        experienceType, attempt, maxAttempts, retryable, elapsedMillis(startedAt), rootMessage(ex));
                if (!retryable || attempt >= maxAttempts) {
                    throw ex;
                }
                sleepBeforeRetry(llm.getRetryInitialDelayMillis(), attempt);
            }
        }
        throw lastFailure == null ? new IllegalStateException("Aliyun LLM request failed") : lastFailure;
    }

    private VoiceGuideModelResult parseResult(String responseBody, VoiceGuideExperienceType experienceType) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("choices").path(0).path("message").path("content").asText("");
            log.info("阿里云大模型返回成功 | experienceType={} | contentLength={}",
                    experienceType, content == null ? 0 : content.length());
            if (content == null || content.isBlank()) {
                throw new IllegalStateException("Aliyun LLM returned blank content");
            }
            VoiceGuideModelResult result = parseContentPayload(content);
            validateResult(result);
            return result;
        } catch (Exception ex) {
            if (ex instanceof ModelResponseException modelResponseException) {
                throw modelResponseException;
            }
            throw new ModelResponseException("Failed to parse Aliyun LLM response", ex);
        }
    }

    private VoiceGuideModelResult parseContentPayload(String content) throws Exception {
        Exception lastFailure = null;
        for (String candidate : jsonCandidates(content)) {
            try {
                return mapPayload(objectMapper.readTree(removeTrailingCommas(candidate)));
            } catch (Exception ex) {
                lastFailure = ex;
            }
        }
        throw new ModelResponseException("No valid JSON object found in model content", lastFailure);
    }

    private VoiceGuideModelResult mapPayload(JsonNode payload) {
        if (payload == null || !payload.isObject()) {
            throw new ModelResponseException("Model content must be a JSON object");
        }
        VoiceGuideModelResult result = new VoiceGuideModelResult();
        result.setGuideText(readText(payload, "guideText", 4000));
        result.setFollowUpQuestion(readText(payload, "followUpQuestion", 2000));
        result.setRefinementDirection(readText(payload, "refinementDirection", 2000));
        result.setSuggestedRewrite(readText(payload, "suggestedRewrite", 8000));
        if (payload.has("completionScore") && payload.get("completionScore").canConvertToInt()) {
            result.setCompletionScore(Math.max(0, Math.min(100, payload.get("completionScore").asInt())));
        }
        if (payload.has("isComplete")) {
            result.setComplete(payload.get("isComplete").asBoolean());
        }
        JsonNode structuredNode = payload.get("structuredState");
        if (structuredNode != null && structuredNode.isObject()) {
            try {
                ExperienceStructuredState structuredState = objectMapper.convertValue(
                        structuredNode,
                        new TypeReference<ExperienceStructuredState>() {
                        }
                );
                result.setStructuredState(structuredState);
            } catch (IllegalArgumentException ex) {
                log.warn("忽略无法转换的 structuredState | reason={}", ex.getMessage());
            }
        }
        return result;
    }

    private List<String> jsonCandidates(String content) {
        String normalized = content == null ? "" : content.replace("\uFEFF", "").trim();
        Set<String> candidates = new LinkedHashSet<>();
        if (!normalized.isBlank()) {
            candidates.add(normalized);
            Matcher matcher = FENCED_JSON_PATTERN.matcher(normalized);
            while (matcher.find()) {
                candidates.add(matcher.group(1).trim());
            }
            String balancedObject = extractFirstBalancedObject(normalized);
            if (balancedObject != null) {
                candidates.add(balancedObject);
            }
        }
        return new ArrayList<>(candidates);
    }

    private String extractFirstBalancedObject(String value) {
        int start = -1;
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (ch == '\\') {
                    escaped = true;
                } else if (ch == '"') {
                    inString = false;
                }
                continue;
            }
            if (ch == '"') {
                inString = true;
            } else if (ch == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (ch == '}' && depth > 0) {
                depth--;
                if (depth == 0 && start >= 0) {
                    return value.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String removeTrailingCommas(String value) {
        return value.replaceAll(",\\s*([}\\]])", "$1");
    }

    private void validateResult(VoiceGuideModelResult result) {
        if (isBlank(result.getGuideText()) || isBlank(result.getSuggestedRewrite())) {
            throw new ModelResponseException("Model JSON is missing guideText or suggestedRewrite");
        }
    }

    private boolean isRetryable(RuntimeException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ModelResponseException || current instanceof ResourceAccessException) {
                return true;
            }
            if (current instanceof RestClientResponseException responseException) {
                int status = responseException.getStatusCode().value();
                return status == 408 || status == 429 || status >= 500;
            }
            current = current.getCause();
        }
        return false;
    }

    private void sleepBeforeRetry(long initialDelayMillis, int attempt) {
        long baseDelay = Math.max(0L, initialDelayMillis);
        long exponentialDelay = Math.min(3000L, baseDelay * (1L << Math.min(4, Math.max(0, attempt - 1))));
        long jitter = exponentialDelay == 0L ? 0L : ThreadLocalRandom.current().nextLong(Math.max(1L, exponentialDelay / 3L));
        try {
            Thread.sleep(exponentialDelay + jitter);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying Aliyun LLM request", ex);
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String readText(JsonNode node, String field, int maxLength) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.isTextual() ? value.asText(null) : null;
        if (text == null) return null;
        String normalized = text.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private static final class ModelResponseException extends IllegalStateException {
        private ModelResponseException(String message) {
            super(message);
        }

        private ModelResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
