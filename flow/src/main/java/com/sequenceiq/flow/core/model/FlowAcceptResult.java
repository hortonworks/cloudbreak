package com.sequenceiq.flow.core.model;

import java.util.Collections;
import java.util.Set;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.flow.domain.FlowLogIdWithTypeAndTimestamp;

public class FlowAcceptResult implements AcceptResult {

    private final ResultType resultType;

    private final String pollableId;

    private final Set<FlowLogIdWithTypeAndTimestamp> alreadyRunningFlows;

    public FlowAcceptResult(ResultType resultType, String pollableId) {
        this.resultType = resultType;
        this.pollableId = pollableId;
        this.alreadyRunningFlows = Collections.EMPTY_SET;
    }

    public FlowAcceptResult(ResultType resultType, Set<FlowLogIdWithTypeAndTimestamp> alreadyRunningFlows) {
        this.resultType = resultType;
        this.pollableId = null;
        this.alreadyRunningFlows = alreadyRunningFlows;
    }

    public static FlowAcceptResult alreadyExistingFlow(Set<FlowLogIdWithTypeAndTimestamp> alreadyRunningFlows) {
        return new FlowAcceptResult(ResultType.ALREADY_EXISTING_FLOW, alreadyRunningFlows);
    }

    public static FlowAcceptResult runningInFlow(String flowId) {
        return new FlowAcceptResult(ResultType.RUNNING_IN_FLOW, flowId);
    }

    public static FlowAcceptResult runningInFlowChain(String flowChainId) {
        return new FlowAcceptResult(ResultType.RUNNING_IN_FLOW_CHAIN, flowChainId);
    }

    public ResultType getResultType() {
        return resultType;
    }

    public Set<FlowLogIdWithTypeAndTimestamp> getAlreadyRunningFlows() {
        return alreadyRunningFlows;
    }

    public String getAsFlowId() {
        if (!ResultType.RUNNING_IN_FLOW.equals(resultType)) {
            throw new IllegalStateException("Can't handle " + resultType + " as flow.");
        }
        return pollableId;
    }

    public String getAsFlowChainId() {
        if (!ResultType.RUNNING_IN_FLOW_CHAIN.equals(resultType)) {
            throw new IllegalStateException("Can't handle " + resultType + " as flow chain.");
        }
        return pollableId;
    }
}
