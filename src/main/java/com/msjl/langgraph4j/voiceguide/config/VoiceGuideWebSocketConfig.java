package com.msjl.langgraph4j.voiceguide.config;

import com.msjl.langgraph4j.voiceguide.ws.VoiceGuideStreamWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
public class VoiceGuideWebSocketConfig implements WebSocketConfigurer {

    private static final int MAX_TEXT_MESSAGE_BYTES = 64 * 1024;
    private static final int MAX_BINARY_MESSAGE_BYTES = 1024 * 1024;

    private final VoiceGuideStreamWebSocketHandler voiceGuideStreamWebSocketHandler;

    public VoiceGuideWebSocketConfig(VoiceGuideStreamWebSocketHandler voiceGuideStreamWebSocketHandler) {
        this.voiceGuideStreamWebSocketHandler = voiceGuideStreamWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(voiceGuideStreamWebSocketHandler, "/api/langgraph4j/voice-guidance/ws/stream")
                .setAllowedOriginPatterns("*");
    }

    @Bean
    public ServletServerContainerFactoryBean webSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(MAX_TEXT_MESSAGE_BYTES);
        container.setMaxBinaryMessageBufferSize(MAX_BINARY_MESSAGE_BYTES);
        return container;
    }
}
