package com.msjl.langgraph4j.voiceguide.node;

import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideDraft;
import com.msjl.langgraph4j.voiceguide.state.VoiceGuideState;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CompletionDecisionNode implements NodeAction<VoiceGuideState> {

    private static final int DEFAULT_COMPLETE_THRESHOLD = 85;

    @Override
    public Map<String, Object> apply(VoiceGuideState state) {
        VoiceGuideDraft draft = state.guideDraft()
                .orElseThrow(() -> new IllegalArgumentException("Guide draft not generated"));

        int score = normalizeScore(draft.getCompletionScore());
        boolean complete = Boolean.TRUE.equals(draft.getComplete()) || score >= DEFAULT_COMPLETE_THRESHOLD;
        draft.setCompletionScore(score);
        draft.setComplete(complete);

        if (complete) {
            draft.setGuideText("这一段关键信息已经比较完整了，我先帮你整理成一版可以直接放进简历的内容，你看看还要不要再微调。");
            draft.setFollowUpQuestion("");
            if (draft.getRefinementDirection() == null || draft.getRefinementDirection().isBlank()) {
                draft.setRefinementDirection("关键信息已基本齐全，可直接进入成稿确认或轻微润色。");
            }
        } else if (draft.getFollowUpQuestion() == null || draft.getFollowUpQuestion().isBlank()) {
            draft.setFollowUpQuestion("我们继续补最关键的一条信息吧，优先补一个能证明结果的数据。");
        }

        Map<String, Object> updates = new LinkedHashMap<>();
        updates.put(VoiceGuideState.GUIDE_DRAFT, draft);
        updates.put(VoiceGuideState.COMPLETION_DECISION, complete ? "COMPLETE" : "INCOMPLETE");
        updates.put(
                VoiceGuideState.EXECUTION_LOGS,
                "COMPLETE_CHECK: score=" + score + ", isComplete=" + complete
        );
        return updates;
    }

    private int normalizeScore(Integer score) {
        if (score == null) {
            return 60;
        }
        if (score < 0) {
            return 0;
        }
        return Math.min(score, 100);
    }
}
