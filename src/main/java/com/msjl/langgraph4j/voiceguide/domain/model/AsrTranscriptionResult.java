package com.msjl.langgraph4j.voiceguide.domain.model;

import java.io.Serializable;

public class AsrTranscriptionResult implements Serializable {

    private String provider;
    private String mode;
    private String requestId;
    private String wavName;
    private String text;
    private Boolean finalResult;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getWavName() {
        return wavName;
    }

    public void setWavName(String wavName) {
        this.wavName = wavName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Boolean getFinalResult() {
        return finalResult;
    }

    public void setFinalResult(Boolean finalResult) {
        this.finalResult = finalResult;
    }
}
