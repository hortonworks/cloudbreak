package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.embeddeddb;

import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.AbstractUpgradeRdsEvent;

public class UpgradeDatabaseInitRequest extends AbstractUpgradeRdsEvent {

    public UpgradeDatabaseInitRequest(String selector, Long stackId, TargetMajorVersion version) {
        super(selector, stackId, version);
    }
}
