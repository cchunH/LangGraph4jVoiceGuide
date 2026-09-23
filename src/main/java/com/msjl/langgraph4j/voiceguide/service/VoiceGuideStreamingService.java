package com.msjl.langgraph4j.voiceguide.service;

import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideAsrGateway;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideAsrListener;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideResponse;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideStreamEvent;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideStreamStartResponse;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideStreamEventType;
import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@Service
public class VoiceGuideStreamingService {

    private static final Logger log = LoggerFactory.getLogger(VoiceGuideStreamingService.class);

    private final VoiceGuideAsrGateway voiceGuideAsrGateway;
    private final VoiceGuideOrchestrationService voiceGuideOrchestrationService;
    private final VoiceGuideStreamSessionService streamSessionService;
    private final AsyncTaskExecutor voiceGuideStreamTaskExecutor;

    public VoiceGuideStreamingService(
            VoiceGuideAsrGateway voiceGuideAsrGateway,
            VoiceGuideOrchestrationService voiceGuideOrchestrationService,
            VoiceGuideStreamSessionService streamSessionService,
            AsyncTaskExecutor voiceGuideStreamTaskExecutor
    ) {
        this.voiceGuideAsrGateway = voiceGuideAsrGateway;
        this.voiceGuideOrchestrationService = voiceGuideOrchestrationService;
        this.streamSessionService = streamSessionService;
        this.voiceGuideStreamTaskExecutor = voiceGuideStreamTaskExecutor;
    }

    public VoiceGuideStreamStartResponse startStream(
            VoiceGuideAudioRequest request,
            byte[] audioBytes,
            String originalFilename,
            String contentType
    ) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = "voice-guide-stream-" + UUID.randomUUID();
            request.setSessionId(sessionId);
        }

        publish(sessionId, buildEvent(
                sessionId,
                request,
                VoiceGuideStreamEventType.STARTED,
                "流式语音引导任务已开始",
                null,
                null,
                null
        ));

        String finalSessionId = sessionId;
        voiceGuideStreamTaskExecutor.execute(() -> processStream(
                finalSessionId,
                request,
                audioBytes,
                originalFilename,
                contentType
        ));

        VoiceGuideStreamStartResponse response = new VoiceGuideStreamStartResponse();
        response.setSessionId(sessionId);
        response.setStreamPath("/api/langgraph4j/voice-guidance/stream/" + sessionId + "/events");
        response.setStatus("accepted");
        return response;
    }

    public SseEmitter subscribe(String sessionId) {
        return streamSessionService.subscribe(sessionId);
    }

    void processStream(
            String sessionId,
            VoiceGuideAudioRequest request,
            byte[] audioBytes,
            String originalFilename,
            String contentType
    ) {
        AsrTranscriptionResult completedAsr = null;
        try {
            AsrTranscriptionResult asrResult = voiceGuideAsrGateway.transcribe(
                    audioBytes,
                    originalFilename,
                    contentType,
                    request.getHotwords(),
                    new VoiceGuideAsrListener() {
                        @Override
                        public void onResult(AsrTranscriptionResult result) {
                            VoiceGuideStreamEventType type = isFinalAsrEvent(result)
                                    ? VoiceGuideStreamEventType.ASR_FINAL
                                    : VoiceGuideStreamEventType.ASR_PARTIAL;
                            publish(sessionId, buildEvent(
                                    sessionId,
                                    request,
                                    type,
                                    null,
                                    result.getText(),
                                    result,
                                    null
                            ));
                        }

                        @Override
                        public void onError(Throwable error) {
                            publish(sessionId, buildEvent(
                                    sessionId,
                                    request,
                                    VoiceGuideStreamEventType.ERROR,
                                    error.getMessage(),
                                    null,
                                    null,
                                    null
                            ));
                        }
                    }
            );
            completedAsr = asrResult;

            VoiceGuideRequest textRequest = new VoiceGuideRequest();
            textRequest.setSessionId(request.getSessionId());
            textRequest.setResumeId(request.getResumeId());
            textRequest.setTargetJobTitle(request.getTargetJobTitle());
            textRequest.setExperienceType(request.getExperienceType());
            textRequest.setCurrentContent(request.getCurrentContent());
            textRequest.setUserLatestReply(asrResult.getText());
            textRequest.setGlobalInstruction(request.getGlobalInstruction());
            textRequest.setConversationHistory(request.getConversationHistory());
            textRequest.setIncludeAudio(request.getIncludeAudio());

            VoiceGuideResponse guideResponse = voiceGuideOrchestrationService.generate(textRequest);
            guideResponse.setAsr(asrResult);
            publish(sessionId, buildEvent(
                    sessionId,
                    request,
                    VoiceGuideStreamEventType.GUIDE_RESULT,
                    "语音引导成稿已生成",
                    guideResponse.getSuggestedRewrite(),
                    asrResult,
                    guideResponse
            ));
            publish(sessionId, buildEvent(
                    sessionId,
                    request,
                    VoiceGuideStreamEventType.COMPLETE,
                    "流式语音引导任务已完成",
                    guideResponse.getSuggestedRewrite(),
                    asrResult,
                    guideResponse
            ));
        } catch (Exception ex) {
            log.error("流式语音引导任务失败 | sessionId={}", sessionId, ex);
            if (completedAsr != null && completedAsr.getText() != null && !completedAsr.getText().isBlank()) {
                VoiceGuideResponse degraded = new VoiceGuideResponse();
                degraded.setSessionId(request.getSessionId());
                degraded.setResumeId(request.getResumeId());
                degraded.setExperienceType(request.getExperienceType());
                degraded.setAsr(completedAsr);
                degraded.setDegraded(true);
                degraded.setComplete(false);
                degraded.setWarning("语音已转写，AI 分析暂时失败，可直接使用转写文字或重新分析");
                publish(sessionId, buildEvent(
                        sessionId,
                        request,
                        VoiceGuideStreamEventType.GUIDE_RESULT,
                        degraded.getWarning(),
                        completedAsr.getText(),
                        completedAsr,
                        degraded
                ));
                publish(sessionId, buildEvent(
                        sessionId,
                        request,
                        VoiceGuideStreamEventType.COMPLETE,
                        "语音转写已保留，AI 分析已降级",
                        completedAsr.getText(),
                        completedAsr,
                        degraded
                ));
                return;
            }
            publish(sessionId, buildEvent(
                    sessionId,
                    request,
                    VoiceGuideStreamEventType.ERROR,
                    ex.getMessage(),
                    null,
                    null,
                    null
            ));
        } finally {
            streamSessionService.complete(sessionId);
        }
    }

    private boolean isFinalAsrEvent(AsrTranscriptionResult result) {
        if (result == null) {
            return false;
        }
        String mode = result.getMode();
        if (mode == null) {
            return Boolean.TRUE.equals(result.getFinalResult());
        }
        return "offline".equalsIgnoreCase(mode)
                || "2pass-offline".equalsIgnoreCase(mode)
                || ("online".equalsIgnoreCase(mode) && Boolean.TRUE.equals(result.getFinalResult()));
    }

    private void publish(String sessionId, VoiceGuideStreamEvent event) {
        streamSessionService.publish(sessionId, event);
    }

    private VoiceGuideStreamEvent buildEvent(
            String sessionId,
            VoiceGuideAudioRequest request,
            VoiceGuideStreamEventType type,
            String message,
            String text,
            AsrTranscriptionResult asr,
            VoiceGuideResponse response
    ) {
        VoiceGuideStreamEvent event = new VoiceGuideStreamEvent();
        event.setSessionId(sessionId);
        event.setEventType(type);
        event.setExperienceType(request.getExperienceType());
        event.setMessage(message);
        event.setText(text);
        event.setAsr(asr);
        event.setResponse(response);
        event.setTimestamp(System.currentTimeMillis());
        return event;
    }
}
