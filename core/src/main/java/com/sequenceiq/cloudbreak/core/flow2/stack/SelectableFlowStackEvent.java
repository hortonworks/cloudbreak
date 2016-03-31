package com.sequenceiq.cloudbreak.core.flow2.stack;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;

public class SelectableFlowStackEvent extends FlowStackEvent implements Selectable {
    private final String selector;

    public SelectableFlowStackEvent(Long stackId, String selector) {
        super(stackId);
        this.selector = selector;
    }

    @Override
    public String selector() {
        return selector;
    }
}
