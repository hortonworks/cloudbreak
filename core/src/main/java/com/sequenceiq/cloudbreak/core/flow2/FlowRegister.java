package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

@Component
public class FlowRegister {
    private Map<String, Pair<Flow, String>> runningFlows = new ConcurrentHashMap<>();

    public void put(Flow flow, String chainFlowId) {
        runningFlows.put(flow.getFlowId(), new ImmutablePair<>(flow, chainFlowId));
    }

    public Flow get(String flowId) {
        Pair<Flow, String> p = runningFlows.get(flowId);
        return p != null ? p.getLeft() : null;
    }

    public String getFlowChainId(String flowId) {
        Pair<Flow, String> p = runningFlows.get(flowId);
        return p != null ? p.getRight() : null;
    }

    public Flow remove(String flowId) {
        Pair<Flow, String> pair = runningFlows.remove(flowId);
        return pair == null ? null : pair.getLeft();
    }
}
