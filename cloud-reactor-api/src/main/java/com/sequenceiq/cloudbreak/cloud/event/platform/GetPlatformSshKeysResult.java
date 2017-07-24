package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudSshKeys;

public class GetPlatformSshKeysResult extends CloudPlatformResult<CloudPlatformRequest> {
    private CloudSshKeys cloudSshKeys;

    public GetPlatformSshKeysResult(CloudPlatformRequest<?> request, CloudSshKeys cloudSshKeys) {
        super(request);
        this.cloudSshKeys = cloudSshKeys;
    }

    public GetPlatformSshKeysResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudSshKeys getCloudSshKeys() {
        return cloudSshKeys;
    }
}
