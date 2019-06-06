package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;

public class GetPlatformSshKeysResult extends CloudPlatformResult {
    private CloudSshKeys cloudSshKeys;

    public GetPlatformSshKeysResult(Long resourceId, CloudSshKeys cloudSshKeys) {
        super(resourceId);
        this.cloudSshKeys = cloudSshKeys;
    }

    public GetPlatformSshKeysResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudSshKeys getCloudSshKeys() {
        return cloudSshKeys;
    }
}
