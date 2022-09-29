package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeRdsDataBackupRequest extends AbstractUpgradeRdsEvent {

    private final String backupLocation;

    @JsonCreator
    public UpgradeRdsDataBackupRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version,
            @JsonProperty("backupLocation") String backupLocation) {
        super(stackId, version);
        this.backupLocation = backupLocation;
    }

    public String getBackupLocation() {
        return backupLocation;
    }
}
