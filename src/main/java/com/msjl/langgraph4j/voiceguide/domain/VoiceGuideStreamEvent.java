package com.msjl.langgraph4j.voiceguide.domain;

import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideStreamEventType;
import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;

public class VoiceGuideStreamEvent {

    private String sessionId;
    private VoiceGuideStreamEventType eventType;
    private VoiceGuideExperienceType experienceType;
    private String text;
    private String message;
    private Long timestamp;
    private AsrTranscriptionResult asr;
    private VoiceGuideResponse response;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public VoiceGuideStreamEventType getEventType() {
        return eventType;
    }

    public void setEventType(VoiceGuideStreamEventType eventType) {
        this.eventType = eventType;
    }

    public VoiceGuideExperienceType getExperienceType() {
        return experienceType;
    }

    public void setExperienceType(VoiceGuideExperienceType experienceType) {
        this.experienceType = experienceType;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public AsrTranscriptionResult getAsr() {
        return asr;
    }

    public void setAsr(AsrTranscriptionResult asr) {
        this.asr = asr;
    }

    public VoiceGuideResponse getResponse() {
        return response;
    }

    public void setResponse(VoiceGuideResponse response) {
        this.response = response;
    }
}
