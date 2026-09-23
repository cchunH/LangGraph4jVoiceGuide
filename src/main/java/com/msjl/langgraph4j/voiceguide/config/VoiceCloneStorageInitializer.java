package com.msjl.langgraph4j.voiceguide.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;

@Component
public class VoiceCloneStorageInitializer {

    private static final Logger log = LoggerFactory.getLogger(VoiceCloneStorageInitializer.class);

    private final VoiceGuideProperties properties;

    public VoiceCloneStorageInitializer(VoiceGuideProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        VoiceGuideProperties.VoiceCloneRealtimeProperties clone = properties.getTts()
                .getAliyun()
                .getVoiceCloneRealtime();
        try {
            createDir(clone.getStorageDir());
            createDir(clone.getRawSourceDir());
            createDir(clone.getManifestDir());
            createDir(clone.getEnrolledVoiceDir());
            createDir(clone.getTempDir());
            createReadme(clone.getStorageDir());
            createManifest(clone.getManifestFile());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to initialize reserved voice clone storage", ex);
        }
        log.info("已初始化 VC-Realtime 预留存储目录 | root={}", clone.getStorageDir());
    }

    private void createDir(String dir) throws IOException {
        Files.createDirectories(Path.of(dir));
    }

    private void createReadme(String rootDir) throws IOException {
        Path readme = Path.of(rootDir, "README.md");
        if (Files.exists(readme)) {
            return;
        }
        String content = """
                # Custom Voice Sources

                This directory is reserved for the Aliyun Qwen VC-Realtime backup route.

                - `raw/`: original reference audio files for voice cloning enrollment
                - `manifest/voices.json`: registered custom voice metadata and mapped voice ids
                - `enrolled/`: exported metadata or cached artifacts after enrollment
                - `tmp/`: temporary files used during upload, conversion, or validation

                Recommended source audio requirements:
                - 10 to 20 seconds
                - single speaker
                - clean background
                - sample rate >= 24kHz
                - wav/mp3/m4a
                """;
        Files.writeString(readme, content);
    }

    private void createManifest(String manifestFile) throws IOException {
        Path manifest = Path.of(manifestFile);
        if (Files.exists(manifest)) {
            return;
        }
        Files.createDirectories(manifest.getParent());
        String content = """
                {
                  "schemaVersion": "1.0",
                  "provider": "aliyun",
                  "targetModel": "qwen3-tts-vc-realtime-2026-01-15",
                  "enrollmentModel": "qwen-voice-enrollment",
                  "defaultVoiceId": "",
                  "updatedAt": "%s",
                  "voices": [
                    {
                      "id": "guide_natural_female_001",
                      "provider": "aliyun",
                      "kind": "qwen_vc_realtime",
                      "status": "draft",
                      "enabled": false,
                      "preferredName": "guide-natural-female",
                      "displayName": "语音引导-自然女声",
                      "remoteVoiceId": "",
                      "targetModel": "qwen3-tts-vc-realtime-2026-01-15",
                      "enrollmentModel": "qwen-voice-enrollment",
                      "language": "zh",
                      "experienceTypes": [
                        "INTERNSHIP_EXPERIENCE",
                        "PROJECT_EXPERIENCE"
                      ],
                      "usageTags": [
                        "guide",
                        "natural",
                        "backup"
                      ],
                      "sourceAudio": {
                        "path": "raw/guide-natural-female.wav",
                        "format": "wav",
                        "sampleRate": 24000,
                        "channels": 1,
                        "durationMs": 12000,
                        "transcript": "",
                        "sha256": ""
                      },
                      "styleProfile": {
                        "responseFormat": "pcm",
                        "sampleRate": 24000,
                        "speechRate": 1.02,
                        "pitchRate": 1.01,
                        "volume": 52
                      },
                      "lifecycle": {
                        "requestId": "",
                        "createdAt": "%s",
                        "updatedAt": "%s",
                        "enrolledAt": "",
                        "lastVerifiedAt": "",
                        "notes": ""
                      }
                    }
                  ]
                }
                """.formatted(
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        Files.writeString(manifest, content);
    }
}
