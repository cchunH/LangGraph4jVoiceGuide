package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.msjl.langgraph4j.voiceguide.agent.SpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutedSpeechSynthesisGateway implements SpeechSynthesisGateway {

    private static final Logger log = LoggerFactory.getLogger(RoutedSpeechSynthesisGateway.class);

    private final SpeechSynthesisGateway primaryGateway;
    private final SpeechSynthesisGateway vcRealtimeGateway;
    private final AliyunVoiceCloneRealtimeGateway reservedCloneGateway;
    private final VoiceGuideProperties properties;

    public RoutedSpeechSynthesisGateway(
            SpeechSynthesisGateway primaryGateway,
            SpeechSynthesisGateway vcRealtimeGateway,
            AliyunVoiceCloneRealtimeGateway reservedCloneGateway,
            VoiceGuideProperties properties
    ) {
        this.primaryGateway = primaryGateway;
        this.vcRealtimeGateway = vcRealtimeGateway;
        this.reservedCloneGateway = reservedCloneGateway;
        this.properties = properties;
    }

    @Override
    public VoiceGuideAudio synthesize(String text, VoiceGuideExperienceType experienceType) {
        VoiceGuideProperties.TtsRouteProperties route = properties.getTts().getRoute();
        if (route.isBackupReserveEnabled()) {
            reservedCloneGateway.logReservedRoute(experienceType);
        }

        String primaryMode = normalize(route.getPrimaryMode());
        if ("vc-realtime".equals(primaryMode) && reservedCloneGateway.isReady(experienceType)) {
            return vcRealtimeGateway.synthesize(text, experienceType);
        }
        if ("vc-realtime".equals(primaryMode)) {
            log.warn("VC-Realtime 主路由已配置，但当前经历类型没有可用自定义音色；回退到 instruct TTS 主路由");
        }
        return primaryGateway.synthesize(text, experienceType);
    }

    private String normalize(String mode) {
        return mode == null ? "" : mode.trim().toLowerCase();
    }
}
