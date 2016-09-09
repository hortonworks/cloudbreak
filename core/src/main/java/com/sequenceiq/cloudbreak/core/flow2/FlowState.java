package com.sequenceiq.cloudbreak.core.flow2;

public interface FlowState {
    Class<? extends AbstractAction> action();

    String name();
}
