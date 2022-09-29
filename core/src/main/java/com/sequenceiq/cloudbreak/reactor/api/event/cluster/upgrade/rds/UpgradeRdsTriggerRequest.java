package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeRdsTriggerRequest extends AbstractUpgradeRdsEvent {

    private final String backupLocation;

    @JsonCreator
    public UpgradeRdsTriggerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version,
            @JsonProperty("backupLocation") String backupLocation) {
        super(selector, stackId, version);
        this.backupLocation = backupLocation;
    }

    public String getBackupLocation() {
        return backupLocation;
    }
}
