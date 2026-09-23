package com.msjl.langgraph4j.voiceguide.domain.model.voiceclone;

import java.util.ArrayList;
import java.util.List;

public class VoiceCloneManifest {

    private String schemaVersion = "1.0";
    private String provider = "aliyun";
    private String targetModel = "qwen3-tts-vc-realtime-2026-01-15";
    private String enrollmentModel = "qwen-voice-enrollment";
    private String defaultVoiceId;
    private String updatedAt;
    private List<VoiceCloneVoiceEntry> voices = new ArrayList<>();

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTargetModel() {
        return targetModel;
    }

    public void setTargetModel(String targetModel) {
        this.targetModel = targetModel;
    }

    public String getEnrollmentModel() {
        return enrollmentModel;
    }

    public void setEnrollmentModel(String enrollmentModel) {
        this.enrollmentModel = enrollmentModel;
    }

    public String getDefaultVoiceId() {
        return defaultVoiceId;
    }

    public void setDefaultVoiceId(String defaultVoiceId) {
        this.defaultVoiceId = defaultVoiceId;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<VoiceCloneVoiceEntry> getVoices() {
        return voices;
    }

    public void setVoices(List<VoiceCloneVoiceEntry> voices) {
        this.voices = voices == null ? new ArrayList<>() : voices;
    }
}
