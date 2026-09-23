package com.msjl.langgraph4j.voiceguide.agent;

import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;

public interface VoiceGuideAsrGateway {

    default AsrTranscriptionResult transcribe(byte[] audioBytes, String originalFilename, String contentType, String hotwords) {
        return transcribe(audioBytes, originalFilename, contentType, hotwords, null);
    }

    AsrTranscriptionResult transcribe(
            byte[] audioBytes,
            String originalFilename,
            String contentType,
            String hotwords,
            VoiceGuideAsrListener listener
    );
}
