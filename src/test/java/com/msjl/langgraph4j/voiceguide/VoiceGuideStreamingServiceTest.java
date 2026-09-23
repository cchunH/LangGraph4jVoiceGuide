package com.msjl.langgraph4j.voiceguide;

import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideStreamEvent;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideStreamStartResponse;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideStreamEventType;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideStreamSessionService;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideStreamingService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "langgraph4j.voice-guide.llm.provider=mock",
        "langgraph4j.voice-guide.tts.provider=mock",
        "langgraph4j.voice-guide.asr.provider=mock"
})
class VoiceGuideStreamingServiceTest {

    @Autowired
    private VoiceGuideStreamingService voiceGuideStreamingService;

    @Autowired
    private VoiceGuideStreamSessionService voiceGuideStreamSessionService;

    @Test
    void shouldPublishStreamingAsrAndGuideEvents() throws Exception {
        VoiceGuideAudioRequest request = new VoiceGuideAudioRequest();
        request.setSessionId("stream-test-001");
        request.setExperienceType(VoiceGuideExperienceType.INTERNSHIP_EXPERIENCE);
        request.setCurrentContent("在某互联网公司实习，参与后端接口开发和性能优化。");
        request.setGlobalInstruction("优先帮助用户补充量化结果。");

        VoiceGuideStreamStartResponse startResponse = voiceGuideStreamingService.startStream(
                request,
                "mock-audio".getBytes(),
                "internship.wav",
                "audio/wav"
        );

        Assertions.assertEquals("stream-test-001", startResponse.getSessionId());

        List<VoiceGuideStreamEvent> events = waitForEvents("stream-test-001");
        Assertions.assertTrue(events.stream().anyMatch(event -> event.getEventType() == VoiceGuideStreamEventType.STARTED));
        Assertions.assertTrue(events.stream().anyMatch(event -> event.getEventType() == VoiceGuideStreamEventType.ASR_PARTIAL));
        Assertions.assertTrue(events.stream().anyMatch(event -> event.getEventType() == VoiceGuideStreamEventType.ASR_FINAL));
        Assertions.assertTrue(events.stream().anyMatch(event -> event.getEventType() == VoiceGuideStreamEventType.GUIDE_RESULT));
        Assertions.assertTrue(events.stream().anyMatch(event -> event.getEventType() == VoiceGuideStreamEventType.COMPLETE));
    }

    private List<VoiceGuideStreamEvent> waitForEvents(String sessionId) throws InterruptedException {
        for (int i = 0; i < 40; i++) {
            List<VoiceGuideStreamEvent> events = voiceGuideStreamSessionService.listBufferedEvents(sessionId);
            boolean completed = events.stream().anyMatch(event -> event.getEventType() == VoiceGuideStreamEventType.COMPLETE);
            if (completed) {
                return events;
            }
            Thread.sleep(50L);
        }
        return voiceGuideStreamSessionService.listBufferedEvents(sessionId);
    }
}
