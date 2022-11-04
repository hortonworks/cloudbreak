package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class ValidateRdsUpgradeOnCloudProviderResult extends AbstractValidateRdsUpgradeEvent {
    private final TargetMajorVersion version;

    private final String reason;

    @JsonCreator
    public ValidateRdsUpgradeOnCloudProviderResult(
            @JsonProperty("resourceId") Long stackId, @JsonProperty("version") TargetMajorVersion version, @JsonProperty("reason") String reason) {
        super(stackId);
        this.version = version;
        this.reason = reason;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return "ValidateRdsUpgradeOnCloudProviderResult{" +
                "version=" + version +
                ", reason='" + reason + '\'' +
                "} " + super.toString();
    }
}
