package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AzureInstanceTemplateV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

class InstanceTemplateParameterConverterTest {

    private static final String PRIVATE_ID = "privateId";

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    private InstanceTemplateParameterConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new InstanceTemplateParameterConverter();
    }

    @Test
    void convertTestAzureInstanceTemplateV1ParametersToAzureInstanceTemplateV4ParametersWhenBasicFields() {
        AzureInstanceTemplateV1Parameters source = new AzureInstanceTemplateV1Parameters();
        source.setPrivateId(PRIVATE_ID);
        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForAzureEncryption(false, false, null);

        AzureInstanceTemplateV4Parameters azureInstanceTemplateV4Parameters = underTest.convert(source, environment);

        assertThat(azureInstanceTemplateV4Parameters).isNotNull();
        assertThat(azureInstanceTemplateV4Parameters.getEncrypted()).isEqualTo(Boolean.FALSE);
        assertThat(azureInstanceTemplateV4Parameters.getManagedDisk()).isEqualTo(Boolean.TRUE);
        assertThat(azureInstanceTemplateV4Parameters.getPrivateId()).isEqualTo(PRIVATE_ID);
    }

    static Object[][] convertTestAzureInstanceTemplateV1ParametersToAzureInstanceTemplateV4ParametersWhenEncryptionDataProvider() {
        return new Object[][]{
                // testCaseName withAzure withResourceEncryption diskEncryptionSetId expectedEncryption expectedDiskEncryptionSetId
                {"withAzure=false", false, false, null, false, null},
                {"withAzure=true, withResourceEncryption=false", true, false, null, false, null},
                {"withAzure=true, withResourceEncryption=true, diskEncryptionSetId=null", true, true, null, false, null},
                {"withAzure=true, withResourceEncryption=true, diskEncryptionSetId=DISK_ENCRYPTION_SET_ID", true, true, DISK_ENCRYPTION_SET_ID, true,
                        DISK_ENCRYPTION_SET_ID},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("convertTestAzureInstanceTemplateV1ParametersToAzureInstanceTemplateV4ParametersWhenEncryptionDataProvider")
    void convertTestAzureInstanceTemplateV1ParametersToAzureInstanceTemplateV4ParametersWhenEncryption(String testCaseName, boolean withAzure,
            boolean withResourceEncryption, String diskEncryptionSetId, boolean expectedEncryption, String expectedDiskEncryptionSetId) {
        AzureInstanceTemplateV1Parameters source = new AzureInstanceTemplateV1Parameters();
        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForAzureEncryption(withAzure, withResourceEncryption, diskEncryptionSetId);

        AzureInstanceTemplateV4Parameters azureInstanceTemplateV4Parameters = underTest.convert(source, environment);

        assertThat(azureInstanceTemplateV4Parameters).isNotNull();

        AzureEncryptionV4Parameters encryption = azureInstanceTemplateV4Parameters.getEncryption();
        if (expectedEncryption) {
            assertThat(encryption).isNotNull();
            assertThat(encryption.getType()).isEqualTo(EncryptionType.CUSTOM);
            assertThat(encryption.getDiskEncryptionSetId()).isEqualTo(expectedDiskEncryptionSetId);
        } else {
            assertThat(encryption).isNull();
        }
    }

    private DetailedEnvironmentResponse createDetailedEnvironmentResponseForAzureEncryption(boolean withAzure, boolean withResourceEncryption,
            String diskEncryptionSetId) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        if (withAzure) {
            AzureEnvironmentParameters parameters = new AzureEnvironmentParameters();
            environment.setAzure(parameters);
            if (withResourceEncryption) {
                AzureResourceEncryptionParameters encryption = new AzureResourceEncryptionParameters();
                parameters.setResourceEncryptionParameters(encryption);
                encryption.setDiskEncryptionSetId(diskEncryptionSetId);
            }
        }
        return  environment;
    }

}