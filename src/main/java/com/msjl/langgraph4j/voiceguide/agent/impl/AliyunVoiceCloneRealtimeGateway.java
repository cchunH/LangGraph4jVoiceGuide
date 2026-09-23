package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.voiceclone.VoiceCloneVoiceEntry;
import com.msjl.langgraph4j.voiceguide.service.VoiceCloneManifestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class AliyunVoiceCloneRealtimeGateway {

    private static final Logger log = LoggerFactory.getLogger(AliyunVoiceCloneRealtimeGateway.class);

    private final VoiceGuideProperties properties;
    private final VoiceCloneManifestService manifestService;

    public AliyunVoiceCloneRealtimeGateway(VoiceGuideProperties properties, VoiceCloneManifestService manifestService) {
        this.properties = properties;
        this.manifestService = manifestService;
    }

    public boolean isReady(VoiceGuideExperienceType experienceType) {
        VoiceGuideProperties.VoiceCloneRealtimeProperties clone = properties.getTts()
                .getAliyun()
                .getVoiceCloneRealtime();
        return clone.isEnabled() && resolveRegisteredVoice(experienceType) != null;
    }

    public String resolveRegisteredVoice(VoiceGuideExperienceType experienceType) {
        Optional<VoiceCloneVoiceEntry> manifestVoice = manifestService.resolveVoice(experienceType);
        if (manifestVoice.isPresent()) {
            return manifestVoice.get().getRemoteVoiceId();
        }
        VoiceGuideProperties.VoiceCloneRealtimeProperties clone = properties.getTts()
                .getAliyun()
                .getVoiceCloneRealtime();
        String mapped = clone.getRegisteredVoices().get(experienceType.name());
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        String lowerMapped = clone.getRegisteredVoices().get(experienceType.name().toLowerCase());
        if (lowerMapped != null && !lowerMapped.isBlank()) {
            return lowerMapped;
        }
        String defaultMapped = clone.getRegisteredVoices().get("default");
        if (defaultMapped != null && !defaultMapped.isBlank()) {
            return defaultMapped;
        }
        return null;
    }

    public void logReservedRoute(VoiceGuideExperienceType experienceType) {
        VoiceGuideProperties.VoiceCloneRealtimeProperties clone = properties.getTts()
                .getAliyun()
                .getVoiceCloneRealtime();
        log.info("VC-Realtime 备选路由已预留 | experienceType={} | enabled={} | model={} | storageDir={} | voice={}",
                experienceType,
                clone.isEnabled(),
                clone.getModel(),
                clone.getStorageDir(),
                resolveRegisteredVoice(experienceType));
    }
}
