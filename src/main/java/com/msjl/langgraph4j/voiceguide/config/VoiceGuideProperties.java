package com.msjl.langgraph4j.voiceguide.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "langgraph4j.voice-guide")
public class VoiceGuideProperties {

    private String graphId = "voice-guided-experience-graph";
    private LlmProperties llm = new LlmProperties();
    private TtsProperties tts = new TtsProperties();
    private AsrProperties asr = new AsrProperties();

    public String getGraphId() {
        return graphId;
    }

    public void setGraphId(String graphId) {
        this.graphId = graphId;
    }

    public LlmProperties getLlm() {
        return llm;
    }

    public void setLlm(LlmProperties llm) {
        this.llm = llm;
    }

    public TtsProperties getTts() {
        return tts;
    }

    public void setTts(TtsProperties tts) {
        this.tts = tts;
    }

    public AsrProperties getAsr() {
        return asr;
    }

    public void setAsr(AsrProperties asr) {
        this.asr = asr;
    }

    public static class LlmProperties {

        private String provider = "aliyun";
        private AliyunLlmProperties aliyun = new AliyunLlmProperties();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public AliyunLlmProperties getAliyun() {
            return aliyun;
        }

        public void setAliyun(AliyunLlmProperties aliyun) {
            this.aliyun = aliyun;
        }
    }

    public static class TtsProperties {

        private String provider = "aliyun";
        private TtsRouteProperties route = new TtsRouteProperties();
        private AliyunTtsProperties aliyun = new AliyunTtsProperties();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public TtsRouteProperties getRoute() {
            return route;
        }

        public void setRoute(TtsRouteProperties route) {
            this.route = route;
        }

        public AliyunTtsProperties getAliyun() {
            return aliyun;
        }

        public void setAliyun(AliyunTtsProperties aliyun) {
            this.aliyun = aliyun;
        }
    }

    public static class AsrProperties {

        private String provider = "funasr";
        private FunAsrProperties funasr = new FunAsrProperties();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public FunAsrProperties getFunasr() {
            return funasr;
        }

        public void setFunasr(FunAsrProperties funasr) {
            this.funasr = funasr;
        }
    }

    public static class FunAsrProperties {

        private String websocketUrl = "ws://127.0.0.1:10095";
        private String mode = "offline";
        private String chunkSize = "5,10,5";
        private int chunkInterval = 10;
        private boolean itn = true;
        private int sampleRate = 16000;
        private int connectTimeoutSeconds = 10;
        private int responseTimeoutSeconds = 90;
        private String wavNamePrefix = "voice-guide-asr";

        public String getWebsocketUrl() {
            return websocketUrl;
        }

        public void setWebsocketUrl(String websocketUrl) {
            this.websocketUrl = websocketUrl;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(String chunkSize) {
            this.chunkSize = chunkSize;
        }

        public int getChunkInterval() {
            return chunkInterval;
        }

        public void setChunkInterval(int chunkInterval) {
            this.chunkInterval = chunkInterval;
        }

        public boolean isItn() {
            return itn;
        }

        public void setItn(boolean itn) {
            this.itn = itn;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getResponseTimeoutSeconds() {
            return responseTimeoutSeconds;
        }

        public void setResponseTimeoutSeconds(int responseTimeoutSeconds) {
            this.responseTimeoutSeconds = responseTimeoutSeconds;
        }

        public String getWavNamePrefix() {
            return wavNamePrefix;
        }

        public void setWavNamePrefix(String wavNamePrefix) {
            this.wavNamePrefix = wavNamePrefix;
        }
    }

    public static class TtsRouteProperties {

        private String primaryMode = "instruct";
        private String backupMode = "vc-realtime";
        private boolean backupReserveEnabled = true;
        private boolean allowPrimaryFallback = true;

        public String getPrimaryMode() {
            return primaryMode;
        }

        public void setPrimaryMode(String primaryMode) {
            this.primaryMode = primaryMode;
        }

        public String getBackupMode() {
            return backupMode;
        }

        public void setBackupMode(String backupMode) {
            this.backupMode = backupMode;
        }

        public boolean isBackupReserveEnabled() {
            return backupReserveEnabled;
        }

        public void setBackupReserveEnabled(boolean backupReserveEnabled) {
            this.backupReserveEnabled = backupReserveEnabled;
        }

        public boolean isAllowPrimaryFallback() {
            return allowPrimaryFallback;
        }

        public void setAllowPrimaryFallback(boolean allowPrimaryFallback) {
            this.allowPrimaryFallback = allowPrimaryFallback;
        }
    }

    public static class AliyunLlmProperties {

        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        private String apiKey;
        private String model = "qwen-plus";
        private double temperature = 0.4D;
        private int maxTokens = 1200;
        private int connectTimeoutSeconds = 8;
        private int readTimeoutSeconds = 18;
        private int maxAttempts = 3;
        private long retryInitialDelayMillis = 400L;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getReadTimeoutSeconds() {
            return readTimeoutSeconds;
        }

        public void setReadTimeoutSeconds(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getRetryInitialDelayMillis() {
            return retryInitialDelayMillis;
        }

        public void setRetryInitialDelayMillis(long retryInitialDelayMillis) {
            this.retryInitialDelayMillis = retryInitialDelayMillis;
        }
    }

    public static class AliyunTtsProperties {

        private String baseUrl = "https://dashscope.aliyuncs.com/api/v1";
        private String apiKey;
        private String endpointPath = "/services/aigc/multimodal-generation/generation";
        private String model = "qwen3-tts-instruct-flash";
        private String voice = "Cherry";
        private String format = "mp3";
        private int sampleRate = 24000;
        private Integer speechRate = 0;
        private Integer pitchRate = 0;
        private Integer volume = 50;
        private String languageType = "Chinese";
        private boolean optimizeInstructions = true;
        private int connectTimeoutSeconds = 8;
        private int readTimeoutSeconds = 20;
        private ExperienceVoiceProfiles profiles = new ExperienceVoiceProfiles();
        private VoiceCloneRealtimeProperties voiceCloneRealtime = new VoiceCloneRealtimeProperties();

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getEndpointPath() {
            return endpointPath;
        }

        public void setEndpointPath(String endpointPath) {
            this.endpointPath = endpointPath;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getVoice() {
            return voice;
        }

        public void setVoice(String voice) {
            this.voice = voice;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public Integer getSpeechRate() {
            return speechRate;
        }

        public void setSpeechRate(Integer speechRate) {
            this.speechRate = speechRate;
        }

        public Integer getPitchRate() {
            return pitchRate;
        }

        public void setPitchRate(Integer pitchRate) {
            this.pitchRate = pitchRate;
        }

        public Integer getVolume() {
            return volume;
        }

        public void setVolume(Integer volume) {
            this.volume = volume;
        }

        public String getLanguageType() {
            return languageType;
        }

        public void setLanguageType(String languageType) {
            this.languageType = languageType;
        }

        public boolean isOptimizeInstructions() {
            return optimizeInstructions;
        }

        public void setOptimizeInstructions(boolean optimizeInstructions) {
            this.optimizeInstructions = optimizeInstructions;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getReadTimeoutSeconds() {
            return readTimeoutSeconds;
        }

        public void setReadTimeoutSeconds(int readTimeoutSeconds) {
            this.readTimeoutSeconds = readTimeoutSeconds;
        }

        public ExperienceVoiceProfiles getProfiles() {
            return profiles;
        }

        public void setProfiles(ExperienceVoiceProfiles profiles) {
            this.profiles = profiles;
        }

        public VoiceCloneRealtimeProperties getVoiceCloneRealtime() {
            return voiceCloneRealtime;
        }

        public void setVoiceCloneRealtime(VoiceCloneRealtimeProperties voiceCloneRealtime) {
            this.voiceCloneRealtime = voiceCloneRealtime;
        }
    }

    public static class ExperienceVoiceProfiles {

        private VoiceStyle education = new VoiceStyle(2, 3, 52);
        private VoiceStyle internshipExperience = new VoiceStyle(4, 5, 54);
        private VoiceStyle workExperience = new VoiceStyle(1, 2, 50);
        private VoiceStyle projectExperience = new VoiceStyle(3, 4, 53);

        public VoiceStyle getEducation() {
            return education;
        }

        public void setEducation(VoiceStyle education) {
            this.education = education;
        }

        public VoiceStyle getInternshipExperience() {
            return internshipExperience;
        }

        public void setInternshipExperience(VoiceStyle internshipExperience) {
            this.internshipExperience = internshipExperience;
        }

        public VoiceStyle getWorkExperience() {
            return workExperience;
        }

        public void setWorkExperience(VoiceStyle workExperience) {
            this.workExperience = workExperience;
        }

        public VoiceStyle getProjectExperience() {
            return projectExperience;
        }

        public void setProjectExperience(VoiceStyle projectExperience) {
            this.projectExperience = projectExperience;
        }
    }

    public static class VoiceStyle {

        private Integer speechRate;
        private Integer pitchRate;
        private Integer volume;

        public VoiceStyle() {
        }

        public VoiceStyle(Integer speechRate, Integer pitchRate, Integer volume) {
            this.speechRate = speechRate;
            this.pitchRate = pitchRate;
            this.volume = volume;
        }

        public Integer getSpeechRate() {
            return speechRate;
        }

        public void setSpeechRate(Integer speechRate) {
            this.speechRate = speechRate;
        }

        public Integer getPitchRate() {
            return pitchRate;
        }

        public void setPitchRate(Integer pitchRate) {
            this.pitchRate = pitchRate;
        }

        public Integer getVolume() {
            return volume;
        }

        public void setVolume(Integer volume) {
            this.volume = volume;
        }
    }

    public static class VoiceCloneRealtimeProperties {

        private boolean enabled = false;
        private String model = "qwen3-tts-vc-realtime-2026-01-15";
        private String enrollmentModel = "qwen-voice-enrollment";
        private String websocketUrl = "wss://dashscope.aliyuncs.com/api-ws/v1/realtime";
        private String mode = "commit";
        private String responseFormat = "pcm";
        private int sampleRate = 24000;
        private int connectTimeoutSeconds = 20;
        private int responseTimeoutSeconds = 90;
        private String storageDir = "./voice-assets/custom-voice-sources";
        private String rawSourceDir = "./voice-assets/custom-voice-sources/raw";
        private String manifestDir = "./voice-assets/custom-voice-sources/manifest";
        private String enrolledVoiceDir = "./voice-assets/custom-voice-sources/enrolled";
        private String tempDir = "./voice-assets/custom-voice-sources/tmp";
        private String manifestFile = "./voice-assets/custom-voice-sources/manifest/voices.json";
        private Map<String, String> registeredVoices = new LinkedHashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getEnrollmentModel() {
            return enrollmentModel;
        }

        public void setEnrollmentModel(String enrollmentModel) {
            this.enrollmentModel = enrollmentModel;
        }

        public String getWebsocketUrl() {
            return websocketUrl;
        }

        public void setWebsocketUrl(String websocketUrl) {
            this.websocketUrl = websocketUrl;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getResponseFormat() {
            return responseFormat;
        }

        public void setResponseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public int getConnectTimeoutSeconds() {
            return connectTimeoutSeconds;
        }

        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
            this.connectTimeoutSeconds = connectTimeoutSeconds;
        }

        public int getResponseTimeoutSeconds() {
            return responseTimeoutSeconds;
        }

        public void setResponseTimeoutSeconds(int responseTimeoutSeconds) {
            this.responseTimeoutSeconds = responseTimeoutSeconds;
        }

        public String getStorageDir() {
            return storageDir;
        }

        public void setStorageDir(String storageDir) {
            this.storageDir = storageDir;
        }

        public String getRawSourceDir() {
            return rawSourceDir;
        }

        public void setRawSourceDir(String rawSourceDir) {
            this.rawSourceDir = rawSourceDir;
        }

        public String getManifestDir() {
            return manifestDir;
        }

        public void setManifestDir(String manifestDir) {
            this.manifestDir = manifestDir;
        }

        public String getEnrolledVoiceDir() {
            return enrolledVoiceDir;
        }

        public void setEnrolledVoiceDir(String enrolledVoiceDir) {
            this.enrolledVoiceDir = enrolledVoiceDir;
        }

        public String getTempDir() {
            return tempDir;
        }

        public void setTempDir(String tempDir) {
            this.tempDir = tempDir;
        }

        public String getManifestFile() {
            return manifestFile;
        }

        public void setManifestFile(String manifestFile) {
            this.manifestFile = manifestFile;
        }

        public Map<String, String> getRegisteredVoices() {
            return registeredVoices;
        }

        public void setRegisteredVoices(Map<String, String> registeredVoices) {
            this.registeredVoices = registeredVoices;
        }
    }
}
