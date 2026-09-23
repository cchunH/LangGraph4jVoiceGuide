package com.msjl.langgraph4j.voiceguide.agent;

import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;

public interface VoiceGuideAsrListener {

    void onResult(AsrTranscriptionResult result);

    default void onError(Throwable error) {
    }
}
