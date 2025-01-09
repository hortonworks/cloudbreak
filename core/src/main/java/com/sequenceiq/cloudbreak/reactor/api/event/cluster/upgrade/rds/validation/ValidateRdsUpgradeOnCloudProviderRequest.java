package com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.rds.validation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;

public class ValidateRdsUpgradeOnCloudProviderRequest extends AbstractValidateRdsUpgradeEvent {

    private final TargetMajorVersion version;

    @JsonCreator
    public ValidateRdsUpgradeOnCloudProviderRequest(
            @JsonProperty("resourceId") Long stackId, @JsonProperty("version") TargetMajorVersion version) {
        super(stackId);
        this.version = version;
    }

    public TargetMajorVersion getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "ValidateRdsUpgradeOnCloudProviderRequest{" +
                "version=" + version +
                "} " + super.toString();
    }
}