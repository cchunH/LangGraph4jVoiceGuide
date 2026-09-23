package com.msjl.langgraph4j.voiceguide.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideContext;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.ConversationTurn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class VoiceGuidePromptFactory {

    private static final String TEMPLATE_BASE_PATH = "classpath:prompts/voice-guidance/";

    private final Map<VoiceGuideExperienceType, VoiceGuidePromptTemplateDefinition> templateMap =
            new EnumMap<>(VoiceGuideExperienceType.class);

    public VoiceGuidePromptFactory(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        for (VoiceGuideExperienceType type : VoiceGuideExperienceType.values()) {
            templateMap.put(type, loadTemplate(type, resourceLoader, objectMapper));
        }
    }

    public String buildSystemPrompt(VoiceGuideExperienceType type) {
        VoiceGuidePromptTemplateDefinition template = getTemplate(type);
        VoiceGuidePromptTemplateDefinition.SystemPromptDefinition system = template.getSystem();
        return joinSections(
                formatSection("角色定位", system.getRole()),
                formatSection("核心目标", system.getObjective()),
                formatListSection("引导规则", system.getRules()),
                formatListSection("硬性约束", system.getConstraints()),
                formatListSection("执行流程", system.getWorkflow()),
                formatListSection("输出契约", system.getOutputContract()),
                formatSection("统一输出 Schema", buildStructuredOutputSchema())
        );
    }

    public String buildUserPrompt(VoiceGuideContext context) {
        VoiceGuidePromptTemplateDefinition template = getTemplate(context.getExperienceType());
        VoiceGuidePromptTemplateDefinition.UserPromptDefinition user = template.getUser();
        return joinSections(
                formatSection("任务板块", user.getSectionName()),
                formatSection(
                        "上下文信息",
                        "- 会话ID: " + defaultValue(context.getSessionId(), "未提供") + "\n"
                                + "- 简历ID: " + defaultValue(context.getResumeId() == null ? null : String.valueOf(context.getResumeId()), "未提供") + "\n"
                                + "- 目标岗位: " + defaultValue(context.getTargetJobTitle(), "未指定目标岗位") + "\n"
                                + "- 全局要求: " + defaultValue(context.getGlobalInstruction(), "无额外要求") + "\n"
                                + "- 板块类型: " + context.getExperienceType().name()
                ),
                formatListSection("输入检查项", user.getInputChecklist()),
                formatListSection("引导目标", user.getRewriteGoals()),
                formatSection("当前已有内容", defaultValue(context.getCurrentContent(), "当前还没有可用内容")),
                formatSection("用户最新回答", defaultValue(context.getUserLatestReply(), "用户尚未给出新的补充信息")),
                formatSection("历史对话", formatConversationHistory(context.getConversationHistory())),
                formatListSection("输出要求", user.getOutputRequirements()),
                formatSection("推荐输出模板", buildStructuredTemplate()),
                formatSection("参考样例", buildStructuredExample(context))
        );
    }

    public String buildFocus(VoiceGuideExperienceType type) {
        return getTemplate(type).getFocus();
    }

    private VoiceGuidePromptTemplateDefinition getTemplate(VoiceGuideExperienceType type) {
        VoiceGuidePromptTemplateDefinition template = templateMap.get(type);
        if (template == null) {
            throw new IllegalArgumentException("Voice guide prompt template not found for type: " + type);
        }
        return template;
    }

    private VoiceGuidePromptTemplateDefinition loadTemplate(
            VoiceGuideExperienceType type,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper
    ) {
        String fileName = type.name().toLowerCase(Locale.ROOT) + ".yml";
        Resource resource = resourceLoader.getResource(TEMPLATE_BASE_PATH + fileName);
        if (!resource.exists()) {
            throw new IllegalStateException("Prompt template file not found: " + TEMPLATE_BASE_PATH + fileName);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            Object yamlData = new Yaml().load(inputStream);
            return objectMapper.convertValue(yamlData, VoiceGuidePromptTemplateDefinition.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read voice guide prompt template for " + type, ex);
        }
    }

    private String formatSection(String title, String content) {
        return "## " + title + "\n" + content;
    }

    private String formatListSection(String title, List<String> items) {
        if (items == null || items.isEmpty()) {
            return formatSection(title, "无");
        }
        StringBuilder builder = new StringBuilder("## ").append(title).append("\n");
        for (String item : items) {
            builder.append("- ").append(item).append("\n");
        }
        return builder.toString().trim();
    }

    private String formatConversationHistory(List<ConversationTurn> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return "暂无历史对话";
        }
        StringBuilder builder = new StringBuilder();
        for (ConversationTurn turn : conversationHistory) {
            String role = turn.getRole() == null || turn.getRole().isBlank() ? "unknown" : turn.getRole();
            builder.append("- ").append(role).append(": ")
                    .append(defaultValue(turn.getContent(), ""))
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private String joinSections(String... sections) {
        return String.join("\n\n", sections);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String buildStructuredOutputSchema() {
        return """
                只输出 JSON 对象，不要输出 markdown。
                JSON 必须包含以下字段：
                - guideText: string
                - followUpQuestion: string
                - refinementDirection: string
                - suggestedRewrite: string
                - completionScore: integer (0-100)
                - isComplete: boolean
                - structuredState: object
                  - title: string
                  - organization: string
                  - role: string
                  - location: string
                  - timeRange: string
                  - background: string
                  - objective: string
                  - challenge: string
                  - action: string
                  - result: string
                  - technologies: string[]
                  - metrics: string[]
                  - missingFields: string[]
                判定规则：
                - 如果当前信息足以整理成完整经历，completionScore >= 85，isComplete=true
                - 如果仍缺少关键事实或量化结果，completionScore < 85，isComplete=false
                - missingFields 必须只保留当前仍缺失的关键信息
                """;
    }

    private String buildStructuredTemplate() {
        return """
                {
                  "guideText": "...",
                  "followUpQuestion": "...",
                  "refinementDirection": "...",
                  "suggestedRewrite": "...",
                  "completionScore": 72,
                  "isComplete": false,
                  "structuredState": {
                    "title": "...",
                    "organization": "...",
                    "role": "...",
                    "location": "...",
                    "timeRange": "...",
                    "background": "...",
                    "objective": "...",
                    "challenge": "...",
                    "action": "...",
                    "result": "...",
                    "technologies": ["..."],
                    "metrics": ["..."],
                    "missingFields": ["..."]
                  }
                }
                """;
    }

    private String buildStructuredExample(VoiceGuideContext context) {
        return """
                {
                  "guideText": "这段内容已经有基础了，我来帮你继续补最能拉开差距的一条信息。优先补一个可量化结果，或者补清你当时最关键的技术动作。",
                  "followUpQuestion": "那我们继续补最关键的一点吧，这次优化最后把哪个指标提升了多少？",
                  "refinementDirection": "补强量化结果和关键技术动作。",
                  "suggestedRewrite": "%s",
                  "completionScore": 68,
                  "isComplete": false,
                  "structuredState": {
                    "title": "",
                    "organization": "",
                    "role": "",
                    "location": "",
                    "timeRange": "",
                    "background": "%s",
                    "objective": "",
                    "challenge": "",
                    "action": "%s",
                    "result": "",
                    "technologies": [],
                    "metrics": [],
                    "missingFields": ["量化结果", "关键业务背景"]
                  }
                }
                """.formatted(
                defaultValue(context.getCurrentContent(), "请根据用户内容整理一版经历草稿"),
                defaultValue(context.getCurrentContent(), "暂无背景"),
                defaultValue(context.getUserLatestReply(), "暂无动作信息")
        );
    }
}
