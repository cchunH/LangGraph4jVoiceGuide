package com.msjl.langgraph4j.voiceguide.node;

import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideDraft;
import com.msjl.langgraph4j.voiceguide.state.VoiceGuideState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class VoiceGuideFinalizeNode implements NodeAction<VoiceGuideState> {

    @Override
    public Map<String, Object> apply(VoiceGuideState state) {
        VoiceGuideDraft draft = state.guideDraft()
                .orElseThrow(() -> new IllegalArgumentException("Guide draft not generated"));
        String summary = "Generated voice guidance for "
                + state.experienceType().map(Enum::name).orElse("UNKNOWN")
                + " via " + draft.getAgentName()
                + " | completionScore=" + (draft.getCompletionScore() == null ? "unknown" : draft.getCompletionScore())
                + " | isComplete=" + Boolean.TRUE.equals(draft.getComplete());

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(VoiceGuideState.SUMMARY, summary);
        updates.put(VoiceGuideState.EXECUTION_LOGS, "FINALIZE: " + summary);
        return updates;
    }
}
