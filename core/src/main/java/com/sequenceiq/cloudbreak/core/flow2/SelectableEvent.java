package com.sequenceiq.cloudbreak.core.flow2;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;

public class SelectableEvent implements Selectable {

    private final String selector;

    public SelectableEvent(String selector) {
        this.selector = selector;
    }
    @Override
    public String selector() {
        return selector;
    }
}
