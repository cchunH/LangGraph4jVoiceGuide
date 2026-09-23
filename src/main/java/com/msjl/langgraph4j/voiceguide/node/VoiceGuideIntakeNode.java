package com.msjl.langgraph4j.voiceguide.node;

import com.msjl.langgraph4j.voiceguide.state.VoiceGuideState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VoiceGuideIntakeNode implements NodeAction<VoiceGuideState> {

    @Override
    public Map<String, Object> apply(VoiceGuideState state) {
        if (state.experienceType().isEmpty()) {
            throw new IllegalArgumentException("Experience type is required");
        }

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(
                VoiceGuideState.EXECUTION_LOGS,
                "INTAKE: accepted voice guide request for " + state.experienceType().get().name()
        );
        return updates;
    }
}
