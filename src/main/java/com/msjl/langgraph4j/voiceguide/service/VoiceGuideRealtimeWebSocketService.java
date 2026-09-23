package com.msjl.langgraph4j.voiceguide.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideResponse;
import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
public class VoiceGuideRealtimeWebSocketService {

    private static final Logger log = LoggerFactory.getLogger(VoiceGuideRealtimeWebSocketService.class);
    private static final int MAX_BUFFERED_AUDIO_BYTES = 20 * 1024 * 1024;
    private static final int STREAMING_PCM_CHUNK_BYTES = 3_200;
    private static final long STREAMING_PCM_CHUNK_DELAY_MS = 25L;

    private final VoiceGuideProperties properties;
    private final VoiceGuideOrchestrationService voiceGuideOrchestrationService;
    private final AudioTranscodingService audioTranscodingService;
    private final ObjectMapper objectMapper;
    private final AsyncTaskExecutor voiceGuideStreamTaskExecutor;
    private final ConcurrentHashMap<String, BridgeContext> contexts = new ConcurrentHashMap<>();

    public VoiceGuideRealtimeWebSocketService(
            VoiceGuideProperties properties,
            VoiceGuideOrchestrationService voiceGuideOrchestrationService,
            AudioTranscodingService audioTranscodingService,
            ObjectMapper objectMapper,
            AsyncTaskExecutor voiceGuideStreamTaskExecutor
    ) {
        this.properties = properties;
        this.voiceGuideOrchestrationService = voiceGuideOrchestrationService;
        this.audioTranscodingService = audioTranscodingService;
        this.objectMapper = objectMapper;
        this.voiceGuideStreamTaskExecutor = voiceGuideStreamTaskExecutor;
    }

    public void registerSession(WebSocketSession session) {
        contexts.put(session.getId(), new BridgeContext(
                new ConcurrentWebSocketSessionDecorator(session, 15000, 1024 * 1024)
        ));
    }

    public void startStreaming(String sessionId, VoiceGuideAudioRequest request) throws Exception {
        BridgeContext context = requireContext(sessionId);
        if (context.request != null) {
            throw new IllegalStateException("Streaming already started for session: " + sessionId);
        }
        VoiceGuideAudioRequest normalizedRequest = normalizeRequest(request);
        context.request = normalizedRequest;
        context.wavName = "voice-guide-live-" + UUID.randomUUID();
        context.audioFormat = normalizeAudioFormat(normalizedRequest.getAudioFormat());
        if (!"pcm".equals(context.audioFormat)) {
            context.bufferedAudio = new ByteArrayOutputStream();
            sendText(context.frontSession, buildStartEvent(normalizedRequest));
            log.info("语音引导使用缓冲音频模式 | sessionId={} | format={}", sessionId, context.audioFormat);
            return;
        }
        openFunAsrSocket(context, properties.getAsr().getFunasr().getMode());
        sendText(context.frontSession, buildStartEvent(normalizedRequest));
    }

    private void openFunAsrSocket(BridgeContext context, String mode) {
        context.asrMode = mode;
        context.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getAsr().getFunasr().getConnectTimeoutSeconds()))
                .build();
        context.funasrSocket = context.httpClient.newWebSocketBuilder()
                .buildAsync(
                        URI.create(properties.getAsr().getFunasr().getWebsocketUrl()),
                        new FunAsrForwardListener(context)
                )
                .join();
    }

    public void forwardAudioChunk(String sessionId, byte[] chunk) {
        BridgeContext context = requireContext(sessionId);
        if (context.bufferedAudio != null) {
            synchronized (context.bufferedAudio) {
                if (context.bufferedAudio.size() + chunk.length > MAX_BUFFERED_AUDIO_BYTES) {
                    throw new IllegalArgumentException("Buffered audio exceeds 20 MB limit");
                }
                context.bufferedAudio.writeBytes(chunk);
            }
            return;
        }
        if (context.funasrSocket == null) {
            throw new IllegalStateException("Streaming has not been started");
        }
        context.funasrSocket.sendBinary(ByteBuffer.wrap(chunk), true).join();
    }

    public void stopStreaming(String sessionId) {
        BridgeContext context = requireContext(sessionId);
        context.stopRequested = true;
        if (context.bufferedAudio != null) {
            if (context.bufferedProcessingStarted.compareAndSet(false, true)) {
                voiceGuideStreamTaskExecutor.execute(() -> processBufferedAudio(context));
            }
            return;
        }
        if (context.funasrSocket != null) {
            context.funasrSocket.sendText("{\"is_speaking\":false}", true).join();
        }
    }

    private void processBufferedAudio(BridgeContext context) {
        try {
            byte[] encodedAudio;
            synchronized (context.bufferedAudio) {
                encodedAudio = context.bufferedAudio.toByteArray();
            }
            log.info("开始转换缓冲音频 | sessionId={} | format={} | bytes={}",
                    context.frontSession.getId(), context.audioFormat, encodedAudio.length);
            byte[] pcmAudio = audioTranscodingService.toPcm16Mono16k(encodedAudio, context.audioFormat);
            log.info("缓冲音频转换完成 | sessionId={} | pcmBytes={}",
                    context.frontSession.getId(), pcmAudio.length);

            // Replay the normalized PCM through the same proven streaming mode
            // used by iOS. The deployed FunASR C++ runtime does not emit a
            // terminal result when this PCM is uploaded through offline mode.
            openFunAsrSocket(context, properties.getAsr().getFunasr().getMode());
            for (int offset = 0; offset < pcmAudio.length; offset += STREAMING_PCM_CHUNK_BYTES) {
                int length = Math.min(STREAMING_PCM_CHUNK_BYTES, pcmAudio.length - offset);
                context.funasrSocket.sendBinary(ByteBuffer.wrap(pcmAudio, offset, length), true).join();
                Thread.sleep(STREAMING_PCM_CHUNK_DELAY_MS);
            }
            context.funasrSocket.sendText("{\"is_speaking\":false}", true).join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            fail(context.frontSession.getId(), ex);
        } catch (Exception ex) {
            log.error("缓冲音频处理失败 | sessionId={} | format={}",
                    context.frontSession.getId(), context.audioFormat, ex);
            fail(context.frontSession.getId(), ex);
        }
    }

    public void sendHeartbeat(String sessionId) throws IOException {
        BridgeContext context = requireContext(sessionId);
        sendText(context.frontSession, objectMapper.createObjectNode()
                .put("type", "pong")
                .put("sessionId", sessionId)
                .put("timestamp", System.currentTimeMillis())
                .toString());
    }

    public void cleanup(String sessionId) {
        BridgeContext context = contexts.remove(sessionId);
        if (context == null) {
            return;
        }
        if (context.funasrSocket != null) {
            try {
                context.funasrSocket.sendClose(WebSocket.NORMAL_CLOSURE, "frontend closed").join();
            } catch (Exception ignored) {
            }
        }
    }

    public void fail(String sessionId, Throwable error) {
        BridgeContext context = contexts.get(sessionId);
        if (context == null) {
            return;
        }
        try {
            sendText(context.frontSession, buildErrorEvent(context, error));
        } catch (Exception ignored) {
        } finally {
            cleanup(sessionId);
        }
    }

    private VoiceGuideAudioRequest normalizeRequest(VoiceGuideAudioRequest request) {
        if (request == null || request.getExperienceType() == null) {
            throw new IllegalArgumentException("experienceType is required");
        }
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            request.setSessionId("voice-guide-ws-" + UUID.randomUUID());
        }
        return request;
    }

    private String normalizeAudioFormat(String audioFormat) {
        if (audioFormat == null || audioFormat.isBlank()) {
            return "pcm";
        }
        String normalized = audioFormat.trim().toLowerCase();
        return switch (normalized) {
            case "pcm", "s16le" -> "pcm";
            case "mp3", "mpeg" -> "mp3";
            case "aac" -> "aac";
            case "m4a", "mp4" -> "mp4";
            case "wav", "wave" -> "wav";
            default -> throw new IllegalArgumentException("Unsupported audio format: " + audioFormat);
        };
    }

    private String buildStartEvent(VoiceGuideAudioRequest request) throws Exception {
        return objectMapper.createObjectNode()
                .put("type", "started")
                .put("sessionId", request.getSessionId())
                .put("experienceType", request.getExperienceType().name())
                .put("timestamp", System.currentTimeMillis())
                .toString();
    }

    private String buildErrorEvent(BridgeContext context, Throwable error) throws Exception {
        String message = error == null ? "未知错误" : String.valueOf(error.getMessage());
        return objectMapper.createObjectNode()
                .put("type", "error")
                .put("sessionId", context.request != null ? context.request.getSessionId() : context.frontSession.getId())
                .put("message", message)
                .put("timestamp", System.currentTimeMillis())
                .toString();
    }

    private void sendText(WebSocketSession session, String payload) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(payload));
        }
    }

    private BridgeContext requireContext(String sessionId) {
        BridgeContext context = contexts.get(sessionId);
        if (context == null) {
            throw new IllegalStateException("Unknown websocket session: " + sessionId);
        }
        return context;
    }

    private String buildFunAsrInitPayload(BridgeContext context) throws Exception {
        VoiceGuideProperties.FunAsrProperties asr = properties.getAsr().getFunasr();
        String mode = context.asrMode == null || context.asrMode.isBlank() ? asr.getMode() : context.asrMode;
        ObjectNode payload = objectMapper.createObjectNode()
                .put("mode", mode)
                .put("wav_name", context.wavName)
                .put("wav_format", "pcm")
                .put("is_speaking", true)
                .put("itn", asr.isItn())
                .put("audio_fs", asr.getSampleRate());
        if (!"offline".equalsIgnoreCase(mode)) {
            payload.putArray("chunk_size")
                    .add(parseChunk(asr.getChunkSize(), 0))
                    .add(parseChunk(asr.getChunkSize(), 1))
                    .add(parseChunk(asr.getChunkSize(), 2));
            payload.put("chunk_interval", asr.getChunkInterval());
        }
        if (context.request.getHotwords() != null && !context.request.getHotwords().isBlank()) {
            payload.put("hotwords", context.request.getHotwords());
        }
        return objectMapper.writeValueAsString(payload);
    }

    private int parseChunk(String chunkSize, int idx) {
        String[] parts = chunkSize.split(",");
        if (idx >= parts.length) {
            return 5;
        }
        return Integer.parseInt(parts[idx].trim());
    }

    private void handleFunAsrMessage(BridgeContext context, JsonNode root) throws Exception {
        String mode = root.path("mode").asText("");
        String text = root.path("text").asText("");
        boolean isFinal = root.path("is_final").asBoolean(false);
        if (!text.isBlank()) {
            if ("2pass-online".equalsIgnoreCase(mode)) {
                context.onlineTranscript.append(text);
                context.currentTranscript.setLength(0);
                context.currentTranscript.append(context.committedTranscript).append(context.onlineTranscript);
            } else if ("2pass-offline".equalsIgnoreCase(mode)) {
                context.onlineTranscript.setLength(0);
                context.committedTranscript.append(text);
                context.currentTranscript.setLength(0);
                context.currentTranscript.append(context.committedTranscript);
            } else if ("online".equalsIgnoreCase(mode)) {
                context.onlineTranscript.append(text);
                context.currentTranscript.setLength(0);
                context.currentTranscript.append(context.onlineTranscript);
            } else {
                context.currentTranscript.setLength(0);
                context.currentTranscript.append(text);
            }
        }

        AsrTranscriptionResult result = new AsrTranscriptionResult();
        result.setProvider("funasr");
        result.setMode(mode);
        result.setRequestId(root.path("request_id").asText(null));
        result.setWavName(root.path("wav_name").asText(context.wavName));
        result.setText(context.currentTranscript.toString().trim());
        result.setFinalResult(isTerminal(mode, isFinal));

        if (!result.getText().isBlank()) {
            String eventType = result.getFinalResult() ? "asr_final" : "asr_partial";
            ObjectNode eventNode = objectMapper.createObjectNode()
                    .put("type", eventType)
                    .put("sessionId", context.request.getSessionId())
                    .put("text", result.getText())
                    .put("timestamp", System.currentTimeMillis());
            eventNode.set("asr", objectMapper.valueToTree(result));
            sendText(context.frontSession, eventNode.toString());
        }

        if (result.getFinalResult() && context.completed.compareAndSet(false, true)) {
            context.finalAsr = result;
            voiceGuideStreamTaskExecutor.execute(() -> generateGuideAndRespond(context));
        }
    }

    private boolean isTerminal(String mode, boolean isFinalFlag) {
        if ("offline".equalsIgnoreCase(mode)) {
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

    private void generateGuideAndRespond(BridgeContext context) {
        try {
            VoiceGuideRequest request = new VoiceGuideRequest();
            request.setSessionId(context.request.getSessionId());
            request.setResumeId(context.request.getResumeId());
            request.setTargetJobTitle(context.request.getTargetJobTitle());
            request.setExperienceType(context.request.getExperienceType());
            request.setCurrentContent(context.request.getCurrentContent());
            request.setUserLatestReply(context.finalAsr == null ? "" : context.finalAsr.getText());
            request.setGlobalInstruction(context.request.getGlobalInstruction());
            request.setConversationHistory(context.request.getConversationHistory());
            request.setIncludeAudio(context.request.getIncludeAudio());

            VoiceGuideResponse response = voiceGuideOrchestrationService.generate(request);
            response.setAsr(context.finalAsr);

            sendText(context.frontSession, objectMapper.createObjectNode()
                    .put("type", "guide_result")
                    .put("sessionId", context.request.getSessionId())
                    .put("timestamp", System.currentTimeMillis())
                    .set("response", objectMapper.valueToTree(response))
                    .toString());
            sendText(context.frontSession, objectMapper.createObjectNode()
                    .put("type", "complete")
                    .put("sessionId", context.request.getSessionId())
                    .put("timestamp", System.currentTimeMillis())
                    .toString());
        } catch (Exception ex) {
            log.error("语音转写成功但 AI 分析失败，返回可用转写文本 | sessionId={} | experienceType={}",
                    context.request == null ? context.frontSession.getId() : context.request.getSessionId(),
                    context.request == null ? null : context.request.getExperienceType(),
                    ex);
            try {
                VoiceGuideResponse degraded = buildDegradedResponse(context, ex);
                sendText(context.frontSession, objectMapper.createObjectNode()
                        .put("type", "guide_result")
                        .put("sessionId", context.request.getSessionId())
                        .put("timestamp", System.currentTimeMillis())
                        .set("response", objectMapper.valueToTree(degraded))
                        .toString());
                sendText(context.frontSession, objectMapper.createObjectNode()
                        .put("type", "complete")
                        .put("sessionId", context.request.getSessionId())
                        .put("timestamp", System.currentTimeMillis())
                        .toString());
            } catch (Exception ignored) {
                log.warn("发送 AI 分析降级结果失败 | sessionId={} | reason={}",
                        context.frontSession.getId(), ignored.getMessage());
            }
        }
    }

    private VoiceGuideResponse buildDegradedResponse(BridgeContext context, Exception error) {
        VoiceGuideResponse response = new VoiceGuideResponse();
        response.setSessionId(context.request.getSessionId());
        response.setResumeId(context.request.getResumeId());
        response.setExperienceType(context.request.getExperienceType());
        response.setAsr(context.finalAsr);
        response.setComplete(false);
        response.setDegraded(true);
        response.setWarning("语音已转写，AI 分析暂时失败，可直接使用转写文字或重新分析");
        response.setSummary("ASR succeeded but AI analysis degraded: " + safeErrorMessage(error));
        return response;
    }

    private String safeErrorMessage(Throwable error) {
        if (error == null) return "unknown";
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    private final class FunAsrForwardListener implements WebSocket.Listener {

        private final BridgeContext context;
        private final StringBuilder textFrameBuffer = new StringBuilder();
        private final CompletableFuture<Void> initFuture = new CompletableFuture<>();

        private FunAsrForwardListener(BridgeContext context) {
            this.context = context;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            try {
                webSocket.sendText(buildFunAsrInitPayload(context), true).join();
                initFuture.complete(null);
            } catch (Exception ex) {
                initFuture.completeExceptionally(ex);
            }
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            textFrameBuffer.append(data);
            if (last) {
                try {
                    initFuture.get(10, TimeUnit.SECONDS);
                    JsonNode root = objectMapper.readTree(textFrameBuffer.toString());
                    textFrameBuffer.setLength(0);
                    handleFunAsrMessage(context, root);
                } catch (Exception ex) {
                    fail(context.frontSession.getId(), ex);
                }
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            fail(context.frontSession.getId(), error);
        }
    }

    private static final class BridgeContext {
        private final WebSocketSession frontSession;
        private volatile VoiceGuideAudioRequest request;
        private volatile HttpClient httpClient;
        private volatile WebSocket funasrSocket;
        private volatile String wavName;
        private volatile String audioFormat = "pcm";
        private volatile String asrMode;
        private volatile ByteArrayOutputStream bufferedAudio;
        private final StringBuilder committedTranscript = new StringBuilder();
        private final StringBuilder onlineTranscript = new StringBuilder();
        private final StringBuilder currentTranscript = new StringBuilder();
        private final java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean(false);
        private final java.util.concurrent.atomic.AtomicBoolean bufferedProcessingStarted = new java.util.concurrent.atomic.AtomicBoolean(false);
        private volatile boolean stopRequested;
        private volatile AsrTranscriptionResult finalAsr;

        private BridgeContext(WebSocketSession frontSession) {
            this.frontSession = frontSession;
        }
    }
}
