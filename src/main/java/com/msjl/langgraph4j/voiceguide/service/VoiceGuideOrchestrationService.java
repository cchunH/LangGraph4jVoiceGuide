package com.msjl.langgraph4j.voiceguide.service;

import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.agent.VoiceGuideAsrGateway;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideAudioRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideRequest;
import com.msjl.langgraph4j.voiceguide.domain.VoiceGuideResponse;
import com.msjl.langgraph4j.voiceguide.domain.model.AsrTranscriptionResult;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideAudio;
import com.msjl.langgraph4j.voiceguide.domain.model.VoiceGuideDraft;
import com.msjl.langgraph4j.voiceguide.state.VoiceGuideState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class VoiceGuideOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(VoiceGuideOrchestrationService.class);

    private final CompiledGraph<VoiceGuideState> compiledGraph;
    private final VoiceGuideProperties properties;
    private final VoiceGuideAsrGateway voiceGuideAsrGateway;

    public VoiceGuideOrchestrationService(
            CompiledGraph<VoiceGuideState> compiledGraph,
            VoiceGuideProperties properties,
            VoiceGuideAsrGateway voiceGuideAsrGateway
    ) {
        this.compiledGraph = compiledGraph;
        this.properties = properties;
        this.voiceGuideAsrGateway = voiceGuideAsrGateway;
    }

    public VoiceGuideResponse generate(VoiceGuideRequest request) throws Exception {
        log.info("开始执行语音引导 Agent | graphId={} | sessionId={} | experienceType={}",
                properties.getGraphId(),
                request.getSessionId(),
                request.getExperienceType());

        Map<String, Object> initialState = new LinkedHashMap<>();
        initialState.put(VoiceGuideState.SESSION_ID, request.getSessionId());
        initialState.put(VoiceGuideState.RESUME_ID, request.getResumeId());
        initialState.put(VoiceGuideState.TARGET_JOB_TITLE, request.getTargetJobTitle());
        initialState.put(VoiceGuideState.EXPERIENCE_TYPE, request.getExperienceType());
        initialState.put(VoiceGuideState.CURRENT_CONTENT, request.getCurrentContent());
        initialState.put(VoiceGuideState.USER_LATEST_REPLY, request.getUserLatestReply());
        initialState.put(VoiceGuideState.GLOBAL_INSTRUCTION, request.getGlobalInstruction());
        initialState.put(VoiceGuideState.CONVERSATION_HISTORY, request.getConversationHistory());
        initialState.put(VoiceGuideState.INCLUDE_AUDIO, request.getIncludeAudio());

        RunnableConfig runnableConfig = RunnableConfig.builder()
                .threadId("voice-guide-" + UUID.randomUUID())
                .build();

        VoiceGuideState state = compiledGraph.invoke(initialState, runnableConfig)
                .orElseThrow(() -> new IllegalStateException("Graph execution returned no final state"));

        VoiceGuideDraft draft = state.guideDraft()
                .orElseThrow(() -> new IllegalStateException("Guide draft missing"));
        VoiceGuideAudio audio = state.guideAudio().orElse(null);

        VoiceGuideResponse response = new VoiceGuideResponse();
        response.setSessionId(state.sessionId().orElse(null));
        response.setResumeId(state.resumeId().orElse(null));
        response.setGraphId(properties.getGraphId());
        response.setExperienceType(state.experienceType().orElse(null));
        response.setGuideText(draft.getGuideText());
        response.setFollowUpQuestion(draft.getFollowUpQuestion());
        response.setRefinementDirection(draft.getRefinementDirection());
        response.setSuggestedRewrite(draft.getSuggestedRewrite());
        response.setStructuredState(draft.getStructuredState());
        response.setCompletionScore(draft.getCompletionScore());
        response.setComplete(draft.getComplete());
        response.setAgentName(draft.getAgentName());
        response.setAudio(audio);
        response.setSummary(state.summary().orElse("No summary produced"));
        response.setExecutionLogs(state.executionLogs());

        log.info("语音引导 Agent 执行结束 | sessionId={} | agent={} | hasAudio={}",
                response.getSessionId(),
                response.getAgentName(),
                response.getAudio() != null);
        return response;
    }

    public VoiceGuideResponse generateFromAudio(
            VoiceGuideAudioRequest request,
            byte[] audioBytes,
            String originalFilename,
            String contentType
    ) throws Exception {
        log.info("开始执行语音引导 ASR 转写 | sessionId={} | experienceType={} | filename={} | contentType={} | bytes={}",
                request.getSessionId(),
                request.getExperienceType(),
                originalFilename,
                contentType,
                audioBytes == null ? 0 : audioBytes.length);

        AsrTranscriptionResult asrResult = voiceGuideAsrGateway.transcribe(
                audioBytes,
                originalFilename,
                contentType,
                request.getHotwords()
        );

        VoiceGuideRequest textRequest = new VoiceGuideRequest();
        textRequest.setSessionId(request.getSessionId());
        textRequest.setResumeId(request.getResumeId());
        textRequest.setTargetJobTitle(request.getTargetJobTitle());
        textRequest.setExperienceType(request.getExperienceType());
        textRequest.setCurrentContent(request.getCurrentContent());
        textRequest.setUserLatestReply(asrResult.getText());
        textRequest.setGlobalInstruction(request.getGlobalInstruction());
        textRequest.setConversationHistory(request.getConversationHistory());
        textRequest.setIncludeAudio(request.getIncludeAudio());

        VoiceGuideResponse response = generate(textRequest);
        response.setAsr(asrResult);
        return response;
    }
}
