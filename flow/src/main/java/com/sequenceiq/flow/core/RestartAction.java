package com.sequenceiq.flow.core;

public interface RestartAction {

    void restart(FlowParameters flowParameters, String flowChainId, String event, Object payload);
}
