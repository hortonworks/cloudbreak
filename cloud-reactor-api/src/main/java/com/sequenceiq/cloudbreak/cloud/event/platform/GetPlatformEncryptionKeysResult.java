package com.sequenceiq.cloudbreak.cloud.event.platform;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;

public class GetPlatformEncryptionKeysResult extends CloudPlatformResult {
    private CloudEncryptionKeys cloudEncryptionKeys;

    public GetPlatformEncryptionKeysResult(Long resourceId, CloudEncryptionKeys cloudEncryptionKeys) {
        super(resourceId);
        this.cloudEncryptionKeys = cloudEncryptionKeys;
    }

    public GetPlatformEncryptionKeysResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public CloudEncryptionKeys getCloudEncryptionKeys() {
        return cloudEncryptionKeys;
    }
}
