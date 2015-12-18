package com.sequenceiq.cloudbreak.cloud.event.instance;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;

public class GetSSHFingerprintsResult extends CloudPlatformResult<CloudPlatformRequest> {

    private Set<String> sshFingerprints;

    public GetSSHFingerprintsResult(CloudPlatformRequest<?> request, Set<String> sshFingerprints) {
        super(request);
        this.sshFingerprints = sshFingerprints;
    }

    public GetSSHFingerprintsResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public Set<String> getSshFingerprints() {
        return sshFingerprints;
    }
}
