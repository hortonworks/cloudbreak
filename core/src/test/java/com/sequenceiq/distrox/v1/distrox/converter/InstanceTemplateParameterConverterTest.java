package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.KeyEncryptionMethod;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AzureInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.GcpInstanceTemplateV4Parameters;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsEncryptionV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AwsInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.AzureInstanceTemplateV1Parameters;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.GcpInstanceTemplateV1Parameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

class InstanceTemplateParameterConverterTest {

    private static final String PRIVATE_ID = "privateId";

    private static final String DISK_ENCRYPTION_SET_ID = "diskEncryptionSetId";

    private static final String ENCRYPTION_KEY = "encryptionKey";

    private static final String ENCRYPTION_KEY_URL = "encryptionKeyUrl";

    private static final String DATAHUB_ENCRYPTION_KEY = "awsDatahubEncryptionKey";

    private  static final String ENVIRONMENT_ENCRYPTION_KEY = "awsEnvironmentEncryptionKey";

    private InstanceTemplateParameterConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new InstanceTemplateParameterConverter();
    }

    @Test
    void convertTestAzureInstanceTemplateV1ParametersToAzureInstanceTemplateV4ParametersWhenBasicFields() {
        AzureInstanceTemplateV1Parameters source = new AzureInstanceTemplateV1Parameters();
        source.setPrivateId(PRIVATE_ID);
        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForAzureEncryption(false, false, null, null);

        AzureInstanceTemplateV4Parameters azureInstanceTemplateV4Parameters = underTest.convert(source, environment);

        assertThat(azureInstanceTemplateV4Parameters).isNotNull();
        assertThat(azureInstanceTemplateV4Parameters.getEncrypted()).isEqualTo(Boolean.FALSE);
        assertThat(azureInstanceTemplateV4Parameters.getManagedDisk()).isEqualTo(Boolean.TRUE);
        assertThat(azureInstanceTemplateV4Parameters.getPrivateId()).isEqualTo(PRIVATE_ID);
    }

    static Object[][] convertTestAzureInstanceTemplateV1ParametersToAzureInstanceTemplateV4ParametersWhenEncryptionDataProvider() {
        return new Object[][]{
                // testCaseName withAzure withResourceEncryption diskEncryptionSetId encryptionKeyUrl expectedEncryption expectedDiskEncryptionSetId
                // expectedEncryptionKeyUrl
                {"withAzure=false", false, false, null, null, false, null, null},
                {"withAzure=true, withResourceEncryption=false", true, false, null, null, false, null, null},
                {"withAzure=true, withResourceEncryption=true, diskEncryptionSetId=null", true, true, null, null, false, null, null},
                {"withAzure=true, withResourceEncryption=true, diskEncryptionSetId=DISK_ENCRYPTION_SET_ID, encryptionKeyUrl=ENCRYPTION_KEY_URL", true, true,
                        DISK_ENCRYPTION_SET_ID, ENCRYPTION_KEY_URL, true, DISK_ENCRYPTION_SET_ID, ENCRYPTION_KEY_URL},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("convertTestAzureInstanceTemplateV1ParametersToAzureInstanceTemplateV4ParametersWhenEncryptionDataProvider")
    void convertTestAzureInstanceTemplateV1ParametersToAzureInstanceTemplateV4ParametersWhenEncryption(String testCaseName, boolean withAzure,
            boolean withResourceEncryption, String diskEncryptionSetId, String encryptionKeyUrl, boolean expectedEncryption,
            String expectedDiskEncryptionSetId, String expectedEncryptionKeyUrl) {
        AzureInstanceTemplateV1Parameters source = new AzureInstanceTemplateV1Parameters();
        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForAzureEncryption(withAzure, withResourceEncryption,
                diskEncryptionSetId, encryptionKeyUrl);

        AzureInstanceTemplateV4Parameters azureInstanceTemplateV4Parameters = underTest.convert(source, environment);

        assertThat(azureInstanceTemplateV4Parameters).isNotNull();

        AzureEncryptionV4Parameters encryption = azureInstanceTemplateV4Parameters.getEncryption();
        if (expectedEncryption) {
            assertThat(encryption).isNotNull();
            assertThat(encryption.getType()).isEqualTo(EncryptionType.CUSTOM);
            assertThat(encryption.getDiskEncryptionSetId()).isEqualTo(expectedDiskEncryptionSetId);
            assertThat(encryption.getKey()).isEqualTo(expectedEncryptionKeyUrl);
        } else {
            assertThat(encryption).isNull();
        }
    }

    private DetailedEnvironmentResponse createDetailedEnvironmentResponseForAzureEncryption(boolean withAzure, boolean withResourceEncryption,
            String diskEncryptionSetId, String encryptionKeyUrl) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        if (withAzure) {
            AzureEnvironmentParameters parameters = new AzureEnvironmentParameters();
            environment.setAzure(parameters);
            if (withResourceEncryption) {
                AzureResourceEncryptionParameters encryption = new AzureResourceEncryptionParameters();
                parameters.setResourceEncryptionParameters(encryption);
                encryption.setEncryptionKeyUrl(encryptionKeyUrl);
                encryption.setDiskEncryptionSetId(diskEncryptionSetId);
            }
        }
        return  environment;
    }

    @Test
    void convertTestGcpInstanceTemplateV1ParametersToGcpInstanceTemplateV4ParametersWhenBasicFields() {
        GcpInstanceTemplateV1Parameters source = new GcpInstanceTemplateV1Parameters();

        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForGcpEncryption(false, false, null);

        GcpInstanceTemplateV4Parameters gcpInstanceTemplateV4Parameters = underTest.convert(source, environment);

        assertThat(gcpInstanceTemplateV4Parameters).isNotNull();
    }

    static Object[][] convertTestGcpInstanceTemplateV1ParametersToGcpInstanceTemplateV4ParametersWhenEncryptionDataProvided() {
        return new Object[][]{
                // testCaseName withGcp withResourceEncryption encryptionKey expectedEncryption expectedEncryptionKey
                {"withGcp=false", false, false, null, false, null},
                {"withGcp=true, withResourceEncryption=false", true, false, null, false, null},
                {"withGcp=true, withResourceEncryption=true, encryptionKey=null", true, true, null, false, null},
                {"withGcp=true, withResourceEncryption=true, encryptionKey=ENCRYPTION_KEY", true, true, ENCRYPTION_KEY, true,
                        ENCRYPTION_KEY},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("convertTestGcpInstanceTemplateV1ParametersToGcpInstanceTemplateV4ParametersWhenEncryptionDataProvided")
    void convertTestGcpInstanceTemplateV1ParametersToGcpInstanceTemplateV4ParametersWhenEncryption(String testCaseName, boolean withGcp,
            boolean withResourceEncryption, String encryptionKey, boolean expectedEncryption, String expectedEncryptionKey) {
        GcpInstanceTemplateV1Parameters source = new GcpInstanceTemplateV1Parameters();
        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForGcpEncryption(withGcp, withResourceEncryption, encryptionKey);

        GcpInstanceTemplateV4Parameters gcpInstanceTemplateV4Parameters = underTest.convert(source, environment);

        assertThat(gcpInstanceTemplateV4Parameters).isNotNull();

        GcpEncryptionV4Parameters encryption = gcpInstanceTemplateV4Parameters.getEncryption();
        if (expectedEncryption) {
            assertThat(encryption).isNotNull();
            assertThat(encryption.getType()).isEqualTo(EncryptionType.CUSTOM);
            assertThat(encryption.getKeyEncryptionMethod()).isEqualTo(KeyEncryptionMethod.KMS);
            assertThat(encryption.getKey()).isEqualTo(expectedEncryptionKey);
            assertEquals(gcpInstanceTemplateV4Parameters.getEncrypted(), Boolean.TRUE);
        } else {
            assertThat(encryption).isNull();
            assertEquals(gcpInstanceTemplateV4Parameters.getEncrypted(), Boolean.FALSE);
        }
    }

    private DetailedEnvironmentResponse createDetailedEnvironmentResponseForGcpEncryption(boolean withGcp, boolean withResourceEncryption,
            String encryptionKey) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        if (withGcp) {
            GcpEnvironmentParameters parameters = new GcpEnvironmentParameters();
            environment.setGcp(parameters);
            if (withResourceEncryption) {
                GcpResourceEncryptionParameters encryption = new GcpResourceEncryptionParameters();
                parameters.setGcpResourceEncryptionParameters(encryption);
                encryption.setEncryptionKey(encryptionKey);
            }
        }
        return  environment;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("convertTestAwsInstanceTemplateV1ParametersToAwsInstanceTemplateV4ParametersWhenEncryptionDataProvided")
    void convertTestAwsInstanceTemplateV1ParametersToAwsInstanceTemplateV4ParametersWhenEncryption(String testCaseName, boolean withAws,
            boolean withResourceEncryption, String dataHubEncryptionKey, String environmentEncryptionKey, String expectedEncryptionKey) {
        AwsInstanceTemplateV1Parameters source = new AwsInstanceTemplateV1Parameters();
        String finalEncryptionKey = null;
        if (dataHubEncryptionKey != null) {
            source.setEncryption(encryption(EncryptionType.CUSTOM, DATAHUB_ENCRYPTION_KEY));
            finalEncryptionKey = DATAHUB_ENCRYPTION_KEY;
        } else {
            if (environmentEncryptionKey != null) {
                source.setEncryption(encryption(EncryptionType.CUSTOM, ENVIRONMENT_ENCRYPTION_KEY));
                finalEncryptionKey = ENVIRONMENT_ENCRYPTION_KEY;
            } else  {
                source.setEncryption(encryption(null, null));
            }
        }
        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForAwsEncryption(withAws, withResourceEncryption, finalEncryptionKey);
        AwsInstanceTemplateV4Parameters awsInstanceTemplateV4Parameters = underTest.convert(source, environment);

        assertThat(awsInstanceTemplateV4Parameters).isNotNull();

        AwsEncryptionV4Parameters encryption = awsInstanceTemplateV4Parameters.getEncryption();
        assertThat(encryption).isNotNull();
        if (dataHubEncryptionKey != null || environmentEncryptionKey != null) {
            assertThat(encryption.getType()).isEqualTo(EncryptionType.CUSTOM);
            assertThat(encryption.getKey()).isEqualTo(expectedEncryptionKey);
        } else {
            assertThat(encryption.getType()).isEqualTo(EncryptionType.DEFAULT);
        }

    }

    @Test
    void testWhenDatahubEncryptionTypeSetToNull() {
        AwsInstanceTemplateV1Parameters source = new AwsInstanceTemplateV1Parameters();
        source.setEncryption(encryption(null, null));
        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForAwsEncryption(true, false, null);
        AwsInstanceTemplateV4Parameters awsInstanceTemplateV4Parameters = underTest.convert(source, environment);
        assertThat(awsInstanceTemplateV4Parameters).isNotNull();
        AwsEncryptionV4Parameters encryption = awsInstanceTemplateV4Parameters.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getType()).isEqualTo(EncryptionType.DEFAULT);
    }

    @Test
    void convertTestAwsInstanceTemplateV1ParametersToAwsInstanceTemplateV4ParametersWhenBasicFields() {
        AwsInstanceTemplateV1Parameters source = new AwsInstanceTemplateV1Parameters();

        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForAwsEncryption(false, false, null);

        AwsInstanceTemplateV4Parameters awsInstanceTemplateV4Parameters = underTest.convert(source, environment);

        assertThat(awsInstanceTemplateV4Parameters).isNotNull();
    }

    @Test
    void testConvertAwsInstanceTemplateV1ToAwsInstanceTemplateV4WhenV1IsNull() {
        // GIVEN: Source for DH encryption does not have encryption params set, i.e, is null
        AwsInstanceTemplateV1Parameters awsParametersEffective = new AwsInstanceTemplateV1Parameters();

        DetailedEnvironmentResponse environment = createDetailedEnvironmentResponseForAwsEncryption(true, true,
                ENVIRONMENT_ENCRYPTION_KEY);

        // WHEN: Converting to V4
        AwsInstanceTemplateV4Parameters result = underTest.convert(awsParametersEffective, environment);

        // THEN: Encrpytion with ENVIRONMENT_ENCRYPTION_KEY should take place
        assertThat(result).isNotNull();
        assertThat(result.getEncryption().getType()).isEqualTo(EncryptionType.CUSTOM);
        assertThat(result.getEncryption().getKey()).isEqualTo(ENVIRONMENT_ENCRYPTION_KEY);
    }

    private DetailedEnvironmentResponse createDetailedEnvironmentResponseForAwsEncryption(boolean withAws, boolean withResourceEncryption,
            String environmentEncryptionKey) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        if (withAws) {
            AwsEnvironmentParameters parameters = new AwsEnvironmentParameters();
            environment.setAws(parameters);
            if (withResourceEncryption && ENVIRONMENT_ENCRYPTION_KEY != null) {
                AwsDiskEncryptionParameters encryption = new AwsDiskEncryptionParameters();
                parameters.setAwsDiskEncryptionParameters(encryption);
                encryption.setEncryptionKeyArn(environmentEncryptionKey);
            }
        }
        return environment;
    }

    static Object[][] convertTestAwsInstanceTemplateV1ParametersToAwsInstanceTemplateV4ParametersWhenEncryptionDataProvided() {
        return new Object[][]{
                // testCaseName withAws withResourceEncryption dataHubEncryptionKey environmentEncryptionKey,
                // expectedEncryptionKey
                {"withAws=false", false, false, null, null, null},
                {"withAws=true, withResourceEncryption=false", true, false, null, null,  null, null},
                {"withAws=true, withResourceEncryption=true, encryptionKey=null", true, true, null, null, null, null},
                {"withAws=true, withResourceEncryption=true, encryptionKey=DATAHUB_ENCRYPTION_KEY",
                        true, true, DATAHUB_ENCRYPTION_KEY, null, DATAHUB_ENCRYPTION_KEY},
                {"withAws=true, withResourceEncryption=true, encryptionKey=DATAHUB_ENCRYPTION_KEY&&ENVIRONMENT_ENCRYPTION_KEY",
                        true, true, DATAHUB_ENCRYPTION_KEY, ENVIRONMENT_ENCRYPTION_KEY, DATAHUB_ENCRYPTION_KEY},
                {"withAws=true, withResourceEncryption=true, encryptionKey=ENVIRONMENT_ENCRYPTION_KEY",
                        true, true, null, ENVIRONMENT_ENCRYPTION_KEY, ENVIRONMENT_ENCRYPTION_KEY},
                {"withAws=true, withResourceEncryption=true, encryptionKey=DEFAULT_ENCRYPTION_KEY",
                        true, true, null, null, null}

        };
    }

    private AwsEncryptionV1Parameters encryption(EncryptionType type, String key) {
        AwsEncryptionV1Parameters encryption = new AwsEncryptionV1Parameters();
        encryption.setType(type);
        encryption.setKey(key);
        return encryption;
    }

}