package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public abstract class AbstractUpgradeEmbeddedDbEvent extends StackEvent {

    private final TargetMajorVersion version;

    public AbstractUpgradeEmbeddedDbEvent(Long stackId, TargetMajorVersion version) {
        super(stackId);
        this.version = version;
    }

    public AbstractUpgradeEmbeddedDbEvent(String selector, Long stackId, TargetMajorVersion version) {
        super(selector, stackId);
        this.version = version;
    }

    public AbstractUpgradeEmbeddedDbEvent(String selector, Long stackId, Promise<AcceptResult> accepted, TargetMajorVersion version) {
        super(selector, stackId, accepted);
        this.version = version;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }
}
