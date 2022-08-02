package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.AbstractUpgradeRdsEvent;

import reactor.rx.Promise;

public class UpgradeEmbeddedDbPrepareTriggerRequest extends AbstractUpgradeRdsEvent {

    public UpgradeEmbeddedDbPrepareTriggerRequest(Long stackId, TargetMajorVersion version) {
        super(stackId, version);
    }

    public UpgradeEmbeddedDbPrepareTriggerRequest(String selector, Long stackId, TargetMajorVersion version) {
        super(selector, stackId, version);
    }

    public UpgradeEmbeddedDbPrepareTriggerRequest(String selector, Long stackId, Promise<AcceptResult> accepted, TargetMajorVersion version) {
        super(selector, stackId, accepted, version);
    }
}
