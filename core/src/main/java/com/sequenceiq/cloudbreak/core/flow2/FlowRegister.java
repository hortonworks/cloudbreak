package com.sequenceiq.cloudbreak.core.flow2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

@Component
public class FlowRegister {
    private Map<String, Flow> runningFlows = new ConcurrentHashMap<>();

    public void put(Flow flow) {
        runningFlows.put(flow.getFlowId(), flow);
    }

    public Flow get(String flowId) {
        return runningFlows.get(flowId);
    }

    public Flow remove(String flowId) {
        return runningFlows.remove(flowId);
    }
}
