package com.msjl.langgraph4j.voiceguide.controller;

import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideResponse;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideStreamStartResponse;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideOrchestrationService;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideStreamingService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/langgraph4j/voice-guidance")
public class VoiceGuideController {

    private static final Logger log = LoggerFactory.getLogger(VoiceGuideController.class);

    private final VoiceGuideOrchestrationService voiceGuideOrchestrationService;
    private final VoiceGuideStreamingService voiceGuideStreamingService;

    public VoiceGuideController(
            VoiceGuideOrchestrationService voiceGuideOrchestrationService,
            VoiceGuideStreamingService voiceGuideStreamingService
    ) {
        this.voiceGuideOrchestrationService = voiceGuideOrchestrationService;
        this.voiceGuideStreamingService = voiceGuideStreamingService;
    }

    @PostMapping("/generate")
    public VoiceGuideResponse generate(@Valid @RequestBody VoiceGuideRequest request) throws Exception {
        log.info("收到语音引导请求 | sessionId={} | experienceType={}",
                request.getSessionId(),
                request.getExperienceType());
        return voiceGuideOrchestrationService.generate(request);
    }

    @PostMapping(
            value = "/generate-from-audio",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public VoiceGuideResponse generateFromAudio(
            @Valid @RequestPart("request") VoiceGuideAudioRequest request,
            @RequestPart("audio") MultipartFile audio
    ) throws Exception {
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }
        log.info("收到语音引导音频请求 | sessionId={} | experienceType={} | filename={}",
                request.getSessionId(),
                request.getExperienceType(),
                audio.getOriginalFilename());
        return voiceGuideOrchestrationService.generateFromAudio(
                request,
                readAudioBytes(audio),
                audio.getOriginalFilename(),
                audio.getContentType()
        );
    }

    private byte[] readAudioBytes(MultipartFile audio) throws IOException {
        return audio.getBytes();
    }

    @PostMapping(
            value = "/stream/start",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public VoiceGuideStreamStartResponse startStreamFromAudio(
            @Valid @RequestPart("request") VoiceGuideAudioRequest request,
            @RequestPart("audio") MultipartFile audio
    ) throws IOException {
        if (audio == null || audio.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }
        log.info("收到流式语音引导启动请求 | sessionId={} | experienceType={} | filename={}",
                request.getSessionId(),
                request.getExperienceType(),
                audio.getOriginalFilename());
        return voiceGuideStreamingService.startStream(
                request,
                readAudioBytes(audio),
                audio.getOriginalFilename(),
                audio.getContentType()
        );
    }

    @GetMapping(
            value = "/stream/{sessionId}/events",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter streamEvents(@PathVariable("sessionId") String sessionId) {
        log.info("建立流式语音引导 SSE 订阅 | sessionId={}", sessionId);
        return voiceGuideStreamingService.subscribe(sessionId);
    }
}
