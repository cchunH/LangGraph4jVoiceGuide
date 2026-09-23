package com.msjl.langgraph4j.voiceguide.domain.model.voiceclone;

import java.util.ArrayList;
import java.util.List;

public class VoiceCloneVoiceEntry {

    private String id;
    private String provider = "aliyun";
    private String kind = "qwen_vc_realtime";
    private String status = "draft";
    private boolean enabled = false;
    private String preferredName;
    private String displayName;
    private String remoteVoiceId;
    private String targetModel = "qwen3-tts-vc-realtime-2026-01-15";
    private String enrollmentModel = "qwen-voice-enrollment";
    private String language = "zh";
    private List<String> experienceTypes = new ArrayList<>();
    private List<String> usageTags = new ArrayList<>();
    private VoiceCloneSourceAudio sourceAudio = new VoiceCloneSourceAudio();
    private VoiceCloneStyleProfile styleProfile = new VoiceCloneStyleProfile();
    private VoiceCloneLifecycle lifecycle = new VoiceCloneLifecycle();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRemoteVoiceId() {
        return remoteVoiceId;
    }

    public void setRemoteVoiceId(String remoteVoiceId) {
        this.remoteVoiceId = remoteVoiceId;
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

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<String> getExperienceTypes() {
        return experienceTypes;
    }

    public void setExperienceTypes(List<String> experienceTypes) {
        this.experienceTypes = experienceTypes == null ? new ArrayList<>() : experienceTypes;
    }

    public List<String> getUsageTags() {
        return usageTags;
    }

    public void setUsageTags(List<String> usageTags) {
        this.usageTags = usageTags == null ? new ArrayList<>() : usageTags;
    }

    public VoiceCloneSourceAudio getSourceAudio() {
        return sourceAudio;
    }

    public void setSourceAudio(VoiceCloneSourceAudio sourceAudio) {
        this.sourceAudio = sourceAudio == null ? new VoiceCloneSourceAudio() : sourceAudio;
    }

    public VoiceCloneStyleProfile getStyleProfile() {
        return styleProfile;
    }

    public void setStyleProfile(VoiceCloneStyleProfile styleProfile) {
        this.styleProfile = styleProfile == null ? new VoiceCloneStyleProfile() : styleProfile;
    }

    public VoiceCloneLifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(VoiceCloneLifecycle lifecycle) {
        this.lifecycle = lifecycle == null ? new VoiceCloneLifecycle() : lifecycle;
    }
}
