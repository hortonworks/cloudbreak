package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeRdsDataRestoreRequest extends AbstractUpgradeRdsEvent {

    @JsonCreator
    public UpgradeRdsDataRestoreRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version) {
        super(stackId, version);
    }

}
