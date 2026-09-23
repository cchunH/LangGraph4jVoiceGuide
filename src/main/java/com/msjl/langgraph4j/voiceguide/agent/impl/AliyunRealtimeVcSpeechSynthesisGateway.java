package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.agent.SpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;
import com.msjl.langgraph4j.voiceguide.domain.model.voiceclone.VoiceCloneStyleProfile;
import com.msjl.langgraph4j.voiceguide.domain.model.voiceclone.VoiceCloneVoiceEntry;
import com.msjl.langgraph4j.voiceguide.service.VoiceCloneManifestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class AliyunRealtimeVcSpeechSynthesisGateway implements SpeechSynthesisGateway {

    private static final Logger log = LoggerFactory.getLogger(AliyunRealtimeVcSpeechSynthesisGateway.class);

    private final VoiceGuideProperties properties;
    private final ObjectMapper objectMapper;
    private final VoiceCloneManifestService manifestService;
    private final HttpClient httpClient;

    public AliyunRealtimeVcSpeechSynthesisGateway(
            VoiceGuideProperties properties,
            ObjectMapper objectMapper,
            VoiceCloneManifestService manifestService
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.manifestService = manifestService;
        int timeout = properties.getTts().getAliyun().getVoiceCloneRealtime().getConnectTimeoutSeconds();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    @Override
    public VoiceGuideAudio synthesize(String text, VoiceGuideExperienceType experienceType) {
        VoiceGuideProperties.AliyunTtsProperties tts = properties.getTts().getAliyun();
        VoiceGuideProperties.VoiceCloneRealtimeProperties clone = tts.getVoiceCloneRealtime();
        if (tts.getApiKey() == null || tts.getApiKey().isBlank()) {
            throw new IllegalStateException("Aliyun TTS apiKey is missing");
        }
        VoiceCloneVoiceEntry voiceEntry = manifestService.resolveVoice(experienceType)
                .orElseThrow(() -> new IllegalStateException("No active VC-Realtime voice configured for " + experienceType));

        String websocketUrl = clone.getWebsocketUrl() + "?model=" + clone.getModel();
        RealtimeSynthesisCollector collector = new RealtimeSynthesisCollector(objectMapper);
        try {
            WebSocket webSocket = httpClient.newWebSocketBuilder()
                    .header("Authorization", "Bearer " + tts.getApiKey())
                    .buildAsync(URI.create(websocketUrl), collector)
                    .join();

            sendJson(webSocket, buildSessionUpdate(tts, clone, voiceEntry));
            sendJson(webSocket, buildAppendText(text));
            if ("commit".equalsIgnoreCase(clone.getMode())) {
                sendJson(webSocket, "{\"type\":\"input_text_buffer.commit\"}");
            }
            sendJson(webSocket, "{\"type\":\"session.finish\"}");

            collector.await(clone.getResponseTimeoutSeconds());
            byte[] pcmBytes = collector.audioBytes();
            if (pcmBytes.length == 0) {
                throw new IllegalStateException("Aliyun VC-Realtime returned empty audio stream");
            }
            byte[] wavBytes = wrapPcmAsWav(pcmBytes, clone.getSampleRate());

            VoiceGuideAudio audio = new VoiceGuideAudio();
            audio.setProvider("aliyun-vc-realtime");
            audio.setVoice(voiceEntry.getRemoteVoiceId());
            audio.setFormat("wav");
            audio.setContentType("audio/wav");
            audio.setRequestId(firstNonBlank(collector.requestId(), collector.responseId()));
            audio.setAudioId(firstNonBlank(collector.responseId(), collector.sessionId()));
            audio.setAudioBase64(Base64.getEncoder().encodeToString(wavBytes));
            return audio;
        } catch (Exception ex) {
            throw new IllegalStateException("Aliyun VC-Realtime synthesis failed", ex);
        }
    }

    private String buildSessionUpdate(
            VoiceGuideProperties.AliyunTtsProperties tts,
            VoiceGuideProperties.VoiceCloneRealtimeProperties clone,
            VoiceCloneVoiceEntry voiceEntry
    ) throws IOException {
        VoiceCloneStyleProfile profile = voiceEntry.getStyleProfile();
        double speechRate = profile.getSpeechRate() != null ? profile.getSpeechRate() : toRealtimeMultiplier(tts.getSpeechRate());
        double pitchRate = profile.getPitchRate() != null ? profile.getPitchRate() : toRealtimeMultiplier(tts.getPitchRate());
        int volume = profile.getVolume() != null ? profile.getVolume() : (tts.getVolume() == null ? 50 : tts.getVolume());
        int sampleRate = profile.getSampleRate() != null ? profile.getSampleRate() : clone.getSampleRate();
        String responseFormat = profile.getResponseFormat() != null ? profile.getResponseFormat() : clone.getResponseFormat();

        JsonNode node = objectMapper.createObjectNode()
                .put("type", "session.update")
                .set("session", objectMapper.createObjectNode()
                        .put("voice", voiceEntry.getRemoteVoiceId())
                        .put("mode", clone.getMode())
                        .put("language_type", tts.getLanguageType())
                        .put("response_format", responseFormat)
                        .put("sample_rate", sampleRate)
                        .put("speech_rate", speechRate)
                        .put("pitch_rate", pitchRate)
                        .put("volume", volume));
        return objectMapper.writeValueAsString(node);
    }

    private String buildAppendText(String text) {
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"type\":\"input_text_buffer.append\",\"text\":\"" + escaped + "\"}";
    }

    private void sendJson(WebSocket webSocket, String payload) {
        webSocket.sendText(payload, true).join();
    }

    private double toRealtimeMultiplier(Integer styleValue) {
        if (styleValue == null) {
            return 1.0D;
        }
        double candidate = 1.0D + (styleValue * 0.03D);
        if (candidate < 0.5D) {
            return 0.5D;
        }
        return Math.min(candidate, 2.0D);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private byte[] wrapPcmAsWav(byte[] pcmData, int sampleRate) {
        int channels = 1;
        int bitsPerSample = 16;
        int byteRate = sampleRate * channels * bitsPerSample / 8;
        int blockAlign = channels * bitsPerSample / 8;
        int dataSize = pcmData.length;
        int chunkSize = 36 + dataSize;

        ByteBuffer buffer = ByteBuffer.allocate(44 + dataSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(chunkSize);
        buffer.put("WAVE".getBytes(StandardCharsets.US_ASCII));
        buffer.put("fmt ".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(16);
        buffer.putShort((short) 1);
        buffer.putShort((short) channels);
        buffer.putInt(sampleRate);
        buffer.putInt(byteRate);
        buffer.putShort((short) blockAlign);
        buffer.putShort((short) bitsPerSample);
        buffer.put("data".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(dataSize);
        buffer.put(pcmData);
        return buffer.array();
    }

    private static final class RealtimeSynthesisCollector implements WebSocket.Listener {

        private final ObjectMapper objectMapper;
        private final StringBuilder textBuffer = new StringBuilder();
        private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
        private final CompletableFuture<Void> completed = new CompletableFuture<>();

        private String requestId;
        private String responseId;
        private String sessionId;

        private RealtimeSynthesisCollector(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                String payload = textBuffer.toString();
                textBuffer.setLength(0);
                try {
                    JsonNode root = objectMapper.readTree(payload);
                    String type = root.path("type").asText("");
                    if ("error".equalsIgnoreCase(type)) {
                        completed.completeExceptionally(new IllegalStateException(payload));
                    } else {
                        requestId = firstNonBlank(requestId, root.path("request_id").asText(null));
                        sessionId = firstNonBlank(sessionId, root.path("session_id").asText(null));
                        responseId = firstNonBlank(responseId,
                                root.path("response_id").asText(null),
                                root.path("id").asText(null));

                        JsonNode delta = root.path("delta");
                        if (!delta.isMissingNode() && delta.hasNonNull("audio")) {
                            byte[] chunk = Base64.getDecoder().decode(delta.path("audio").asText());
                            audioBuffer.write(chunk);
                        }
                        if ("response.audio.delta".equals(type) && root.hasNonNull("audio")) {
                            byte[] chunk = Base64.getDecoder().decode(root.path("audio").asText());
                            audioBuffer.write(chunk);
                        }
                        if ("session.finished".equals(type)
                                || "response.done".equals(type)
                                || "response.completed".equals(type)) {
                            completed.complete(null);
                        }
                    }
                } catch (Exception ex) {
                    completed.completeExceptionally(ex);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            audioBuffer.writeBytes(bytes);
            if (last) {
                completed.complete(null);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            completed.completeExceptionally(error);
        }

        private void await(int timeoutSeconds) throws Exception {
            completed.get(timeoutSeconds, TimeUnit.SECONDS);
        }

        private byte[] audioBytes() {
            return audioBuffer.toByteArray();
        }

        private String requestId() {
            return requestId;
        }

        private String responseId() {
            return responseId;
        }

        private String sessionId() {
            return sessionId;
        }

        private String firstNonBlank(String current, String... candidates) {
            if (current != null && !current.isBlank()) {
                return current;
            }
            for (String candidate : candidates) {
                if (candidate != null && !candidate.isBlank()) {
                    return candidate;
                }
            }
            return null;
        }
    }
}
