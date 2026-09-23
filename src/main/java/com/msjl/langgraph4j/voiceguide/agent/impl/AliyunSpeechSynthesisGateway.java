package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.agent.SpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class AliyunSpeechSynthesisGateway implements SpeechSynthesisGateway {

    private static final Logger log = LoggerFactory.getLogger(AliyunSpeechSynthesisGateway.class);

    private final RestClient restClient;
    private final VoiceGuideProperties properties;
    private final ObjectMapper objectMapper;

    public AliyunSpeechSynthesisGateway(
            RestClient restClient,
            VoiceGuideProperties properties,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public VoiceGuideAudio synthesize(String text, VoiceGuideExperienceType experienceType) {
        VoiceGuideProperties.AliyunTtsProperties tts = properties.getTts().getAliyun();
        if (tts.getApiKey() == null || tts.getApiKey().isBlank()) {
            throw new IllegalStateException("Aliyun TTS apiKey is missing");
        }
        VoiceGuideProperties.VoiceStyle style = resolveStyle(tts, experienceType);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("text", text);
        input.put("voice", tts.getVoice());
        input.put("language_type", tts.getLanguageType());

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", tts.getModel());
        request.put("input", input);
        request.put("parameters", buildParameters(tts, style));

        log.info("调用阿里云 TTS | experienceType={} | model={} | voice={} | speechRate={} | pitchRate={} | volume={}",
                experienceType, tts.getModel(), tts.getVoice(), style.getSpeechRate(), style.getPitchRate(), style.getVolume());

        String responseBody = restClient.post()
                .uri(tts.getEndpointPath())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tts.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        return parseAudio(responseBody, tts);
    }

    private VoiceGuideAudio parseAudio(String responseBody, VoiceGuideProperties.AliyunTtsProperties tts) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            VoiceGuideAudio audio = new VoiceGuideAudio();
            audio.setProvider("aliyun");
            audio.setVoice(tts.getVoice());
            audio.setFormat(tts.getFormat());
            audio.setContentType("audio/" + tts.getFormat());
            audio.setRequestId(firstNonBlank(
                    root.path("request_id").asText(null),
                    root.at("/output/task_id").asText(null),
                    root.path("task_id").asText(null)
            ));
            audio.setAudioId(firstNonBlank(
                    root.at("/output/audio/id").asText(null),
                    root.at("/data/audio/id").asText(null)
            ));
            audio.setAudioUrl(firstNonBlank(
                    root.at("/output/audio/url").asText(null),
                    root.at("/data/audio/url").asText(null)
            ));
            if (root.at("/output/audio/expires_at").isNumber()) {
                audio.setExpiresAt(root.at("/output/audio/expires_at").asLong());
            } else if (root.at("/data/audio/expires_at").isNumber()) {
                audio.setExpiresAt(root.at("/data/audio/expires_at").asLong());
            }
            audio.setAudioBase64(firstNonBlank(
                    root.at("/output/audio/base64").asText(null),
                    root.at("/output/audio/data").asText(null),
                    root.at("/output/audio").asText(null),
                    root.at("/data/audio/base64").asText(null),
                    root.path("audio").asText(null)
            ));
            if ((audio.getAudioBase64() == null || audio.getAudioBase64().isBlank())
                    && audio.getAudioUrl() != null
                    && !audio.getAudioUrl().isBlank()) {
                audio.setAudioBase64(downloadAsBase64(audio.getAudioUrl()));
            }
            return audio;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to parse Aliyun TTS response", ex);
        }
    }

    private Map<String, Object> buildParameters(
            VoiceGuideProperties.AliyunTtsProperties tts,
            VoiceGuideProperties.VoiceStyle style
    ) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (tts.getModel() != null && tts.getModel().contains("instruct")) {
            parameters.put("instructions", buildInstructions(style));
            parameters.put("optimize_instructions", tts.isOptimizeInstructions());
        }
        return parameters;
    }

    private String buildInstructions(VoiceGuideProperties.VoiceStyle style) {
        StringBuilder builder = new StringBuilder("请使用亲切自然、轻盈但像真人助手的口吻进行播报");
        if (style.getSpeechRate() != null) {
            if (style.getSpeechRate() >= 8) {
                builder.append("，语速偏快");
            } else if (style.getSpeechRate() <= -5) {
                builder.append("，语速偏慢");
            } else {
                builder.append("，语速自然");
            }
        }
        if (style.getPitchRate() != null) {
            if (style.getPitchRate() >= 10) {
                builder.append("，音调略高、更轻盈");
            } else if (style.getPitchRate() <= -5) {
                builder.append("，音调偏低、更稳");
            } else {
                builder.append("，音调自然");
            }
        }
        if (style.getVolume() != null) {
            if (style.getVolume() >= 60) {
                builder.append("，音量更有活力");
            } else if (style.getVolume() <= 40) {
                builder.append("，音量柔和");
            } else {
                builder.append("，音量适中");
            }
        }
        builder.append("，断句清晰，避免夸张表演。");
        return builder.toString();
    }

    private String downloadAsBase64(String audioUrl) {
        try {
            byte[] audioBytes = restClient.get()
                    .uri(URI.create(audioUrl))
                    .retrieve()
                    .body(byte[].class);
            if (audioBytes == null || audioBytes.length == 0) {
                return null;
            }
            return Base64.getEncoder().encodeToString(audioBytes);
        } catch (Exception ex) {
            log.warn("下载阿里云 TTS 音频失败 | url={} | reason={}", audioUrl, ex.getMessage());
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private VoiceGuideProperties.VoiceStyle resolveStyle(
            VoiceGuideProperties.AliyunTtsProperties tts,
            VoiceGuideExperienceType experienceType
    ) {
        VoiceGuideProperties.ExperienceVoiceProfiles profiles = tts.getProfiles();
        VoiceGuideProperties.VoiceStyle profileStyle;
        if (profiles == null) {
            profileStyle = null;
        } else {
            profileStyle = switch (experienceType) {
                case EDUCATION -> profiles.getEducation();
                case INTERNSHIP_EXPERIENCE -> profiles.getInternshipExperience();
                case WORK_EXPERIENCE -> profiles.getWorkExperience();
                case PROJECT_EXPERIENCE -> profiles.getProjectExperience();
            };
        }

        VoiceGuideProperties.VoiceStyle fallback = new VoiceGuideProperties.VoiceStyle(
                tts.getSpeechRate(),
                tts.getPitchRate(),
                tts.getVolume()
        );
        if (profileStyle == null) {
            return fallback;
        }
        return new VoiceGuideProperties.VoiceStyle(
                profileStyle.getSpeechRate() != null ? profileStyle.getSpeechRate() : fallback.getSpeechRate(),
                profileStyle.getPitchRate() != null ? profileStyle.getPitchRate() : fallback.getPitchRate(),
                profileStyle.getVolume() != null ? profileStyle.getVolume() : fallback.getVolume()
        );
    }
}
