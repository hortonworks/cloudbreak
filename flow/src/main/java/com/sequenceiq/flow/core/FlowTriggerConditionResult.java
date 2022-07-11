package com.sequenceiq.flow.core;

public class FlowTriggerConditionResult {

    private FlowTriggerConditionResultType flowTriggerConditionResultType;

    private String errorMessage;

    private FlowTriggerConditionResult(FlowTriggerConditionResultType flowTriggerConditionResultType) {
        this.flowTriggerConditionResultType = flowTriggerConditionResultType;
    }

    private FlowTriggerConditionResult(FlowTriggerConditionResultType flowTriggerConditionResultType, String errorMessage) {
        this.flowTriggerConditionResultType = flowTriggerConditionResultType;
        this.errorMessage = errorMessage;
    }

    public static FlowTriggerConditionResult ok() {
        return new FlowTriggerConditionResult(FlowTriggerConditionResultType.OK);
    }

    public static FlowTriggerConditionResult skip(String errorMessage) {
        return new FlowTriggerConditionResult(FlowTriggerConditionResultType.SKIP, errorMessage);
    }

    public static FlowTriggerConditionResult fail(String errorMessage) {
        return new FlowTriggerConditionResult(FlowTriggerConditionResultType.FAIL, errorMessage);
    }

    public FlowTriggerConditionResultType getFlowTriggerConditionResultType() {
        return flowTriggerConditionResultType;
    }

    public boolean isOk() {
        return FlowTriggerConditionResultType.OK == flowTriggerConditionResultType;
    }

    public boolean isSkip() {
        return FlowTriggerConditionResultType.SKIP == flowTriggerConditionResultType;
    }

    public boolean isFail() {
        return FlowTriggerConditionResultType.FAIL == flowTriggerConditionResultType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
