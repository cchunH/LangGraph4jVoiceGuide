package com.msjl.langgraph4j.voiceguide.domain.enums;

public enum VoiceGuideExperienceType {
    EDUCATION,
    INTERNSHIP_EXPERIENCE,
    WORK_EXPERIENCE,
    PROJECT_EXPERIENCE;

    public static VoiceGuideExperienceType fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        for (VoiceGuideExperienceType value : values()) {
            if (value.name().equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
