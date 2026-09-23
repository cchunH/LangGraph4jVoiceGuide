package com.msjl.langgraph4j.voiceguide.domain.model;

import java.io.Serializable;

public class VoiceGuideDraft implements Serializable {

    private String guideText;
    private String followUpQuestion;
    private String refinementDirection;
    private String suggestedRewrite;
    private String agentName;
    private ExperienceStructuredState structuredState;
    private Integer completionScore;
    private Boolean complete;

    public String getGuideText() {
        return guideText;
    }

    public void setGuideText(String guideText) {
        this.guideText = guideText;
    }

    public String getFollowUpQuestion() {
        return followUpQuestion;
    }

    public void setFollowUpQuestion(String followUpQuestion) {
        this.followUpQuestion = followUpQuestion;
    }

    public String getRefinementDirection() {
        return refinementDirection;
    }

    public void setRefinementDirection(String refinementDirection) {
        this.refinementDirection = refinementDirection;
    }

    public String getSuggestedRewrite() {
        return suggestedRewrite;
    }

    public void setSuggestedRewrite(String suggestedRewrite) {
        this.suggestedRewrite = suggestedRewrite;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public ExperienceStructuredState getStructuredState() {
        return structuredState;
    }

    public void setStructuredState(ExperienceStructuredState structuredState) {
        this.structuredState = structuredState;
    }

    public Integer getCompletionScore() {
        return completionScore;
    }

    public void setCompletionScore(Integer completionScore) {
        this.completionScore = completionScore;
    }

    public Boolean getComplete() {
        return complete;
    }

    public void setComplete(Boolean complete) {
        this.complete = complete;
    }
}
