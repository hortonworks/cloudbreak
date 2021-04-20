package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;

public interface EncryptionResources extends CloudPlatformAware {
    CreatedDiskEncryptionSet createDiskEncryptionSet(DiskEncryptionSetCreationRequest diskEncryptionSetCreationRequest);
}