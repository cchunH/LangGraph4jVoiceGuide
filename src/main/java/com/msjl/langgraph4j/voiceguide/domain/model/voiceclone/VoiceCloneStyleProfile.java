package com.msjl.langgraph4j.voiceguide.domain.model.voiceclone;

public class VoiceCloneStyleProfile {

    private String responseFormat = "pcm";
    private Integer sampleRate = 24000;
    private Double speechRate = 1.0D;
    private Double pitchRate = 1.0D;
    private Integer volume = 50;

    public String getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public Integer getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(Integer sampleRate) {
        this.sampleRate = sampleRate;
    }

    public Double getSpeechRate() {
        return speechRate;
    }

    public void setSpeechRate(Double speechRate) {
        this.speechRate = speechRate;
    }

    public Double getPitchRate() {
        return pitchRate;
    }

    public void setPitchRate(Double pitchRate) {
        this.pitchRate = pitchRate;
    }

    public Integer getVolume() {
        return volume;
    }

    public void setVolume(Integer volume) {
        this.volume = volume;
    }
}
