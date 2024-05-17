package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.UpdateEncryptionKeyResourceAccessRequest;

public interface EncryptionResources extends CloudPlatformAware {

    default CreatedDiskEncryptionSet createDiskEncryptionSet(DiskEncryptionSetCreationRequest request) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void deleteDiskEncryptionSet(DiskEncryptionSetDeletionRequest request) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default CloudEncryptionKey createEncryptionKey(EncryptionKeyCreationRequest request) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default void updateEncryptionKeyResourceAccess(UpdateEncryptionKeyResourceAccessRequest request) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

}