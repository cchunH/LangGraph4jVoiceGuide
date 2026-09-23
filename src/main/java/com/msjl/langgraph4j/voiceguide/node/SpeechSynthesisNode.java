package com.msjl.langgraph4j.voiceguide.node;

import com.msjl.langgraph4j.voiceguide.agent.SpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideDraft;
import com.msjl.langgraph4j.voiceguide.state.VoiceGuideState;
import org.bsc.langgraph4j.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SpeechSynthesisNode implements NodeAction<VoiceGuideState> {

    private static final Logger log = LoggerFactory.getLogger(SpeechSynthesisNode.class);

    private final SpeechSynthesisGateway speechSynthesisGateway;

    public SpeechSynthesisNode(SpeechSynthesisGateway speechSynthesisGateway) {
        this.speechSynthesisGateway = speechSynthesisGateway;
    }

    @Override
    public Map<String, Object> apply(VoiceGuideState state) {
        VoiceGuideExperienceType type = state.experienceType()
                .orElseThrow(() -> new IllegalArgumentException("Experience type is required"));
        VoiceGuideDraft draft = state.guideDraft()
                .orElseThrow(() -> new IllegalArgumentException("Guide draft not generated"));
        Map<String, Object> updates = new LinkedHashMap<>();
        if (!state.includeAudio()) {
            updates.put(VoiceGuideState.EXECUTION_LOGS, "SYNTHESIZE: skipped by request");
            return updates;
        }
        try {
            VoiceGuideAudio audio = speechSynthesisGateway.synthesize(draft.getGuideText(), type);
            if (audio != null) {
                updates.put(VoiceGuideState.GUIDE_AUDIO, audio);
                updates.put(VoiceGuideState.EXECUTION_LOGS, "SYNTHESIZE: generated audio via " + audio.getProvider());
            } else {
                updates.put(VoiceGuideState.EXECUTION_LOGS, "SYNTHESIZE: provider returned no audio");
            }
        } catch (RuntimeException ex) {
            log.warn("语音合成失败，继续返回文本分析 | experienceType={} | reason={}", type, ex.getMessage());
            updates.put(VoiceGuideState.EXECUTION_LOGS, "SYNTHESIZE: degraded to text-only");
        }
        return updates;
    }
}
