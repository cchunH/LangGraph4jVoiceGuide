package com.msjl.langgraph4j.voiceguide;

import com.msjl.langgraph4j.voiceguide.agent.SpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideDraft;
import com.msjl.langgraph4j.voiceguide.node.SpeechSynthesisNode;
import com.msjl.langgraph4j.voiceguide.state.VoiceGuideState;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SpeechSynthesisNodeTest {

    @Test
    void skipsTtsWhenCallerOnlyNeedsText() {
        SpeechSynthesisGateway gateway = mock(SpeechSynthesisGateway.class);
        SpeechSynthesisNode node = new SpeechSynthesisNode(gateway);

        Map<String, Object> updates = node.apply(state(false));

        verify(gateway, never()).synthesize(any(), any());
        assertFalse(updates.containsKey(VoiceGuideState.GUIDE_AUDIO));
        assertTrue(String.valueOf(updates.get(VoiceGuideState.EXECUTION_LOGS)).contains("skipped"));
    }

    @Test
    void keepsTextFlowAliveWhenTtsFails() {
        SpeechSynthesisGateway gateway = mock(SpeechSynthesisGateway.class);
        doThrow(new IllegalStateException("tts unavailable")).when(gateway).synthesize(any(), any());
        SpeechSynthesisNode node = new SpeechSynthesisNode(gateway);

        Map<String, Object> updates = node.apply(state(true));

        assertFalse(updates.containsKey(VoiceGuideState.GUIDE_AUDIO));
        assertTrue(String.valueOf(updates.get(VoiceGuideState.EXECUTION_LOGS)).contains("degraded"));
    }

    private VoiceGuideState state(boolean includeAudio) {
        VoiceGuideDraft draft = new VoiceGuideDraft();
        draft.setGuideText("继续补充信息");
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(VoiceGuideState.EXPERIENCE_TYPE, VoiceGuideExperienceType.EDUCATION);
        values.put(VoiceGuideState.GUIDE_DRAFT, draft);
        values.put(VoiceGuideState.INCLUDE_AUDIO, includeAudio);
        return new VoiceGuideState(values);
    }
}
