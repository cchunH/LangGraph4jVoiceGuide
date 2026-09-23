package com.msjl.langgraph4j.voiceguide;

import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideResponse;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.service.VoiceGuideOrchestrationService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "langgraph4j.voice-guide.llm.provider=mock",
        "langgraph4j.voice-guide.tts.provider=mock",
        "langgraph4j.voice-guide.asr.provider=mock"
})
class VoiceGuideOrchestrationServiceTest {

    @Autowired
    private VoiceGuideOrchestrationService voiceGuideOrchestrationService;

    @Test
    void shouldGenerateVoiceGuidanceForInternship() throws Exception {
        VoiceGuideRequest request = new VoiceGuideRequest();
        request.setSessionId("session-001");
        request.setResumeId(1001L);
        request.setTargetJobTitle("Java后端工程师");
        request.setExperienceType(VoiceGuideExperienceType.INTERNSHIP_EXPERIENCE);
        request.setCurrentContent("在某互联网公司实习，参与后端接口开发和性能优化。");
        request.setUserLatestReply("主要做了接口优化和数据库查询性能优化。");
        request.setGlobalInstruction("优先帮助用户补充量化结果。");

        VoiceGuideResponse response = voiceGuideOrchestrationService.generate(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(VoiceGuideExperienceType.INTERNSHIP_EXPERIENCE, response.getExperienceType());
        Assertions.assertNotNull(response.getGuideText());
        Assertions.assertFalse(response.getGuideText().isBlank());
        Assertions.assertNotNull(response.getFollowUpQuestion());
        Assertions.assertFalse(response.getFollowUpQuestion().isBlank());
        Assertions.assertNotNull(response.getStructuredState());
        Assertions.assertNotNull(response.getStructuredState().getAction());
        Assertions.assertNotNull(response.getCompletionScore());
        Assertions.assertFalse(Boolean.TRUE.equals(response.getComplete()));
        Assertions.assertNotNull(response.getAudio());
        Assertions.assertNotNull(response.getAudio().getAudioBase64());
        Assertions.assertFalse(response.getExecutionLogs().isEmpty());
    }

    @Test
    void shouldGenerateVoiceGuidanceFromAudioThroughAsr() throws Exception {
        VoiceGuideAudioRequest request = new VoiceGuideAudioRequest();
        request.setSessionId("session-audio-001");
        request.setResumeId(1002L);
        request.setTargetJobTitle("Java后端工程师");
        request.setExperienceType(VoiceGuideExperienceType.INTERNSHIP_EXPERIENCE);
        request.setCurrentContent("在某互联网公司实习，参与后端接口开发和性能优化。");
        request.setGlobalInstruction("优先帮助用户补充量化结果。");

        byte[] audioBytes = "mock-audio".getBytes();
        VoiceGuideResponse response = voiceGuideOrchestrationService.generateFromAudio(
                request,
                audioBytes,
                "internship.wav",
                "audio/wav"
        );

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getAsr());
        Assertions.assertEquals("mock", response.getAsr().getProvider());
        Assertions.assertNotNull(response.getAsr().getText());
        Assertions.assertFalse(response.getAsr().getText().isBlank());
        Assertions.assertNotNull(response.getSuggestedRewrite());
        Assertions.assertFalse(response.getSuggestedRewrite().isBlank());
    }
}
