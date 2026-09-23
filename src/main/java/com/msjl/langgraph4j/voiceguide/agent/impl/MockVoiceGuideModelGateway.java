package com.msjl.langgraph4j.voiceguide.agent.impl;

import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideModelGateway;
import com.msjl.langgraph4j.voiceguide.domain.enums.VoiceGuideExperienceType;
import com.msjl.langgraph4j.voiceguide.domain.model.ExperienceStructuredState;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideModelResult;

import java.util.List;

public class MockVoiceGuideModelGateway implements VoiceGuideModelGateway {

    @Override
    public VoiceGuideModelResult generate(String systemPrompt, String userPrompt, VoiceGuideExperienceType experienceType) {
        VoiceGuideModelResult result = new VoiceGuideModelResult();
        switch (experienceType) {
            case EDUCATION -> {
                result.setGuideText("我先帮你补教育经历。请补充学校层次、核心课程或 GPA 中最能证明竞争力的一项信息。");
                result.setFollowUpQuestion("你最想突出的是 GPA、排名、课程项目还是竞赛成果？请给我一个具体数据。");
                result.setRefinementDirection("把教育经历补成学校背景 + 专业能力 + 证明性数据。");
                result.setSuggestedRewrite("2020.09-2024.06 XX大学 计算机科学与技术 本科，GPA 3.8/4.0，核心课程包含数据结构、操作系统、计算机网络。");
                result.setStructuredState(structured(
                        "教育经历", "XX大学", "", "", "2020.09-2024.06",
                        "计算机科学与技术本科背景", "", "", "核心课程整理", "GPA 3.8/4.0",
                        List.of("数据结构", "操作系统", "计算机网络"),
                        List.of("GPA 3.8/4.0"),
                        List.of("排名", "竞赛成果")
                ));
                result.setCompletionScore(76);
                result.setComplete(false);
            }
            case INTERNSHIP_EXPERIENCE -> {
                result.setGuideText("这段实习还缺业务场景和量化结果。请补充你解决的具体问题、使用的技术方案，以及至少一个效果数据。");
                result.setFollowUpQuestion("你当时优化了哪个指标，比如响应时间、转化率、错误率或效率？给我一个大致提升幅度。");
                result.setRefinementDirection("把实习经历补成业务背景 + 技术动作 + 量化收益。");
                result.setSuggestedRewrite("在某业务场景中，通过接口治理与索引优化降低请求耗时，使核心接口平均响应时间缩短约 25%。");
                result.setStructuredState(structured(
                        "接口优化实习", "某互联网公司", "后端开发实习生", "", "",
                        "参与接口开发和性能优化", "优化接口性能", "缺少明确业务场景",
                        "接口治理与索引优化", "核心接口平均响应时间缩短约25%",
                        List.of("Java", "MySQL", "索引优化"),
                        List.of("响应时间缩短约25%"),
                        List.of("公司名称", "时间范围", "业务场景")
                ));
                result.setCompletionScore(70);
                result.setComplete(false);
            }
            case WORK_EXPERIENCE -> {
                result.setGuideText("工作经历需要更清楚地体现职责边界和业务结果。请补充你主导的模块、采用的方案，以及最终带来的业务提升。");
                result.setFollowUpQuestion("这段经历里，你最能证明价值的一个结果是什么？最好给出百分比、耗时或规模数据。");
                result.setRefinementDirection("把工作经历补成职责范围 + 方法方案 + 结果影响。");
                result.setSuggestedRewrite("负责订单系统核心链路治理，通过异步解耦与缓存预热优化高峰吞吐，使峰值成功率提升约 18%。");
                result.setStructuredState(structured(
                        "订单系统治理", "", "", "", "",
                        "负责订单系统核心链路", "提升高峰稳定性", "",
                        "异步解耦与缓存预热", "峰值成功率提升约18%",
                        List.of("异步解耦", "缓存预热"),
                        List.of("成功率提升约18%"),
                        List.of("公司名称", "岗位", "时间范围")
                ));
                result.setCompletionScore(82);
                result.setComplete(false);
            }
            case PROJECT_EXPERIENCE -> {
                result.setGuideText("项目经历要更像可讲述的方案案例。请补充项目目标、你负责的核心模块、使用的算法或技术原理，以及最终结果。");
                result.setFollowUpQuestion("你在这个项目里最核心的技术动作是什么？它把准确率、性能或效率提升了多少？");
                result.setRefinementDirection("把项目经历补成挑战 + 任务 + 行动 + 结果。");
                result.setSuggestedRewrite("围绕推荐场景搭建召回与排序链路，通过向量检索与特征重排提升候选集相关性，使点击率提升约 12%。");
                result.setStructuredState(structured(
                        "推荐系统项目", "", "", "", "",
                        "围绕推荐场景进行召回与排序优化", "提升候选集相关性", "",
                        "向量检索与特征重排", "点击率提升约12%",
                        List.of("向量检索", "特征重排"),
                        List.of("点击率提升约12%"),
                        List.of("项目时间", "角色", "项目目标细节")
                ));
                result.setCompletionScore(78);
                result.setComplete(false);
            }
            default -> throw new IllegalArgumentException("Unsupported experience type: " + experienceType);
        }
        return result;
    }

    private ExperienceStructuredState structured(
            String title,
            String organization,
            String role,
            String location,
            String timeRange,
            String background,
            String objective,
            String challenge,
            String action,
            String result,
            List<String> technologies,
            List<String> metrics,
            List<String> missingFields
    ) {
        ExperienceStructuredState state = new ExperienceStructuredState();
        state.setTitle(title);
        state.setOrganization(organization);
        state.setRole(role);
        state.setLocation(location);
        state.setTimeRange(timeRange);
        state.setBackground(background);
        state.setObjective(objective);
        state.setChallenge(challenge);
        state.setAction(action);
        state.setResult(result);
        state.setTechnologies(technologies);
        state.setMetrics(metrics);
        state.setMissingFields(missingFields);
        return state;
    }
}
