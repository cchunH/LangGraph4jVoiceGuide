package com.msjl.langgraph4j.voiceguide.agent;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ExperienceVoiceGuideAgentRegistry {

    private final Map<VoiceGuideExperienceType, ExperienceVoiceGuideAgent> agentMap =
            new EnumMap<>(VoiceGuideExperienceType.class);

    public ExperienceVoiceGuideAgentRegistry(List<ExperienceVoiceGuideAgent> agents) {
        for (ExperienceVoiceGuideAgent agent : agents) {
            agentMap.put(agent.supportType(), agent);
        }
    }

    public ExperienceVoiceGuideAgent get(VoiceGuideExperienceType type) {
        ExperienceVoiceGuideAgent agent = agentMap.get(type);
        if (agent == null) {
            throw new IllegalArgumentException("Voice guide agent not found for type: " + type);
        }
        return agent;
    }
}
