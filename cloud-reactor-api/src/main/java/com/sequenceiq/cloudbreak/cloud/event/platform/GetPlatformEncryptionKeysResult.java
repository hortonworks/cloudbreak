package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;

public class GetPlatformEncryptionKeysResult extends CloudPlatformResult<CloudPlatformRequest<?>> {
    private CloudEncryptionKeys cloudEncryptionKeys;

    public GetPlatformEncryptionKeysResult(CloudPlatformRequest<?> request, CloudEncryptionKeys cloudEncryptionKeys) {
        super(request);
        this.cloudEncryptionKeys = cloudEncryptionKeys;
    }

    public GetPlatformEncryptionKeysResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public CloudEncryptionKeys getCloudEncryptionKeys() {
        return cloudEncryptionKeys;
    }
}
