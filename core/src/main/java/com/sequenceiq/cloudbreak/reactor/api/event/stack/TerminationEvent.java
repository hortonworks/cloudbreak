package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class TerminationEvent extends StackEvent {

    private Boolean deleteDependencies = Boolean.FALSE;

    public TerminationEvent(Long stackId, Boolean deleteDependencies) {
        super(stackId);
        this.deleteDependencies = deleteDependencies;
    }

    public TerminationEvent(String selector, Long stackId, Boolean deleteDependencies) {
        super(selector, stackId);
        this.deleteDependencies = deleteDependencies;
    }

    public TerminationEvent(String selector, Long stackId, Promise<Boolean> accepted, Boolean deleteDependencies) {
        super(selector, stackId, accepted);
        this.deleteDependencies = deleteDependencies;
    }

    public Boolean getDeleteDependencies() {
        return deleteDependencies;
    }
}
