package com.sequenceiq.flow.core;

public interface RestartAction {

    void restart(String flowId, String flowChainId, String event, Object payload);
}
