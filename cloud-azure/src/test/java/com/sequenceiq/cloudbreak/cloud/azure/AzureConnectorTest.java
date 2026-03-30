package com.sequenceiq.cloudbreak.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CommonSecretEncryptionValidator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureFileSystemValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureStorageValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureSubnetValidator;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureTagValidator;

@ExtendWith(MockitoExtension.class)
class AzureConnectorTest {

    @Mock
    private AzureTagValidator azureTagValidator;

    @Mock
    private AzureSubnetValidator azureSubnetValidator;

    @Mock
    private AzureStorageValidator azureStorageValidator;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private AzureFileSystemValidator azureFileSystemValidator;

    @Mock
    private CommonSecretEncryptionValidator commonSecretEncryptionValidator;

    @InjectMocks
    private AzureConnector underTest;

    @Test
    void testValidatorsImage() {
        assertThat(underTest.validators(ValidatorType.IMAGE)).containsExactly(azureImageFormatValidator);
    }

    @Test
    void testValidatorsAll() {
        assertThat(underTest.validators(ValidatorType.ALL)).containsExactly(azureTagValidator, azureSubnetValidator, azureStorageValidator,
                azureImageFormatValidator, azureFileSystemValidator, commonSecretEncryptionValidator);
    }
}
