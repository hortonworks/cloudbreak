package com.sequenceiq.environment.api.v1.environment.model.request.aws;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class UpdateAwsDiskEncryptionParametersRequestTest {
    private static final String ENCRYPTION_KEY_ARN = "encryptionKeyArn";

    @Test
    void builderTest() {
        AwsDiskEncryptionParameters awsDiskEncryptionParameters = AwsDiskEncryptionParameters.builder().
                withEncryptionKeyArn(ENCRYPTION_KEY_ARN).build();
        UpdateAwsDiskEncryptionParametersRequest underTest = UpdateAwsDiskEncryptionParametersRequest.builder().
                withAwsDiskEncryptionParameters(awsDiskEncryptionParameters).build();
        assertThat(underTest.getAwsDiskEncryptionParameters()).isEqualTo(awsDiskEncryptionParameters);
        assertThat(underTest.getAwsDiskEncryptionParameters().getEncryptionKeyArn()).isEqualTo(ENCRYPTION_KEY_ARN);
    }
}
