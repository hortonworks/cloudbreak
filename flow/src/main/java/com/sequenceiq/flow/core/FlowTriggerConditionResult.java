package com.sequenceiq.flow.core;

public class FlowTriggerConditionResult {

    public static final FlowTriggerConditionResult OK = new FlowTriggerConditionResult();

    private boolean triggerable;

    private String errorMessage;

    private FlowTriggerConditionResult() {
        this.triggerable = true;
    }

    public FlowTriggerConditionResult(String errorMessage) {
        this.triggerable = false;
        this.errorMessage = errorMessage;
    }

    public boolean isTriggerable() {
        return triggerable;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
