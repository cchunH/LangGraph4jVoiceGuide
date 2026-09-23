package com.msjl.langgraph4j.voiceguide.agent;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;

public interface SpeechSynthesisGateway {

    VoiceGuideAudio synthesize(String text, VoiceGuideExperienceType experienceType);
}
