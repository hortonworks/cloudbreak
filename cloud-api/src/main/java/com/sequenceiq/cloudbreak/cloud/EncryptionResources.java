package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;

public interface EncryptionResources extends CloudPlatformAware {

    default CreatedDiskEncryptionSet createDiskEncryptionSet(DiskEncryptionSetCreationRequest diskEncryptionSetCreationRequest) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void deleteDiskEncryptionSet(DiskEncryptionSetDeletionRequest diskEncryptionSetDeletionRequest) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default CloudEncryptionKey createEncryptionKey(EncryptionKeyCreationRequest encryptionKeyCreationRequest) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

}