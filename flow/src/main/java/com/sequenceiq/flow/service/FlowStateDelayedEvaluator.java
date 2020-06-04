package com.sequenceiq.flow.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FlowStateDelayedEvaluator {

    @Value("${flow.completion.delay:10000}")
    private long delayInMillis;

    private final Map<String, Long> completedAt = new ConcurrentHashMap<>();

    public boolean isComplete(String flowOrFlowChainId, boolean completed) {
        if (completed) {
            long currentTime  = System.currentTimeMillis();
            long completionTime = completedAt.computeIfAbsent(flowOrFlowChainId, key -> currentTime);
            if (currentTime - completionTime >= delayInMillis) {
                completedAt.remove(flowOrFlowChainId);
                return true;
            } else {
                return false;
            }
        } else {
            completedAt.remove(flowOrFlowChainId);
            return false;
        }
    }
}
