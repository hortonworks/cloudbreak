package com.sequenceiq.flow.core.model;

import java.util.StringJoiner;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;

public class FlowAcceptResult implements AcceptResult {

    private static final FlowAcceptResult ALREADY_EXISTING_FLOW_ACCEPT_RESULT = new FlowAcceptResult(ResultType.ALREADY_EXISTING_FLOW, null);

    private final ResultType resultType;

    private final String pollableId;

    public FlowAcceptResult(ResultType resultType, String pollableId) {
        this.resultType = resultType;
        this.pollableId = pollableId;
    }

    public static FlowAcceptResult alreadyExistingFlow() {
        return ALREADY_EXISTING_FLOW_ACCEPT_RESULT;
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

    @Override
    public String toString() {
        return new StringJoiner(", ", FlowAcceptResult.class.getSimpleName() + "[", "]")
                .add("resultType=" + resultType)
                .add("pollableId='" + pollableId + "'")
                .toString();
    }
}
