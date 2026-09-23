package com.msjl.langgraph4j.voiceguide.agent;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.ConversationTurn;

import java.util.ArrayList;
import java.util.List;

public class VoiceGuideContext {

    private final String sessionId;
    private final Long resumeId;
    private final String targetJobTitle;
    private final VoiceGuideExperienceType experienceType;
    private final String currentContent;
    private final String userLatestReply;
    private final String globalInstruction;
    private final List<ConversationTurn> conversationHistory;

    public VoiceGuideContext(
            String sessionId,
            Long resumeId,
            String targetJobTitle,
            VoiceGuideExperienceType experienceType,
            String currentContent,
            String userLatestReply,
            String globalInstruction,
            List<ConversationTurn> conversationHistory
    ) {
        this.sessionId = sessionId;
        this.resumeId = resumeId;
        this.targetJobTitle = targetJobTitle;
        this.experienceType = experienceType;
        this.currentContent = currentContent;
        this.userLatestReply = userLatestReply;
        this.globalInstruction = globalInstruction;
        this.conversationHistory = conversationHistory == null ? new ArrayList<>() : conversationHistory;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getResumeId() {
        return resumeId;
    }

    public String getTargetJobTitle() {
        return targetJobTitle;
    }

    public VoiceGuideExperienceType getExperienceType() {
        return experienceType;
    }

    public String getCurrentContent() {
        return currentContent;
    }

    public String getUserLatestReply() {
        return userLatestReply;
    }

    public String getGlobalInstruction() {
        return globalInstruction;
    }

    public List<ConversationTurn> getConversationHistory() {
        return conversationHistory;
    }
}
