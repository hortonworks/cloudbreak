package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public abstract class AbstractUpgradeEmbeddedDBPreparationEvent extends StackEvent {

    private final TargetMajorVersion version;

    public AbstractUpgradeEmbeddedDBPreparationEvent(Long stackId, TargetMajorVersion version) {
        super(stackId);
        this.version = version;
    }

    public AbstractUpgradeEmbeddedDBPreparationEvent(String selector, Long stackId, TargetMajorVersion version) {
        super(selector, stackId);
        this.version = version;
    }

    public AbstractUpgradeEmbeddedDBPreparationEvent(String selector, Long stackId, Promise<AcceptResult> accepted, TargetMajorVersion version) {
        super(selector, stackId, accepted);
        this.version = version;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }
}
