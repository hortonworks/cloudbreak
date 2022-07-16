package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class GetSSHFingerprintsResult extends CloudPlatformResult implements FlowPayload {

    private final Set<String> sshFingerprints;

    public GetSSHFingerprintsResult(Long resourceId, Set<String> sshFingerprints) {
        super(resourceId);
        this.sshFingerprints = sshFingerprints;
    }

    @JsonCreator
    public GetSSHFingerprintsResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("resourceId") Long resourceId) {
        super(statusReason, errorDetails, resourceId);
        this.sshFingerprints = null;
    }

    public Set<String> getSshFingerprints() {
        return sshFingerprints;
    }
}
