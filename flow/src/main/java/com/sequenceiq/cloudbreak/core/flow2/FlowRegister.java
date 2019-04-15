package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.metrics.MetricService;

@Component
public class FlowRegister {

    @Inject
    private MetricService metricService;

    private final Map<String, Pair<Flow, String>> runningFlows = new ConcurrentHashMap<>();

    public void put(Flow flow, String chainFlowId) {
        runningFlows.put(flow.getFlowId(), new ImmutablePair<>(flow, chainFlowId));
        metricService.submit(FlowMetricType.ACTIVE_FLOWS, runningFlows.size());
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
        metricService.submit(FlowMetricType.ACTIVE_FLOWS, runningFlows.size());
        return pair == null ? null : pair.getLeft();
    }

    public Set<String> getRunningFlowIds() {
        return runningFlows.keySet();
    }
}
