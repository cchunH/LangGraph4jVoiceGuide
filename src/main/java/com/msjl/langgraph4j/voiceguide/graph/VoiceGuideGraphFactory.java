package com.msjl.langgraph4j.voiceguide.graph;

import com.msjl.langgraph4j.voiceguide.config.VoiceGuideProperties;
import com.msjl.langgraph4j.voiceguide.node.CompletionDecisionNode;
import com.msjl.langgraph4j.voiceguide.node.ExperienceGuideNode;
import com.msjl.langgraph4j.voiceguide.node.SpeechSynthesisNode;
import com.msjl.langgraph4j.voiceguide.node.VoiceGuideFinalizeNode;
import com.msjl.langgraph4j.voiceguide.node.VoiceGuideIntakeNode;
import com.msjl.langgraph4j.voiceguide.node.VoiceGuideNodeNames;
import com.msjl.langgraph4j.voiceguide.state.VoiceGuideState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
public class VoiceGuideGraphFactory {

    private final VoiceGuideProperties properties;
    private final VoiceGuideIntakeNode intakeNode;
    private final ExperienceGuideNode experienceGuideNode;
    private final CompletionDecisionNode completionDecisionNode;
    private final SpeechSynthesisNode speechSynthesisNode;
    private final VoiceGuideFinalizeNode finalizeNode;

    public VoiceGuideGraphFactory(
            VoiceGuideProperties properties,
            VoiceGuideIntakeNode intakeNode,
            ExperienceGuideNode experienceGuideNode,
            CompletionDecisionNode completionDecisionNode,
            SpeechSynthesisNode speechSynthesisNode,
            VoiceGuideFinalizeNode finalizeNode
    ) {
        this.properties = properties;
        this.intakeNode = intakeNode;
        this.experienceGuideNode = experienceGuideNode;
        this.completionDecisionNode = completionDecisionNode;
        this.speechSynthesisNode = speechSynthesisNode;
        this.finalizeNode = finalizeNode;
    }

    public CompiledGraph<VoiceGuideState> build() throws Exception {
        StateGraph<VoiceGuideState> graph = new StateGraph<>(VoiceGuideState.SCHEMA, VoiceGuideState::new)
                .addNode(VoiceGuideNodeNames.INTAKE, node_async(intakeNode))
                .addNode(VoiceGuideNodeNames.GUIDE, node_async(experienceGuideNode))
                .addNode(VoiceGuideNodeNames.COMPLETE_CHECK, node_async(completionDecisionNode))
                .addNode(VoiceGuideNodeNames.SYNTHESIZE, node_async(speechSynthesisNode))
                .addNode(VoiceGuideNodeNames.FINALIZE, node_async(finalizeNode))
                .addEdge(START, VoiceGuideNodeNames.INTAKE)
                .addEdge(VoiceGuideNodeNames.INTAKE, VoiceGuideNodeNames.GUIDE)
                .addEdge(VoiceGuideNodeNames.GUIDE, VoiceGuideNodeNames.COMPLETE_CHECK)
                .addEdge(VoiceGuideNodeNames.COMPLETE_CHECK, VoiceGuideNodeNames.SYNTHESIZE)
                .addEdge(VoiceGuideNodeNames.SYNTHESIZE, VoiceGuideNodeNames.FINALIZE)
                .addEdge(VoiceGuideNodeNames.FINALIZE, END);

        return graph.compile(
                CompileConfig.builder()
                        .graphId(properties.getGraphId())
                        .build()
        );
    }
}
