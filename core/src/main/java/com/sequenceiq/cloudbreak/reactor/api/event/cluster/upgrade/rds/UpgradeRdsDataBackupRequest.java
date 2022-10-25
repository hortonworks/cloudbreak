package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class UpgradeRdsDataBackupRequest extends AbstractUpgradeRdsEvent {

    private final String backupLocation;

    private final String backupInstanceProfile;

    @JsonCreator
    public UpgradeRdsDataBackupRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("version") TargetMajorVersion version,
            @JsonProperty("backupLocation") String backupLocation,
            @JsonProperty("backupInstanceProfile") String backupInstanceProfile
    ) {
        super(stackId, version);
        this.backupLocation = backupLocation;
        this.backupInstanceProfile = backupInstanceProfile;

    }

    public String getBackupLocation() {
        return backupLocation;
    }

    public String getBackupInstanceProfile() {
        return backupInstanceProfile;
    }

    @Override
    public String toString() {
        return "UpgradeRdsDataBackupRequest{" +
                "backupLocation='" + backupLocation + '\'' +
                ", backupInstanceProfile='" + backupInstanceProfile + '\'' +
                "} " + super.toString();
    }
}
