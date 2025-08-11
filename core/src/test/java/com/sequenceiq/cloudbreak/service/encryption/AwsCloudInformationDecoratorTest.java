package com.sequenceiq.cloudbreak.service.encryption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.ArnService;
import com.sequenceiq.cloudbreak.cloud.model.encryption.EncryptionKeyType;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CloudIdentityTypeDecider;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudIdentity;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.cloudstorage.S3Identity;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.response.LoggingResponse;
import com.sequenceiq.common.api.telemetry.response.TelemetryResponse;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.RoleBasedParameters;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AwsCloudInformationDecoratorTest {

    private static final String CROSS_ACCOUNT_ROLE = "cross-account-role";

    private static final String LOGGER_INSTANCE_PROFILE = "logger-instance-profile";

    private static final String ID_BROKER_ASSUMER = "id-broker-assumer";

    @Mock
    private DetailedEnvironmentResponse environment;

    @Mock
    private Stack stack;

    @Mock
    private ArnService arnService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @Mock
    private CloudIdentityTypeDecider cloudIdentityTypeDecider;

    @InjectMocks
    private AwsCloudInformationDecorator underTest;

    @BeforeEach
    public void setUp() {
        setUpEnvironmentTelemetry(LOGGER_INSTANCE_PROFILE);
        setUpEnvironmentCredential(CROSS_ACCOUNT_ROLE);
        setUpStack(LOGGER_INSTANCE_PROFILE, ID_BROKER_ASSUMER);
        when(cmTemplateProcessorFactory.get(anyString())).thenReturn(cmTemplateProcessor);
    }

    @Test
    void testGetLuksEncryptionKeyCryptographicPrincipalsForDataLake() {
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        List<String> result = underTest.getLuksEncryptionKeyCryptographicPrincipals(environment, stack);
        assertThat(result).hasSameElementsAs(List.of(LOGGER_INSTANCE_PROFILE, ID_BROKER_ASSUMER));
    }

    @Test
    void testGetLuksEncryptionKeyCryptographicPrincipalsForDataHub() {
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        List<String> result = underTest.getLuksEncryptionKeyCryptographicPrincipals(environment, stack);
        assertThat(result).hasSameElementsAs(List.of(LOGGER_INSTANCE_PROFILE));
    }

    @Test
    void testGetLuksEncryptionKeyCryptographicPrincipalsUnsupportedStackType() {
        when(stack.getType()).thenReturn(StackType.TEMPLATE);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getLuksEncryptionKeyCryptographicPrincipals(environment, stack));
    }

    @Test
    void testGetLuksEncryptionKeyCryptographicPrincipalsWhenNotFoundForDataLake() {
        setUpStack(null, null);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getLuksEncryptionKeyCryptographicPrincipals(environment, stack));
    }

    @Test
    void testGetLuksEncryptionKeyCryptographicPrincipalsWhenNotFoundForDataHub() {
        setUpEnvironmentTelemetry(null);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getLuksEncryptionKeyCryptographicPrincipals(environment, stack));
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyCryptographicPrincipalsForDataLake() {
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        List<String> result = underTest.getCloudSecretManagerEncryptionKeyCryptographicPrincipals(environment, stack);
        assertThat(result).hasSameElementsAs(List.of(CROSS_ACCOUNT_ROLE, LOGGER_INSTANCE_PROFILE, ID_BROKER_ASSUMER));
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyCryptographicPrincipalsForDataHub() {
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        List<String> result = underTest.getCloudSecretManagerEncryptionKeyCryptographicPrincipals(environment, stack);
        assertThat(result).hasSameElementsAs(List.of(CROSS_ACCOUNT_ROLE, LOGGER_INSTANCE_PROFILE));
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyCryptographicPrincipalsWhenUnsupportedStackType() {
        when(stack.getType()).thenReturn(StackType.TEMPLATE);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getCloudSecretManagerEncryptionKeyCryptographicPrincipals(environment, stack));
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyCryptographicPrincipalsWhenCrossAccountRoleNotFound() {
        setUpEnvironmentCredential(null);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getCloudSecretManagerEncryptionKeyCryptographicPrincipals(environment, stack));
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyCryptographicPrincipalssWhenNotFoundForDataLake() {
        setUpStack(null, null);
        when(stack.getType()).thenReturn(StackType.DATALAKE);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getCloudSecretManagerEncryptionKeyCryptographicPrincipals(environment, stack));
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyCryptographicPrincipalssWhenNotFoundForDataHub() {
        setUpEnvironmentTelemetry(null);
        when(stack.getType()).thenReturn(StackType.WORKLOAD);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getCloudSecretManagerEncryptionKeyCryptographicPrincipals(environment, stack));
    }

    @Test
    void testGetUserdataSecretEncryptionKeyType() {
        assertEquals(EncryptionKeyType.AWS_KMS_KEY_ARN, underTest.getUserdataSecretEncryptionKeyType());
    }

    @Test
    void testGetLuksEncryptionKeyResourceType() {
        assertEquals(ResourceType.AWS_KMS_KEY, underTest.getLuksEncryptionKeyResourceType());
    }

    @Test
    void testGetCloudSecretManagerEncryptionKeyResourceType() {
        assertEquals(ResourceType.AWS_KMS_KEY, underTest.getCloudSecretManagerEncryptionKeyResourceType());
    }

    @Test
    void testGetAuthorizedClientForLuksEncryptionKey() {
        setUpStack("arn:aws:iam::123456789012:instance-profile/example-instance-profile", null);
        when(stack.getRegion()).thenReturn("region");
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        when(instanceMetaData.getInstanceId()).thenReturn("instance-id");
        when(arnService.buildEc2InstanceArn("aws", "region", "123456789012", "instance-id")).thenReturn("ec2-instance-arn");
        String result = underTest.getAuthorizedClientForLuksEncryptionKey(stack, instanceMetaData);
        assertEquals(result, "ec2-instance-arn");
    }

    @Test
    void testGetAuthorizedClientForLuksEncryptionKeyWhenAccountIdNotPresent() {
        setUpStack("arn:aws:iam:::instance-profile/example-instance-profile", null);
        InstanceMetaData instanceMetaData = mock(InstanceMetaData.class);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getAuthorizedClientForLuksEncryptionKey(stack, instanceMetaData));
    }

    @Test
    void testGetArnPartition() {
        assertEquals("aws", underTest.getArnPartition());
    }

    @Test
    void testGetUserdataSecretCryptographicPrincipalsForInstanceGroups() {
        Map<String, Set<String>> componentsByHostGroup = Map.of(
                "master", Set.of(CROSS_ACCOUNT_ROLE, LOGGER_INSTANCE_PROFILE),
                "idbroker", Set.of(CROSS_ACCOUNT_ROLE, ID_BROKER_ASSUMER)
        );
        when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(componentsByHostGroup);
        when(cloudIdentityTypeDecider.getIdentityTypeForInstanceGroup("master", componentsByHostGroup)).thenReturn(CloudIdentityType.LOG);
        when(cloudIdentityTypeDecider.getIdentityTypeForInstanceGroup("idbroker", componentsByHostGroup)).thenReturn(CloudIdentityType.ID_BROKER);
        Map<String, List<String>> result = underTest.getUserdataSecretCryptographicPrincipalsForInstanceGroups(environment, stack);
        assertThat(result.get("master")).hasSameElementsAs(List.of(CROSS_ACCOUNT_ROLE, LOGGER_INSTANCE_PROFILE));
        assertThat(result.get("idbroker")).hasSameElementsAs(List.of(CROSS_ACCOUNT_ROLE, ID_BROKER_ASSUMER));
    }

    @Test
    void testGetUserdataSecretCryptographicPrincipalsForInstanceGroupsWhenCrossAccountRoleNotFound() {
        setUpEnvironmentCredential(null);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getUserdataSecretCryptographicPrincipalsForInstanceGroups(environment, stack));
    }

    @Test
    void testGetUserdataSecretCryptographicPrincipalsForInstanceGroupsWhenInstanceProfileNotFound() {
        when(cmTemplateProcessor.getComponentsByHostGroup()).thenReturn(Map.of());
        assertThrows(CloudbreakServiceException.class, () -> underTest.getUserdataSecretCryptographicPrincipalsForInstanceGroups(environment, stack));
    }

    @Test
    void testGetUserdataSecretCryptographicAuthorizedClients() {
        setUpStack("arn:aws:iam::123456789012:instance-profile/example-instance-profile", null);
        when(stack.getRegion()).thenReturn("region");
        when(arnService.buildEc2InstanceArn("aws", "region", "123456789012", "instance-id")).thenReturn("ec2-instance-arn");
        List<String> result = underTest.getUserdataSecretCryptographicAuthorizedClients(stack, "instance-id");
        assertThat(result).hasSameElementsAs(List.of("ec2-instance-arn"));
    }

    @Test
    void testGetUserdataSecretCryptographicAuthorizedClientsWhenAccountIdNotPresent() {
        setUpStack("arn:aws:iam:::instance-profile/example-instance-profile", null);
        assertThrows(CloudbreakServiceException.class, () -> underTest.getUserdataSecretCryptographicAuthorizedClients(stack, "instance-id"));
    }

    @Test
    void testGetUserdataSecretType() {
        assertEquals(ResourceType.AWS_SECRETSMANAGER_SECRET, underTest.getUserdataSecretResourceType());
    }

    @Test
    void testPlatform() {
        assertEquals(AwsConstants.AWS_PLATFORM, underTest.platform());
    }

    @Test
    void testVariant() {
        assertEquals(AwsConstants.AWS_DEFAULT_VARIANT, underTest.variant());
    }

    private void setUpEnvironmentTelemetry(String loggerInstanceProfile) {
        S3CloudStorageV1Parameters s3CloudStorageV1Parameters = mock(S3CloudStorageV1Parameters.class);
        when(s3CloudStorageV1Parameters.getInstanceProfile()).thenReturn(loggerInstanceProfile);
        LoggingResponse loggingResponse = mock(LoggingResponse.class);
        when(loggingResponse.getS3()).thenReturn(s3CloudStorageV1Parameters);
        TelemetryResponse telemetryResponse = mock(TelemetryResponse.class);
        when(telemetryResponse.getLogging()).thenReturn(loggingResponse);
        when(environment.getTelemetry()).thenReturn(telemetryResponse);
    }

    private void setUpEnvironmentCredential(String crossAccountRole) {
        RoleBasedParameters roleBasedParameters = mock(RoleBasedParameters.class);
        when(roleBasedParameters.getRoleArn()).thenReturn(crossAccountRole);
        AwsCredentialParameters awsCredentialParameters = mock(AwsCredentialParameters.class);
        when(awsCredentialParameters.getRoleBased()).thenReturn(roleBasedParameters);
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getAws()).thenReturn(awsCredentialParameters);
        when(environment.getCredential()).thenReturn(credentialResponse);
    }

    private void setUpStack(String loggerInstanceProfile, String idBrokerAssumer) {
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("blueprint");
        when(stack.getBlueprintJsonText()).thenReturn("blueprint");
        InstanceGroup master = new InstanceGroup();
        master.setGroupName("master");
        InstanceGroup idbroker = new InstanceGroup();
        idbroker.setGroupName("idbroker");
        when(stack.getInstanceGroups()).thenReturn(Set.of(master, idbroker));

        S3Identity s3IdentityLogger = mock(S3Identity.class);
        when(s3IdentityLogger.getInstanceProfile()).thenReturn(loggerInstanceProfile);
        FileSystemType fileSystemTypeLogger = mock(FileSystemType.class);
        when(fileSystemTypeLogger.isS3()).thenReturn(true);
        CloudIdentity cloudIdentityLogger = mock(CloudIdentity.class);
        when(cloudIdentityLogger.getIdentityType()).thenReturn(CloudIdentityType.LOG);
        when(cloudIdentityLogger.getS3Identity()).thenReturn(s3IdentityLogger);
        when(cloudIdentityLogger.getFileSystemType()).thenReturn(fileSystemTypeLogger);

        S3Identity s3IdentityIdBrokerAssumer = mock(S3Identity.class);
        when(s3IdentityIdBrokerAssumer.getInstanceProfile()).thenReturn(idBrokerAssumer);
        FileSystemType fileSystemTypeIdBrokerAssumer = mock(FileSystemType.class);
        when(fileSystemTypeIdBrokerAssumer.isS3()).thenReturn(true);
        CloudIdentity cloudIdentityIdBrokerAssumer = mock(CloudIdentity.class);
        when(cloudIdentityIdBrokerAssumer.getIdentityType()).thenReturn(CloudIdentityType.ID_BROKER);
        when(cloudIdentityIdBrokerAssumer.getS3Identity()).thenReturn(s3IdentityIdBrokerAssumer);
        when(cloudIdentityIdBrokerAssumer.getFileSystemType()).thenReturn(fileSystemTypeIdBrokerAssumer);

        CloudStorage cloudStorage = mock(CloudStorage.class);
        when(cloudStorage.getCloudIdentities()).thenReturn(List.of(cloudIdentityLogger, cloudIdentityIdBrokerAssumer));
        FileSystem fileSystem = mock(FileSystem.class);
        when(fileSystem.getCloudStorage()).thenReturn(cloudStorage);
        Cluster cluster = mock(Cluster.class);
        when(cluster.getFileSystem()).thenReturn(fileSystem);
        when(stack.getCluster()).thenReturn(cluster);
    }
}
