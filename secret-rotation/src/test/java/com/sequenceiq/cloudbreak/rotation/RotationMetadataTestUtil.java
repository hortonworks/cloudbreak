package com.sequenceiq.cloudbreak.rotation;

import com.sequenceiq.cloudbreak.rotation.service.RotationMetadata;

public class RotationMetadataTestUtil {

    private RotationMetadataTestUtil() {

    }

    public static RotationMetadata metadataForRotation(String crn, SecretType secretType) {
        return new RotationMetadata(secretType, RotationFlowExecutionType.ROTATE, null, crn, null);
    }

    public static RotationMetadata metadataForRollback(String crn, SecretType secretType) {
        return new RotationMetadata(secretType, RotationFlowExecutionType.ROLLBACK, null, crn, null);
    }

    public static RotationMetadata metadataForFinalize(String crn, SecretType secretType) {
        return new RotationMetadata(secretType, RotationFlowExecutionType.FINALIZE, null, crn, null);
    }

    public static RotationMetadata metadataForPreValidate(String crn, SecretType secretType) {
        return new RotationMetadata(secretType, RotationFlowExecutionType.PREVALIDATE, null, crn, null);
    }
}
