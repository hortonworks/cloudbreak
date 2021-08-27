package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.encryption.AwsDiskEncryptionParameterCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreateAwsDiskEncryptionParameters;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;

public interface AwsEncryptionParameters extends CloudPlatformAware {
        CreateAwsDiskEncryptionParameters createAwsDiskEncryptionParameters(AwsDiskEncryptionParameterCreationRequest awsDiskEncryptionParameterCreationRequest);

        void deleteDiskEncryptionSet(DiskEncryptionSetDeletionRequest diskEncryptionSetDeletionRequest);
}

