package com.sequenceiq.flow.core;

public interface RestartAction {

    void restart(RestartContext restartContext, Object payload);
}
