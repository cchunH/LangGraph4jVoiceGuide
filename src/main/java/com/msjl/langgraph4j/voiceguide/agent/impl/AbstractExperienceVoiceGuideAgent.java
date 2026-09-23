package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.msjl.langgraph4j.voiceguide.agent.ExperienceVoiceGuideAgent;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideContext;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideModelGateway;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideDraft;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideModelResult;
import com.msjl.langgraph4j.voiceguide.prompt.VoiceGuidePromptFactory;

public abstract class AbstractExperienceVoiceGuideAgent implements ExperienceVoiceGuideAgent {

    private final VoiceGuideModelGateway modelGateway;
    private final VoiceGuidePromptFactory promptFactory;

    protected AbstractExperienceVoiceGuideAgent(
            VoiceGuideModelGateway modelGateway,
            VoiceGuidePromptFactory promptFactory
    ) {
        this.modelGateway = modelGateway;
        this.promptFactory = promptFactory;
    }

    @Override
    public VoiceGuideDraft guide(VoiceGuideContext context) {
        String systemPrompt = promptFactory.buildSystemPrompt(supportType());
        String userPrompt = promptFactory.buildUserPrompt(context);
        VoiceGuideModelResult result = modelGateway.generate(systemPrompt, userPrompt, supportType());

        VoiceGuideDraft draft = new VoiceGuideDraft();
        draft.setGuideText(normalizeText(result.getGuideText(), promptFactory.buildFocus(supportType())));
        draft.setFollowUpQuestion(normalizeText(result.getFollowUpQuestion(), "请补充一个最能证明你成果的数据或例子。"));
        draft.setRefinementDirection(normalizeText(result.getRefinementDirection(), promptFactory.buildFocus(supportType())));
        draft.setSuggestedRewrite(normalizeText(result.getSuggestedRewrite(), context.getCurrentContent()));
        draft.setStructuredState(result.getStructuredState());
        draft.setCompletionScore(result.getCompletionScore());
        draft.setComplete(result.getComplete());
        draft.setAgentName(getClass().getSimpleName());
        return draft;
    }

    private String normalizeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback == null ? "" : fallback.trim();
        }
        return value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
