package com.sequenceiq.flow.conf;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Configuration;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;

@Configuration
public class FlowProgressHolder {

    private final Map<String, Map<String, Integer>> flowProgressState = new HashMap<>();

    private final List<? extends AbstractFlowConfiguration> flowConfigurations;

    public FlowProgressHolder(List<? extends AbstractFlowConfiguration> flowConfigurations) {
        this.flowConfigurations = flowConfigurations;
    }

    @PostConstruct
    public void init() {
        for (AbstractFlowConfiguration flowConfiguration : flowConfigurations) {
            String flowKey = flowConfiguration.getClass().getCanonicalName();
            int numberOfTransitions = flowConfiguration.getMyTransitions().size() + 1;
            Map<String, Integer> progressMap = new HashMap<>();
            Iterator<AbstractFlowConfiguration.Transition> it = flowConfiguration.getMyTransitions().iterator();
            AbstractFlowConfiguration.FlowEdgeConfig edgeConfig = flowConfiguration.getMyEdgeConfig();
            progressMap.put(edgeConfig.getInitState().toString(), 0);
            progressMap.put(edgeConfig.getFinalState().toString(), 100);
            //progressMap.put(edgeConfig.getDefaultFailureState().toString(), 100);
            for (int index = 0; it.hasNext(); index++) {
                AbstractFlowConfiguration.Transition transition = it.next();
                double progress = calculatePercentage(index + 1, numberOfTransitions);
                progressMap.put(transition.getSourceName(), (int) progress);
            }
            flowProgressState.put(flowKey, progressMap);
        }
    }

    public double calculatePercentage(double obtained, double total) {
        return obtained * 100 / total;
    }

    public int getProgressForFlow(String flowConfig, String currentState) {
        return 0;
    }

    public int getProgressForFlowChain(String flowConfig) {
        return 0;
    }
}
