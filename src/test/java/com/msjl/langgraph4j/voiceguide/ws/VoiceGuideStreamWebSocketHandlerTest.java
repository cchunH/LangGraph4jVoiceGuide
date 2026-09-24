package com.msjl.langgraph4j.voiceguide.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideRealtimeWebSocketService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoiceGuideStreamWebSocketHandlerTest {

    @Test
    void invalidStartIsReportedAsStructuredServiceErrorInsteadOfTransportCrash() throws Exception {
        VoiceGuideRealtimeWebSocketService service = mock(VoiceGuideRealtimeWebSocketService.class);
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("invalid-start-session");
        doThrow(new IllegalArgumentException("invalid request"))
                .when(service).startStreaming(eq("invalid-start-session"), any(VoiceGuideAudioRequest.class));
        VoiceGuideStreamWebSocketHandler handler = new VoiceGuideStreamWebSocketHandler(new ObjectMapper(), service);

        assertDoesNotThrow(() -> handler.handleTextMessage(session,
                new TextMessage("{\"type\":\"start\",\"request\":{}}")));

        verify(service).fail(eq("invalid-start-session"), any(IllegalArgumentException.class));
    }
}
