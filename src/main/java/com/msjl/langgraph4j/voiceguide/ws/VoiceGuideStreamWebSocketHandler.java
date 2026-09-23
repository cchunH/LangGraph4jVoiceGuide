package com.msjl.langgraph4j.voiceguide.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideRealtimeWebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.nio.ByteBuffer;

@Component
public class VoiceGuideStreamWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(VoiceGuideStreamWebSocketHandler.class);

    private final ObjectMapper objectMapper;
    private final VoiceGuideRealtimeWebSocketService realtimeWebSocketService;

    public VoiceGuideStreamWebSocketHandler(
            ObjectMapper objectMapper,
            VoiceGuideRealtimeWebSocketService realtimeWebSocketService
    ) {
        this.objectMapper = objectMapper;
        this.realtimeWebSocketService = realtimeWebSocketService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("语音引导 WebSocket 已连接 | sessionId={}", session.getId());
        realtimeWebSocketService.registerSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = objectMapper.readTree(message.getPayload());
            String type = root.path("type").asText("");
            if ("start".equalsIgnoreCase(type)) {
                JsonNode requestNode = root.path("request");
                VoiceGuideAudioRequest request = objectMapper.treeToValue(requestNode, VoiceGuideAudioRequest.class);
                realtimeWebSocketService.startStreaming(session.getId(), request);
                return;
            }
            if ("stop".equalsIgnoreCase(type)) {
                realtimeWebSocketService.stopStreaming(session.getId());
                return;
            }
            if ("ping".equalsIgnoreCase(type)) {
                realtimeWebSocketService.sendHeartbeat(session.getId());
                return;
            }
            throw new IllegalArgumentException("Unsupported websocket message type: " + type);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to process websocket text message", ex);
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        realtimeWebSocketService.forwardAudioChunk(session.getId(), bytes);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("语音引导 WebSocket 已关闭 | sessionId={} | code={} | reason={}",
                session.getId(), status.getCode(), status.getReason());
        realtimeWebSocketService.cleanup(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("语音引导 WebSocket 传输异常 | sessionId={}", session.getId(), exception);
        realtimeWebSocketService.fail(session.getId(), exception);
    }
}
