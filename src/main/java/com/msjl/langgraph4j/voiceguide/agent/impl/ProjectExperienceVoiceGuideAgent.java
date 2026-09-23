package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideModelGateway;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.prompt.VoiceGuidePromptFactory;
import org.springframework.stereotype.Component;

@Component
public class ProjectExperienceVoiceGuideAgent extends AbstractExperienceVoiceGuideAgent {

    public ProjectExperienceVoiceGuideAgent(VoiceGuideModelGateway modelGateway, VoiceGuidePromptFactory promptFactory) {
        super(modelGateway, promptFactory);
    }

    @Override
    public VoiceGuideExperienceType supportType() {
        return VoiceGuideExperienceType.PROJECT_EXPERIENCE;
    }
}
