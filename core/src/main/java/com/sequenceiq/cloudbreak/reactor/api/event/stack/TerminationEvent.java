package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class TerminationEvent extends StackEvent {

    private Boolean deleteDependencies = Boolean.FALSE;

    private Boolean forced = Boolean.FALSE;

    public TerminationEvent(String selector, Long stackId, Boolean forced, Boolean deleteDependencies) {
        super(selector, stackId, null);
        this.deleteDependencies = deleteDependencies;
        this.forced = forced;
    }

    public Boolean getDeleteDependencies() {
        return deleteDependencies;
    }

    public Boolean getForced() {
        return forced;
    }
}
