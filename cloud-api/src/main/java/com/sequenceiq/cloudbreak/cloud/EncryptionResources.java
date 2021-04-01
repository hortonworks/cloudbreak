package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedEncryptionResources;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionResourcesCreationRequest;

public interface EncryptionResources extends CloudPlatformAware {
    CreatedEncryptionResources createDiskEncryptionSet(EncryptionResourcesCreationRequest encryptionResourcesCreationRequest);
}