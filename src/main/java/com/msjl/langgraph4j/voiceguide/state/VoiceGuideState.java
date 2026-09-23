package com.msjl.langgraph4j.voiceguide.state;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.ConversationTurn;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideDraft;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class VoiceGuideState extends AgentState {

    public static final String SESSION_ID = "sessionId";
    public static final String RESUME_ID = "resumeId";
    public static final String TARGET_JOB_TITLE = "targetJobTitle";
    public static final String EXPERIENCE_TYPE = "experienceType";
    public static final String CURRENT_CONTENT = "currentContent";
    public static final String USER_LATEST_REPLY = "userLatestReply";
    public static final String GLOBAL_INSTRUCTION = "globalInstruction";
    public static final String CONVERSATION_HISTORY = "conversationHistory";
    public static final String INCLUDE_AUDIO = "includeAudio";
    public static final String GUIDE_DRAFT = "guideDraft";
    public static final String GUIDE_AUDIO = "guideAudio";
    public static final String COMPLETION_DECISION = "completionDecision";
    public static final String SUMMARY = "summary";
    public static final String EXECUTION_LOGS = "executionLogs";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            EXECUTION_LOGS, Channels.appender(ArrayList::new)
    );

    public VoiceGuideState(Map<String, Object> initData) {
        super(initData);
    }

    public Optional<String> sessionId() {
        return value(SESSION_ID);
    }

    public Optional<Long> resumeId() {
        return value(RESUME_ID);
    }

    public Optional<String> targetJobTitle() {
        return value(TARGET_JOB_TITLE);
    }

    public Optional<VoiceGuideExperienceType> experienceType() {
        return value(EXPERIENCE_TYPE);
    }

    public Optional<String> currentContent() {
        return value(CURRENT_CONTENT);
    }

    public Optional<String> userLatestReply() {
        return value(USER_LATEST_REPLY);
    }

    public Optional<String> globalInstruction() {
        return value(GLOBAL_INSTRUCTION);
    }

    public List<ConversationTurn> conversationHistory() {
        return this.<List<ConversationTurn>>value(CONVERSATION_HISTORY).orElseGet(ArrayList::new);
    }

    public boolean includeAudio() {
        return this.<Boolean>value(INCLUDE_AUDIO).orElse(Boolean.TRUE);
    }

    public Optional<VoiceGuideDraft> guideDraft() {
        return value(GUIDE_DRAFT);
    }

    public Optional<VoiceGuideAudio> guideAudio() {
        return value(GUIDE_AUDIO);
    }

    public Optional<String> summary() {
        return value(SUMMARY);
    }

    public List<String> executionLogs() {
        return this.<List<String>>value(EXECUTION_LOGS).orElseGet(ArrayList::new);
    }
}
