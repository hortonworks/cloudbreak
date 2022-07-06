package com.sequenceiq.cloudbreak.common.event;

public interface Selectable extends Payload {
    String selector();

    default String getSelector() {
        return selector();
    }
}
