package com.msjl.langgraph4j.voiceguide;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideResponse;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.service.AudioTranscodingService;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideOrchestrationService;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideRealtimeWebSocketService;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VoiceGuideRealtimeAsrCompletionTest {

    @Test
    void transcriptionOnlyCompletesWithoutStartingGuideAnalysis() throws Exception {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("ws-transcription-only-test");
        when(socket.isOpen()).thenReturn(true);
        VoiceGuideOrchestrationService orchestration = mock(VoiceGuideOrchestrationService.class);
        AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
        VoiceGuideRealtimeWebSocketService service = new VoiceGuideRealtimeWebSocketService(
                new VoiceGuideProperties(), orchestration, mock(AudioTranscodingService.class),
                new ObjectMapper(), executor);
        service.registerSession(socket);
        @SuppressWarnings("unchecked")
        Map<String, Object> contexts = (Map<String, Object>) ReflectionTestUtils.getField(service, "contexts");
        Object context = contexts.get(socket.getId());
        VoiceGuideAudioRequest request = new ObjectMapper().readValue(
                "{\"sessionId\":\"mock-asr-test\",\"responseMode\":\"transcription\",\"experienceType\":\"EDUCATION\"}",
                VoiceGuideAudioRequest.class);
        ReflectionTestUtils.setField(context, "request", request);

        // A segment is only terminal after the client explicitly stops recording.
        ReflectionTestUtils.setField(context, "asrStopSent", true);
        ReflectionTestUtils.invokeMethod(service, "handleFunAsrMessage", context,
                new ObjectMapper().readTree("{\"mode\":\"2pass-offline\",\"text\":\"我在清华大学读本科\",\"is_final\":true}"));

        org.mockito.ArgumentCaptor<org.springframework.web.socket.WebSocketMessage> messages =
                org.mockito.ArgumentCaptor.forClass(org.springframework.web.socket.WebSocketMessage.class);
        verify(socket, org.mockito.Mockito.atLeast(2)).sendMessage(messages.capture());
        assertTrue(messages.getAllValues().stream().map(message -> ((TextMessage) message).getPayload())
                .anyMatch(payload -> payload.contains("\"type\":\"asr_final\"")));
        assertTrue(messages.getAllValues().stream().map(message -> ((TextMessage) message).getPayload())
                .anyMatch(payload -> payload.contains("\"type\":\"complete\"")));
        verify(executor, never()).execute(any(Runnable.class));
        verify(orchestration, never()).generate(any(VoiceGuideRequest.class));
        service.cleanup(socket.getId());
    }

    @Test
    void stoppedTwoPassSessionCompletesFromRecognizedTextWithoutIsFinalFlag() throws Exception {
        WebSocketSession socket = mock(WebSocketSession.class);
        when(socket.getId()).thenReturn("ws-asr-final-test");
        when(socket.isOpen()).thenReturn(true);

        VoiceGuideOrchestrationService orchestration = mock(VoiceGuideOrchestrationService.class);
        CountDownLatch analyzed = new CountDownLatch(1);
        VoiceGuideResponse response = new VoiceGuideResponse();
        response.setSuggestedRewrite("本科就读于清华大学");
        when(orchestration.generate(any(VoiceGuideRequest.class))).thenAnswer(invocation -> {
            analyzed.countDown();
            return response;
        });
        AsyncTaskExecutor executor = mock(AsyncTaskExecutor.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(executor).execute(any(Runnable.class));

        VoiceGuideRealtimeWebSocketService service = new VoiceGuideRealtimeWebSocketService(
                new VoiceGuideProperties(), orchestration, mock(AudioTranscodingService.class),
                new ObjectMapper(), executor);
        service.registerSession(socket);
        @SuppressWarnings("unchecked")
        Map<String, Object> contexts = (Map<String, Object>) ReflectionTestUtils.getField(service, "contexts");
        Object context = contexts.get(socket.getId());
        VoiceGuideAudioRequest request = new VoiceGuideAudioRequest();
        request.setSessionId("resume-asr-test");
        request.setExperienceType(VoiceGuideExperienceType.EDUCATION);
        request.setIncludeAudio(false);
        ReflectionTestUtils.setField(context, "request", request);

        // FunASR 2pass emits usable offline text before stop, but no is_final=true.
        ReflectionTestUtils.invokeMethod(service, "handleFunAsrMessage", context,
                new ObjectMapper().readTree("{\"mode\":\"2pass-offline\",\"text\":\"我在清华大学读本科\",\"is_final\":false}"));
        ReflectionTestUtils.invokeMethod(service, "onAsrStopSent", context);

        assertTrue(analyzed.await(3, TimeUnit.SECONDS));
        org.mockito.ArgumentCaptor<VoiceGuideRequest> requestCaptor =
                org.mockito.ArgumentCaptor.forClass(VoiceGuideRequest.class);
        verify(orchestration).generate(requestCaptor.capture());
        assertEquals("我在清华大学读本科", requestCaptor.getValue().getUserLatestReply());
        assertEquals(false, requestCaptor.getValue().getIncludeAudio());
        org.mockito.ArgumentCaptor<org.springframework.web.socket.WebSocketMessage> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.web.socket.WebSocketMessage.class);
        verify(socket, org.mockito.Mockito.timeout(2_000).atLeast(3)).sendMessage(messageCaptor.capture());
        assertTrue(messageCaptor.getAllValues().stream().map(message -> ((TextMessage) message).getPayload())
                .anyMatch(payload -> payload.contains("\"type\":\"asr_final\"")));
        assertTrue(messageCaptor.getAllValues().stream().map(message -> ((TextMessage) message).getPayload())
                .anyMatch(payload -> payload.contains("\"type\":\"guide_result\"")));
        service.cleanup(socket.getId());
    }
}
