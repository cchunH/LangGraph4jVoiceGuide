package com.msjl.langgraph4j.voiceguide.agent;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideDraft;

public interface ExperienceVoiceGuideAgent {

    VoiceGuideExperienceType supportType();

    VoiceGuideDraft guide(VoiceGuideContext context);
}
