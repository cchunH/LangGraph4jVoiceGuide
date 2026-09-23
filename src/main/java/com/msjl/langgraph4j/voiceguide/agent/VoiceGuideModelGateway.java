package com.msjl.langgraph4j.voiceguide.agent;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideModelResult;

public interface VoiceGuideModelGateway {

    VoiceGuideModelResult generate(String systemPrompt, String userPrompt, VoiceGuideExperienceType experienceType);
}
