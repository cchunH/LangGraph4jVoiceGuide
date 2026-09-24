package com.msjl.langgraph4j.voiceguide.domain;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.ConversationTurn;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class VoiceGuideAudioRequest {

    private String sessionId;
    private Long resumeId;
    private String targetJobTitle;
    @NotNull
    private VoiceGuideExperienceType experienceType;
    private String currentContent;
    private String globalInstruction;
    private String hotwords;
    private String audioFormat;
    private String responseMode;
    private Boolean includeAudio = Boolean.TRUE;
    @Valid
    private List<ConversationTurn> conversationHistory = new ArrayList<>();

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

    public String getTargetJobTitle() {
        return targetJobTitle;
    }

    public void setTargetJobTitle(String targetJobTitle) {
        this.targetJobTitle = targetJobTitle;
    }

    public VoiceGuideExperienceType getExperienceType() {
        return experienceType;
    }

    public void setExperienceType(VoiceGuideExperienceType experienceType) {
        this.experienceType = experienceType;
    }

    public String getCurrentContent() {
        return currentContent;
    }

    public void setCurrentContent(String currentContent) {
        this.currentContent = currentContent;
    }

    public String getGlobalInstruction() {
        return globalInstruction;
    }

    public void setGlobalInstruction(String globalInstruction) {
        this.globalInstruction = globalInstruction;
    }

    public String getHotwords() {
        return hotwords;
    }

    public void setHotwords(String hotwords) {
        this.hotwords = hotwords;
    }

    public String getAudioFormat() {
        return audioFormat;
    }

    public void setAudioFormat(String audioFormat) {
        this.audioFormat = audioFormat;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public boolean isTranscriptionOnly() {
        return "transcription".equalsIgnoreCase(responseMode);
    }

    public Boolean getIncludeAudio() {
        return includeAudio;
    }

    public void setIncludeAudio(Boolean includeAudio) {
        this.includeAudio = includeAudio == null ? Boolean.TRUE : includeAudio;
    }

    public List<ConversationTurn> getConversationHistory() {
        return conversationHistory;
    }

    public void setConversationHistory(List<ConversationTurn> conversationHistory) {
        this.conversationHistory = conversationHistory == null ? new ArrayList<>() : conversationHistory;
    }
}
