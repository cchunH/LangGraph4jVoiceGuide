package com.msjl.langgraph4j.voiceguide.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.agent.SpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideAsrGateway;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideModelGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.AliyunChatModelGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.AliyunRealtimeVcSpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.AliyunSpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.AliyunVoiceCloneRealtimeGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.FunAsrWebSocketGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.MockSpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.MockVoiceGuideAsrGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.MockVoiceGuideModelGateway;
import com.msjl.langgraph4j.voiceguide.agent.impl.RoutedSpeechSynthesisGateway;
import com.msjl.langgraph4j.voiceguide.graph.VoiceGuideGraphFactory;
import com.msjl.langgraph4j.voiceguide.service.VoiceCloneManifestService;
import com.msjl.langgraph4j.voiceguide.state.VoiceGuideState;
import org.bsc.langgraph4j.CompiledGraph;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(VoiceGuideProperties.class)
public class VoiceGuideConfiguration {

    @Bean
    public RestClient aliyunLlmRestClient(RestClient.Builder builder, VoiceGuideProperties properties) {
        VoiceGuideProperties.AliyunLlmProperties llm = properties.getLlm().getAliyun();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(Math.max(1, llm.getConnectTimeoutSeconds())));
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, llm.getReadTimeoutSeconds())));
        return builder.clone()
                .baseUrl(properties.getLlm().getAliyun().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public RestClient aliyunTtsRestClient(RestClient.Builder builder, VoiceGuideProperties properties) {
        VoiceGuideProperties.AliyunTtsProperties tts = properties.getTts().getAliyun();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(Math.max(1, tts.getConnectTimeoutSeconds())));
        requestFactory.setReadTimeout(Duration.ofSeconds(Math.max(1, tts.getReadTimeoutSeconds())));
        return builder.clone()
                .baseUrl(properties.getTts().getAliyun().getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public VoiceGuideModelGateway voiceGuideModelGateway(
            VoiceGuideProperties properties,
            RestClient aliyunLlmRestClient,
            ObjectMapper objectMapper
    ) {
        String provider = properties.getLlm().getProvider();
        if ("mock".equalsIgnoreCase(provider)) {
            return new MockVoiceGuideModelGateway();
        }
        if ("aliyun".equalsIgnoreCase(provider)) {
            return new AliyunChatModelGateway(aliyunLlmRestClient, properties, objectMapper);
        }
        throw new IllegalArgumentException("Unsupported voice guide llm provider: " + provider);
    }

    @Bean
    public VoiceGuideAsrGateway voiceGuideAsrGateway(
            VoiceGuideProperties properties,
            ObjectMapper objectMapper
    ) {
        String provider = properties.getAsr().getProvider();
        if ("mock".equalsIgnoreCase(provider)) {
            return new MockVoiceGuideAsrGateway();
        }
        if ("funasr".equalsIgnoreCase(provider)) {
            return new FunAsrWebSocketGateway(properties, objectMapper);
        }
        throw new IllegalArgumentException("Unsupported voice guide asr provider: " + provider);
    }

    @Bean
    public SpeechSynthesisGateway speechSynthesisGateway(
            VoiceGuideProperties properties,
            RestClient aliyunTtsRestClient,
            ObjectMapper objectMapper,
            VoiceCloneManifestService voiceCloneManifestService
    ) {
        String provider = properties.getTts().getProvider();
        if ("mock".equalsIgnoreCase(provider)) {
            return new MockSpeechSynthesisGateway();
        }
        if ("aliyun".equalsIgnoreCase(provider)) {
            SpeechSynthesisGateway primary = new AliyunSpeechSynthesisGateway(aliyunTtsRestClient, properties, objectMapper);
            SpeechSynthesisGateway vcRealtime = new AliyunRealtimeVcSpeechSynthesisGateway(properties, objectMapper, voiceCloneManifestService);
            AliyunVoiceCloneRealtimeGateway reserved = new AliyunVoiceCloneRealtimeGateway(properties, voiceCloneManifestService);
            return new RoutedSpeechSynthesisGateway(primary, vcRealtime, reserved, properties);
        }
        throw new IllegalArgumentException("Unsupported voice guide tts provider: " + provider);
    }

    @Bean
    public CompiledGraph<VoiceGuideState> voiceGuideCompiledGraph(
            VoiceGuideGraphFactory graphFactory
    ) throws Exception {
        return graphFactory.build();
    }

    @Bean
    @Primary
    public AsyncTaskExecutor voiceGuideStreamTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("voice-guide-stream-");
        executor.setConcurrencyLimit(8);
        return executor;
    }
}
