package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterRepairTriggerEvent extends StackEvent {

    private final Map<String, List<String>> failedNodesMap;

    private final boolean removeOnly;

    private final Stack stack;

    public ClusterRepairTriggerEvent(Stack stack, Map<String, List<String>> failedNodesMap, boolean removeOnly) {
        super(stack.getId());
        this.failedNodesMap = failedNodesMap;
        this.removeOnly = removeOnly;
        this.stack = stack;
    }

    public Map<String, List<String>> getFailedNodesMap() {
        return failedNodesMap;
    }

    public boolean isRemoveOnly() {
        return removeOnly;
    }

    public Stack getStack() {
        return stack;
    }
}
