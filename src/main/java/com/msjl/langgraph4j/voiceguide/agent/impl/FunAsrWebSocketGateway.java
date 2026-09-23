package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideAsrListener;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideAsrGateway;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

public class FunAsrWebSocketGateway implements VoiceGuideAsrGateway {

    private static final Logger log = LoggerFactory.getLogger(FunAsrWebSocketGateway.class);

    private final VoiceGuideProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public FunAsrWebSocketGateway(VoiceGuideProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        int timeout = properties.getAsr().getFunasr().getConnectTimeoutSeconds();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .build();
    }

    @Override
    public AsrTranscriptionResult transcribe(
            byte[] audioBytes,
            String originalFilename,
            String contentType,
            String hotwords,
            VoiceGuideAsrListener listener
    ) {
        VoiceGuideProperties.FunAsrProperties asr = properties.getAsr().getFunasr();
        String wavName = buildWavName(originalFilename, asr.getWavNamePrefix());
        ResultCollector collector = new ResultCollector(
                objectMapper,
                "offline".equalsIgnoreCase(asr.getMode()),
                listener
        );

        try {
            PreparedAudio preparedAudio = prepareAudio(audioBytes, originalFilename, contentType, asr);
            WebSocket webSocket = httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(asr.getWebsocketUrl()), collector)
                    .join();

            sendJson(webSocket, buildInitMessage(asr, wavName, preparedAudio.format(), hotwords));
            sendAudio(webSocket, preparedAudio.bytes(), asr);
            sendJson(webSocket, "{\"is_speaking\":false}");

            AsrTranscriptionResult result = collector.await(asr.getResponseTimeoutSeconds());
            result.setProvider("funasr");
            if (result.getMode() == null || result.getMode().isBlank()) {
                result.setMode(asr.getMode());
            }
            result.setWavName(wavName);
            return result;
        } catch (Exception ex) {
            if (listener != null) {
                listener.onError(ex);
            }
            throw new IllegalStateException("FunASR transcription failed", ex);
        }
    }

    private String buildInitMessage(
            VoiceGuideProperties.FunAsrProperties asr,
            String wavName,
            String wavFormat,
            String hotwords
    ) throws Exception {
        com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        root.put("mode", asr.getMode());
        root.put("wav_name", wavName);
        root.put("wav_format", wavFormat);
        root.put("is_speaking", true);
        root.put("itn", asr.isItn());
        if ("pcm".equalsIgnoreCase(wavFormat)) {
            root.put("audio_fs", asr.getSampleRate());
        }
        if (hotwords != null && !hotwords.isBlank()) {
            root.put("hotwords", hotwords);
        }
        if (!"offline".equalsIgnoreCase(asr.getMode())) {
            root.putArray("chunk_size")
                    .add(parseChunk(asr.getChunkSize(), 0))
                    .add(parseChunk(asr.getChunkSize(), 1))
                    .add(parseChunk(asr.getChunkSize(), 2));
            root.put("chunk_interval", asr.getChunkInterval());
        }
        return objectMapper.writeValueAsString(root);
    }

    private PreparedAudio prepareAudio(
            byte[] audioBytes,
            String originalFilename,
            String contentType,
            VoiceGuideProperties.FunAsrProperties asr
    ) {
        String suffix = resolveSuffix(originalFilename, contentType);
        if ("offline".equalsIgnoreCase(asr.getMode())) {
            return new PreparedAudio(audioBytes, suffix);
        }
        if ("wav".equalsIgnoreCase(suffix)) {
            if (audioBytes.length <= 44) {
                throw new IllegalArgumentException("WAV audio bytes are too short for streaming 2pass mode");
            }
            byte[] pcmBytes = new byte[audioBytes.length - 44];
            System.arraycopy(audioBytes, 44, pcmBytes, 0, pcmBytes.length);
            return new PreparedAudio(pcmBytes, "pcm");
        }
        if ("pcm".equalsIgnoreCase(suffix)) {
            return new PreparedAudio(audioBytes, "pcm");
        }
        throw new IllegalArgumentException("Streaming FunASR only supports PCM payloads or WAV files convertible to PCM");
    }

    private void sendAudio(WebSocket webSocket, byte[] audioBytes, VoiceGuideProperties.FunAsrProperties asr) throws Exception {
        if ("offline".equalsIgnoreCase(asr.getMode())) {
            webSocket.sendBinary(ByteBuffer.wrap(audioBytes), true).join();
            return;
        }
        int[] chunk = parseChunkSize(asr.getChunkSize());
        int stride = (int) (60D * chunk[1] / asr.getChunkInterval() / 1000D * asr.getSampleRate() * 2);
        if (stride <= 0) {
            stride = Math.min(audioBytes.length, 1920);
        }
        long sleepMillis = Math.max(1L, Math.round(60D * chunk[1] / asr.getChunkInterval()));
        for (int offset = 0; offset < audioBytes.length; offset += stride) {
            int size = Math.min(stride, audioBytes.length - offset);
            webSocket.sendBinary(ByteBuffer.wrap(audioBytes, offset, size), true).join();
            TimeUnit.MILLISECONDS.sleep(sleepMillis);
        }
    }

    private int parseChunk(String chunkSize, int idx) {
        String[] parts = chunkSize.split(",");
        if (idx >= parts.length) {
            return 5;
        }
        return Integer.parseInt(parts[idx].trim());
    }

    private int[] parseChunkSize(String chunkSize) {
        return new int[]{
                parseChunk(chunkSize, 0),
                parseChunk(chunkSize, 1),
                parseChunk(chunkSize, 2)
        };
    }

    private String buildWavName(String originalFilename, String prefix) {
        String baseName = originalFilename == null || originalFilename.isBlank() ? "audio" : originalFilename;
        return prefix + "-" + UUID.randomUUID() + "-" + baseName;
    }

    private String resolveSuffix(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (contentType == null) {
            return "wav";
        }
        if (contentType.contains("wav")) {
            return "wav";
        }
        if (contentType.contains("mpeg") || contentType.contains("mp3")) {
            return "mp3";
        }
        if (contentType.contains("pcm")) {
            return "pcm";
        }
        return "wav";
    }

    private void sendJson(WebSocket webSocket, String json) {
        log.info("发送 FunASR 控制消息 | payload={}", json);
        webSocket.sendText(json, true).join();
    }

    private static final class ResultCollector implements WebSocket.Listener {

        private final ObjectMapper objectMapper;
        private final boolean offlineMode;
        private final VoiceGuideAsrListener listener;
        private final StringBuilder textBuffer = new StringBuilder();
        private final CompletableFuture<AsrTranscriptionResult> completed = new CompletableFuture<>();
        private final StringBuilder transcript = new StringBuilder();
        private final StringBuilder committedTranscript = new StringBuilder();
        private final StringBuilder onlineTranscript = new StringBuilder();
        private String requestId;
        private boolean sawFinalFlag = false;

        private ResultCollector(ObjectMapper objectMapper, boolean offlineMode, VoiceGuideAsrListener listener) {
            this.objectMapper = objectMapper;
            this.offlineMode = offlineMode;
            this.listener = listener;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                try {
                    JsonNode root = objectMapper.readTree(textBuffer.toString());
                    textBuffer.setLength(0);
                    String mode = root.path("mode").asText("");
                    String text = root.path("text").asText("");
                    if (!text.isBlank()) {
                        updateTranscript(mode, text);
                    }
                    requestId = firstNonBlank(requestId,
                            root.path("wav_name").asText(null),
                            root.path("request_id").asText(null));
                    sawFinalFlag = root.path("is_final").asBoolean(false);
                    if (!text.isBlank()) {
                        AsrTranscriptionResult current = new AsrTranscriptionResult();
                        current.setMode(mode);
                        current.setText(transcript.toString().trim());
                        current.setFinalResult(isTerminal(mode, sawFinalFlag));
                        current.setRequestId(requestId);
                        if (listener != null) {
                            listener.onResult(current);
                        }
                        if (current.getFinalResult() && !completed.isDone()) {
                            completed.complete(current);
                        }
                    } else if (offlineMode && sawFinalFlag && !completed.isDone()) {
                        AsrTranscriptionResult result = new AsrTranscriptionResult();
                        result.setMode(mode);
                        result.setText(transcript.toString().trim());
                        result.setFinalResult(true);
                        result.setRequestId(requestId);
                        completed.complete(result);
                    }
                } catch (Exception ex) {
                    if (listener != null) {
                        listener.onError(ex);
                    }
                    completed.completeExceptionally(ex);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            if (listener != null) {
                listener.onError(error);
            }
            completed.completeExceptionally(error);
        }

        public AsrTranscriptionResult await(int timeoutSeconds) throws Exception {
            return completed.get(timeoutSeconds, TimeUnit.SECONDS);
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

        private void updateTranscript(String mode, String text) {
            if (offlineMode || "offline".equalsIgnoreCase(mode)) {
                transcript.setLength(0);
                transcript.append(text);
                return;
            }
            if ("online".equalsIgnoreCase(mode)) {
                onlineTranscript.append(text);
                transcript.setLength(0);
                transcript.append(onlineTranscript);
                return;
            }
            if ("2pass-online".equalsIgnoreCase(mode)) {
                onlineTranscript.append(text);
                transcript.setLength(0);
                transcript.append(committedTranscript).append(onlineTranscript);
                return;
            }
            if ("2pass-offline".equalsIgnoreCase(mode)) {
                onlineTranscript.setLength(0);
                committedTranscript.append(text);
                transcript.setLength(0);
                transcript.append(committedTranscript);
                return;
            }
            transcript.setLength(0);
            transcript.append(text);
        }

        private boolean isTerminal(String mode, boolean isFinalFlag) {
            if (offlineMode || "offline".equalsIgnoreCase(mode)) {
                return true;
            }
            if ("2pass-offline".equalsIgnoreCase(mode)) {
                return isFinalFlag;
            }
            if ("online".equalsIgnoreCase(mode)) {
                return isFinalFlag;
            }
            return false;
        }
    }

    private record PreparedAudio(byte[] bytes, String format) {
    }
}
