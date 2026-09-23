package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.msjl.langgraph4j.voiceguide.agent.SpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class MockSpeechSynthesisGateway implements SpeechSynthesisGateway {

    @Override
    public VoiceGuideAudio synthesize(String text, VoiceGuideExperienceType experienceType) {
        VoiceGuideAudio audio = new VoiceGuideAudio();
        audio.setProvider("mock");
        audio.setVoice("mock-voice");
        audio.setFormat("mp3");
        audio.setContentType("audio/mpeg");
        audio.setRequestId("mock-" + experienceType.name().toLowerCase());
        audio.setAudioBase64(Base64.getEncoder()
                .encodeToString(("MOCK_AUDIO:" + text).getBytes(StandardCharsets.UTF_8)));
        return audio;
    }
}
