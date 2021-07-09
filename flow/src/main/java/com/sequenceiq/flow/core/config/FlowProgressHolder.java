package com.sequenceiq.flow.core.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.sequenceiq.flow.core.FlowConstants;

@Component
public class FlowProgressHolder {

    private static final int UNKNOWN_PERCENT = -1;

    private static final int MIN_PERCENT = 0;

    private static final int MAX_PERCENT = 100;

    private final Map<String, Map<String, Integer>> flowProgressState = new HashMap<>();

    private final Map<String, Integer> transitionsSizeMap = new HashMap<>();

    private final List<? extends AbstractFlowConfiguration> flowConfigurations;

    public FlowProgressHolder(List<? extends AbstractFlowConfiguration> flowConfigurations) {
        this.flowConfigurations = flowConfigurations;
    }

    @PostConstruct
    public void init() {
        for (AbstractFlowConfiguration flowConfiguration : flowConfigurations) {
            String flowKey = flowConfiguration.getClass().getCanonicalName();
            int numberOfTransitions = flowConfiguration.getTransitions().size() + 1;
            Map<String, Integer> progressMap = new HashMap<>();
            AbstractFlowConfiguration.FlowEdgeConfig edgeConfig = flowConfiguration.getEdgeConfig();
            progressMap.put(edgeConfig.getInitState().toString(), MIN_PERCENT);
            progressMap.put(edgeConfig.getFinalState().toString(), MAX_PERCENT);
            progressMap.put(edgeConfig.getDefaultFailureState().toString(), MAX_PERCENT);
            List<AbstractFlowConfiguration.Transition> transitionList = flowConfiguration.getTransitions();
            transitionsSizeMap.put(flowKey, transitionList.size());
            Iterator<AbstractFlowConfiguration.Transition> it = transitionList.iterator();
            for (int index = 0; it.hasNext(); index++) {
                AbstractFlowConfiguration.Transition transition = it.next();
                double progress = calculatePercentage(index + 1, numberOfTransitions);
                if (transition.getFailureState() != null && !progressMap.containsKey(transition.getFailureState().toString())) {
                    // TODO: here, the actual percent can be used as well?
                    progressMap.put(transition.getFailureState().toString(), MAX_PERCENT);
                }
                if (!progressMap.containsKey(transition.getSource().toString())) {
                    progressMap.put(transition.getSource().toString(), (int) progress);
                }
            }
            flowProgressState.put(flowKey, progressMap);
        }
    }

    public int getProgressPercentageForState(Class<? extends AbstractFlowConfiguration> clazz, String state) {
        return getProgressPercentageForState(clazz.getCanonicalName(), state);
    }

    public int getProgressPercentageForState(String className, String state) {
        int result = UNKNOWN_PERCENT;
        if (!flowProgressState.containsKey(className)) {
            return result;
        }
        if (flowProgressState.get(className).containsKey(state)) {
            result = flowProgressState.get(className).getOrDefault(state, UNKNOWN_PERCENT);
        } else if (FlowConstants.INIT_STATE.equals(state)) {
            result = MIN_PERCENT;
        } else if (isFlowCompleted(state)) {
            result = MAX_PERCENT;
        }
        return result;
    }

    public int getTransitionsSize(String className) {
        return transitionsSizeMap.getOrDefault(className, 0);
    }

    private double calculatePercentage(double obtained, double total) {
        return obtained * MAX_PERCENT / total;
    }

    private boolean isFlowCompleted(String state) {
        return FlowConstants.CANCELLED_STATE.equals(state)
                || FlowConstants.TERMINATED_STATE.equals(state)
                || FlowConstants.FINISHED_STATE.equals(state);
    }
}
