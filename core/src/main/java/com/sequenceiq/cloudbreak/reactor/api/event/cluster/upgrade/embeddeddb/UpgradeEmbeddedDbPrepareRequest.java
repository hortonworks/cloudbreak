package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.AbstractUpgradeRdsEvent;

public class UpgradeEmbeddedDbPrepareRequest extends AbstractUpgradeRdsEvent {

    public UpgradeEmbeddedDbPrepareRequest(Long stackId, TargetMajorVersion version) {
        super(stackId, version);
    }
}
