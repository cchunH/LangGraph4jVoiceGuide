package com.msjl.langgraph4j.voiceguide.domain;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;
import com.msjl.langgraph4j.voiceguide.domain.model.ExperienceStructuredState;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;

import java.util.ArrayList;
import java.util.List;

public class VoiceGuideResponse {

    private String sessionId;
    private Long resumeId;
    private String graphId;
    private VoiceGuideExperienceType experienceType;
    private String guideText;
    private String followUpQuestion;
    private String refinementDirection;
    private String suggestedRewrite;
    private ExperienceStructuredState structuredState;
    private Integer completionScore;
    private Boolean complete;
    private AsrTranscriptionResult asr;
    private String agentName;
    private String summary;
    private List<String> executionLogs = new ArrayList<>();
    private VoiceGuideAudio audio;
    private boolean degraded;
    private String warning;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public void setResumeId(Long resumeId) {
        this.resumeId = resumeId;
    }

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public VoiceGuideExperienceType getExperienceType() {
        return experienceType;
    }

    public void setExperienceType(VoiceGuideExperienceType experienceType) {
        this.experienceType = experienceType;
    }

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

    public AsrTranscriptionResult getAsr() {
        return asr;
    }

    public void setAsr(AsrTranscriptionResult asr) {
        this.asr = asr;
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

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getExecutionLogs() {
        return executionLogs;
    }

    public void setExecutionLogs(List<String> executionLogs) {
        this.executionLogs = executionLogs == null ? new ArrayList<>() : executionLogs;
    }

    public VoiceGuideAudio getAudio() {
        return audio;
    }

    public void setAudio(VoiceGuideAudio audio) {
        this.audio = audio;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public String getWarning() {
        return warning;
    }

    public void setWarning(String warning) {
        this.warning = warning;
    }
}
