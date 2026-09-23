package com.msjl.langgraph4j.voiceguide.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.voiceclone.VoiceCloneManifest;
import com.msjl.langgraph4j.voiceguide.domain.model.voiceclone.VoiceCloneVoiceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class VoiceCloneManifestService {

    private static final Logger log = LoggerFactory.getLogger(VoiceCloneManifestService.class);

    private final VoiceGuideProperties properties;
    private final ObjectMapper objectMapper;

    public VoiceCloneManifestService(VoiceGuideProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public VoiceCloneManifest loadManifest() {
        Path manifestPath = Path.of(properties.getTts().getAliyun().getVoiceCloneRealtime().getManifestFile());
        try {
            if (!Files.exists(manifestPath)) {
                return defaultManifest();
            }
            VoiceCloneManifest manifest = objectMapper.readValue(manifestPath.toFile(), VoiceCloneManifest.class);
            if (manifest.getVoices() == null) {
                manifest.setVoices(List.of());
            }
            return manifest;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read voices manifest: " + manifestPath, ex);
        }
    }

    public Optional<VoiceCloneVoiceEntry> resolveVoice(VoiceGuideExperienceType experienceType) {
        VoiceCloneManifest manifest = loadManifest();
        Optional<VoiceCloneVoiceEntry> exact = manifest.getVoices().stream()
                .filter(this::isActive)
                .filter(voice -> matchesExperienceType(voice, experienceType))
                .sorted(Comparator.comparing(VoiceCloneVoiceEntry::getId, Comparator.nullsLast(String::compareTo)))
                .findFirst();
        if (exact.isPresent()) {
            return exact;
        }

        if (manifest.getDefaultVoiceId() != null && !manifest.getDefaultVoiceId().isBlank()) {
            Optional<VoiceCloneVoiceEntry> defaultVoice = manifest.getVoices().stream()
                    .filter(this::isActive)
                    .filter(voice -> manifest.getDefaultVoiceId().equals(voice.getId()))
                    .findFirst();
            if (defaultVoice.isPresent()) {
                return defaultVoice;
            }
        }

        Optional<VoiceCloneVoiceEntry> taggedDefault = manifest.getVoices().stream()
                .filter(this::isActive)
                .filter(voice -> voice.getUsageTags().stream().anyMatch(tag -> "default".equalsIgnoreCase(tag)))
                .findFirst();
        if (taggedDefault.isPresent()) {
            return taggedDefault;
        }
        return Optional.empty();
    }

    public VoiceCloneManifest defaultManifest() {
        VoiceGuideProperties.VoiceCloneRealtimeProperties clone = properties.getTts().getAliyun().getVoiceCloneRealtime();
        VoiceCloneManifest manifest = new VoiceCloneManifest();
        manifest.setProvider("aliyun");
        manifest.setTargetModel(clone.getModel());
        manifest.setEnrollmentModel(clone.getEnrollmentModel());
        manifest.setUpdatedAt(OffsetDateTime.now().toString());
        return manifest;
    }

    private boolean isActive(VoiceCloneVoiceEntry voice) {
        return voice != null
                && voice.isEnabled()
                && voice.getRemoteVoiceId() != null
                && !voice.getRemoteVoiceId().isBlank()
                && voice.getStatus() != null
                && ("active".equalsIgnoreCase(voice.getStatus()) || "ready".equalsIgnoreCase(voice.getStatus()));
    }

    private boolean matchesExperienceType(VoiceCloneVoiceEntry voice, VoiceGuideExperienceType experienceType) {
        String expected = experienceType.name();
        return voice.getExperienceTypes().stream()
                .filter(type -> type != null && !type.isBlank())
                .map(type -> type.trim().toUpperCase(Locale.ROOT))
                .anyMatch(expected::equals);
    }

    public void logManifestSummary() {
        VoiceCloneManifest manifest = loadManifest();
        log.info("已加载 voices.json | schemaVersion={} | voices={}",
                manifest.getSchemaVersion(),
                manifest.getVoices().size());
    }
}
