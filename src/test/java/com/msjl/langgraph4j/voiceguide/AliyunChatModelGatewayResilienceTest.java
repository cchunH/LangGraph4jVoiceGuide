package com.msjl.langgraph4j.voiceguide;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.agent.impl.AliyunChatModelGateway;
import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideModelResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AliyunChatModelGatewayResilienceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private VoiceGuideProperties properties;
    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private AliyunChatModelGateway gateway;

    @BeforeEach
    void setUp() {
        properties = new VoiceGuideProperties();
        properties.getLlm().getAliyun().setApiKey("test-key");
        properties.getLlm().getAliyun().setBaseUrl("https://dashscope.test/v1");
        properties.getLlm().getAliyun().setMaxAttempts(3);
        properties.getLlm().getAliyun().setRetryInitialDelayMillis(0);
        builder = RestClient.builder().baseUrl("https://dashscope.test/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        gateway = new AliyunChatModelGateway(builder.build(), properties, objectMapper);
    }

    @Test
    void parsesJsonCodeBlockEvenWhenModelAddsProse() {
        expectSuccess("模型结果如下：\n```json\n" + validContent("代码块结果") + "\n```\n请查收。");

        VoiceGuideModelResult result = generate();

        assertEquals("代码块结果", result.getSuggestedRewrite());
        server.verify();
    }

    @Test
    void retriesAfterMalformedJson() {
        expectSuccess("{\"guideText\":\"未结束");
        expectSuccess(validContent("重试成功"));

        VoiceGuideModelResult result = generate();

        assertEquals("重试成功", result.getSuggestedRewrite());
        server.verify();
    }

    @Test
    void retriesAfterRetryableHttpFailure() {
        server.expect(once(), requestTo("https://dashscope.test/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        expectSuccess(validContent("服务恢复"));

        VoiceGuideModelResult result = generate();

        assertEquals("服务恢复", result.getSuggestedRewrite());
        server.verify();
    }

    @Test
    void retriesWhenRequiredFieldsAreMissing() {
        expectSuccess("{\"guideText\":\"只有引导语\"}");
        expectSuccess(validContent("字段补全"));

        VoiceGuideModelResult result = generate();

        assertEquals("字段补全", result.getSuggestedRewrite());
        server.verify();
    }

    private VoiceGuideModelResult generate() {
        return gateway.generate("system", "user", VoiceGuideExperienceType.EDUCATION);
    }

    private void expectSuccess(String content) {
        server.expect(once(), requestTo("https://dashscope.test/v1/chat/completions"))
                .andRespond(withSuccess(response(content), MediaType.APPLICATION_JSON));
    }

    private String validContent(String rewrite) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "guideText", "继续补充信息",
                    "followUpQuestion", "结果提升了多少？",
                    "refinementDirection", "补充量化结果",
                    "suggestedRewrite", rewrite,
                    "completionScore", 75,
                    "isComplete", false
            ));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private String response(String content) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", content)))
            ));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
