package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class GetSSHFingerprintsResult extends CloudPlatformResult {

    private Set<String> sshFingerprints;

    public GetSSHFingerprintsResult(Long resourceId, Set<String> sshFingerprints) {
        super(resourceId);
        this.sshFingerprints = sshFingerprints;
    }

    public GetSSHFingerprintsResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public Set<String> getSshFingerprints() {
        return sshFingerprints;
    }
}
