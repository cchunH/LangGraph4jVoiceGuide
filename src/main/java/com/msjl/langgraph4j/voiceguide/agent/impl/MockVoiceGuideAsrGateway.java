package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideAsrGateway;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideAsrListener;
import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;

public class MockVoiceGuideAsrGateway implements VoiceGuideAsrGateway {

    @Override
    public AsrTranscriptionResult transcribe(
            byte[] audioBytes,
            String originalFilename,
            String contentType,
            String hotwords,
            VoiceGuideAsrListener listener
    ) {
        if (listener != null) {
            AsrTranscriptionResult partial = new AsrTranscriptionResult();
            partial.setProvider("mock");
            partial.setMode("2pass-online");
            partial.setWavName(originalFilename);
            partial.setText("我主要做了接口响应优化和慢查询治理");
            partial.setFinalResult(false);
            listener.onResult(partial);
        }
        AsrTranscriptionResult result = new AsrTranscriptionResult();
        result.setProvider("mock");
        result.setMode("2pass-offline");
        result.setWavName(originalFilename);
        result.setText("我主要做了接口响应优化和慢查询治理，核心接口平均响应时间下降了百分之二十五。");
        result.setFinalResult(true);
        if (listener != null) {
            listener.onResult(result);
        }
        return result;
    }
}
