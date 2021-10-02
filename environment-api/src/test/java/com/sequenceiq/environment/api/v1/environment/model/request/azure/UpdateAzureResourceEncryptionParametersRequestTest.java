package com.sequenceiq.environment.api.v1.environment.model.request.azure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UpdateAzureResourceEncryptionParametersRequestTest {

    private static final String ENCRYPTION_KEY_URL = "encryptionKeyUrl";

    private static final String ENCRYPTION_KEY_RESOURCE_GROUP_NAME = "encryptionKeyResourceGroupName";

    @Test
    void builderTest() {
        UpdateAzureResourceEncryptionParametersRequest underTest = new UpdateAzureResourceEncryptionParametersRequest();
        underTest.setAzureResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                .withEncryptionKeyUrl(ENCRYPTION_KEY_URL)
                .withEncryptionKeyResourceGroupName(ENCRYPTION_KEY_RESOURCE_GROUP_NAME)
                .build()
        );

        assertThat(underTest.getAzureResourceEncryptionParameters().getEncryptionKeyUrl()).isEqualTo(ENCRYPTION_KEY_URL);
        assertThat(underTest.getAzureResourceEncryptionParameters().getEncryptionKeyResourceGroupName()).isEqualTo(ENCRYPTION_KEY_RESOURCE_GROUP_NAME);
    }
}