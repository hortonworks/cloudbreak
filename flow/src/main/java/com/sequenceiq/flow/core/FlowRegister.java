package com.sequenceiq.flow.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.metrics.MetricService;

@Component
public class FlowRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowRegister.class);

    @Inject
    private MetricService metricService;

    private Map<String, Pair<Flow, String>> runningFlows;

    @PostConstruct
    public void init() {
        runningFlows = metricService.gaugeMapSize(FlowMetricType.ACTIVE_FLOWS, new ConcurrentHashMap<>());
    }

    public void put(Flow flow, String chainFlowId) {
        LOGGER.info("Put flow {} to running flows", flow.getFlowId());
        runningFlows.put(flow.getFlowId(), new ImmutablePair<>(flow, chainFlowId));
        LOGGER.info("Running flows after put: {}", runningFlows.keySet());
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
        LOGGER.info("Remove flow {} from running flows", flowId);
        Pair<Flow, String> pair = runningFlows.remove(flowId);
        metricService.submit(FlowMetricType.ACTIVE_FLOWS, runningFlows.size());
        LOGGER.info("Running flows after removal: {}", runningFlows.keySet());
        return pair == null ? null : pair.getLeft();
    }

    public Set<String> getRunningFlowIds() {
        return runningFlows.keySet();
    }
}
