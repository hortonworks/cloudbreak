package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;

@ExtendWith(MockitoExtension.class)
public class StackRequestManifesterTest {

    private static final String ENVIRONMENT_CRN = "crn:myEnvironment";

    private static final String BAD_ENVIRONMENT_CRN = "crn:myBadEnvironment";

    private static final String STACK_NAME = "myStack";

    private static final String CLOUD_PLATFORM_AWS = CloudPlatform.AWS.name();

    private static final String CLOUD_PLATFORM_AZURE = CloudPlatform.AZURE.name();

    private static final String USER_1 = "user1";

    private static final String GROUP_1 = "group1";

    private static final String USER_2 = "user2";

    private static final String GROUP_2 = "group2";

    private static final String USER_ROLE_1 = "user-role1";

    private static final String GROUP_ROLE_1 = "group-role1";

    private static final String USER_ROLE_2 = "user-role2";

    private static final String GROUP_ROLE_2 = "group-role2";

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENCRYPTION_KEY = "mykey";

    @Mock
    private GrpcIdbmmsClient idbmmsClient;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackV4Request stackV4Request;

    private ClusterV4Request clusterV4Request;

    private CloudStorageRequest cloudStorage;

    @Mock
    private MappingsConfig mappingsConfig;

    @InjectMocks
    private StackRequestManifester underTest;

    @BeforeEach
    public void setUp() {
        clusterV4Request = new ClusterV4Request();
        cloudStorage = new CloudStorageRequest();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenNoCloudStorage() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isNull();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithExistingAccountMappingAndEmptyMaps() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);

        clusterV4Request.setCloudStorage(cloudStorage);
        AccountMappingBase accountMapping = new AccountMappingBase();
        cloudStorage.setAccountMapping(accountMapping);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isSameAs(accountMapping);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping().getGroupMappings()).isEmpty();
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping().getUserMappings()).isEmpty();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithExistingAccountMappingAndNonemptyMaps() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        clusterV4Request.setCloudStorage(cloudStorage);
        AccountMappingBase accountMapping = new AccountMappingBase();
        accountMapping.setGroupMappings(Map.ofEntries(Map.entry(GROUP_1, GROUP_ROLE_1)));
        accountMapping.setUserMappings(Map.ofEntries(Map.entry(USER_1, USER_ROLE_1)));
        cloudStorage.setAccountMapping(accountMapping);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isSameAs(accountMapping);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping().getGroupMappings()).containsOnly(Map.entry(GROUP_1, GROUP_ROLE_1));
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping().getUserMappings()).containsOnly(Map.entry(USER_1, USER_ROLE_1));
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndIdbmmsSourceAndSuccess() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        when(idbmmsClient.getMappingsConfig(INTERNAL_ACTOR_CRN, ENVIRONMENT_CRN, Optional.empty())).thenReturn(mappingsConfig);
        when(mappingsConfig.getGroupMappings()).thenReturn(Map.ofEntries(Map.entry(GROUP_2, GROUP_ROLE_2)));
        when(mappingsConfig.getActorMappings()).thenReturn(Map.ofEntries(Map.entry(USER_2, USER_ROLE_2)));

        clusterV4Request.setCloudStorage(cloudStorage);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        AccountMappingBase accountMapping = clusterV4Request.getCloudStorage().getAccountMapping();
        assertThat(accountMapping).isNotNull();
        assertThat(accountMapping.getGroupMappings()).containsOnly(Map.entry(GROUP_2, GROUP_ROLE_2));
        assertThat(accountMapping.getUserMappings()).containsOnly(Map.entry(USER_2, USER_ROLE_2));
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndIdbmmsSourceAndFailure() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        when(idbmmsClient.getMappingsConfig(INTERNAL_ACTOR_CRN, BAD_ENVIRONMENT_CRN, Optional.empty()))
                .thenThrow(new IdbmmsOperationException("Houston, we have a problem."));

        clusterV4Request.setCloudStorage(cloudStorage);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> underTest.setupCloudStorageAccountMapping(stackV4Request, BAD_ENVIRONMENT_CRN, IdBrokerMappingSource.IDBMMS, CLOUD_PLATFORM_AWS));
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndMockSourceAndAws() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        clusterV4Request.setCloudStorage(cloudStorage);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.MOCK, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isNull();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndMockSourceAndAzure() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        clusterV4Request.setCloudStorage(cloudStorage);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.MOCK, CLOUD_PLATFORM_AZURE);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isNull();
    }

    @Test
    public void testSetupCloudStorageAccountMappingWhenCloudStorageWithNoAccountMappingAndNoneSource() {
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        when(stackV4Request.getName()).thenReturn(STACK_NAME);
        clusterV4Request.setCloudStorage(cloudStorage);

        underTest.setupCloudStorageAccountMapping(stackV4Request, ENVIRONMENT_CRN, IdBrokerMappingSource.NONE, CLOUD_PLATFORM_AWS);

        assertThat(clusterV4Request.getCloudStorage()).isSameAs(cloudStorage);
        assertThat(clusterV4Request.getCloudStorage().getAccountMapping()).isNull();
    }

    @Test
    public void testAddAzureIdbrokerMsiToTelemetry() {
        Map<String, Object> attributes = new HashMap<>();
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        List<StorageIdentityBase> identities = new ArrayList<>();
        StorageIdentityBase identity = new StorageIdentityBase();
        identity.setType(CloudIdentityType.ID_BROKER);
        AdlsGen2CloudStorageV1Parameters adlsGen2 = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2.setManagedIdentity("msi");
        identity.setAdlsGen2(adlsGen2);
        identities.add(identity);
        cloudStorageRequest.setIdentities(identities);
        when(stackV4Request.getCluster()).thenReturn(clusterV4Request);
        clusterV4Request.setCloudStorage(cloudStorageRequest);

        underTest.addAzureIdbrokerMsiToTelemetry(attributes, stackV4Request);

        assertThat(clusterV4Request.getCloudStorage()).isNotNull();
        assertThat(attributes.get(FluentConfigView.AZURE_IDBROKER_INSTANCE_MSI)).isEqualTo("msi");
    }

    @Test
    public void testAddAzureIdbrokerMsiToTelemetryWithoutCloudStorage() {
        Map<String, Object> attributes = new HashMap<>();
        underTest.addAzureIdbrokerMsiToTelemetry(attributes, stackV4Request);

        assertThat(clusterV4Request.getCloudStorage()).isNull();
        assertThat(attributes.size()).isEqualTo(0);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAzure() {
        underTest.setupInstanceVolumeEncryption(stackV4Request, CLOUD_PLATFORM_AZURE, ACCOUNT_ID);

        verify(stackV4Request, never()).getInstanceGroups();
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndNotEntitled() {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(false);

        underTest.setupInstanceVolumeEncryption(stackV4Request, CLOUD_PLATFORM_AWS, ACCOUNT_ID);

        verify(stackV4Request, never()).getInstanceGroups();
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndEntitledAndNoInstanceGroups() {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(true);

        List<InstanceGroupV4Request> instanceGroups = new ArrayList<>();
        when(stackV4Request.getInstanceGroups()).thenReturn(instanceGroups);

        underTest.setupInstanceVolumeEncryption(stackV4Request, CLOUD_PLATFORM_AWS, ACCOUNT_ID);

        assertThat(instanceGroups).isEmpty();
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndEntitledAndNoInstanceTemplateParameters() {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(true);

        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, CLOUD_PLATFORM_AWS, ACCOUNT_ID);

        verifyEncryption(instanceGroupV4Request.getTemplate(), EncryptionType.DEFAULT);
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndEntitledAndNoEncryptionParameters() {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(true);

        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request = instanceGroupV4Request.getTemplate();
        instanceTemplateV4Request.createAws();
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, CLOUD_PLATFORM_AWS, ACCOUNT_ID);

        verifyEncryption(instanceTemplateV4Request, EncryptionType.DEFAULT);
    }

    static Object[][] encryptionTypeDataProvider() {
        return new Object[][]{
                // testCaseName, encryptionType, encryptionKey, encryptionTypeExpected, encryptionKeyExpected
                {"encryptionType == null", null, null, EncryptionType.DEFAULT, null},
                {"EncryptionType.NONE", EncryptionType.NONE, null, EncryptionType.NONE, null},
                {"EncryptionType.DEFAULT", EncryptionType.DEFAULT, null, EncryptionType.DEFAULT, null},
                {"EncryptionType.CUSTOM", EncryptionType.CUSTOM, ENCRYPTION_KEY, EncryptionType.CUSTOM, ENCRYPTION_KEY},
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("encryptionTypeDataProvider")
    void setupInstanceVolumeEncryptionTestWhenAwsAndEntitledAndEncryptionParameters(String testCaseName, EncryptionType encryptionType, String encryptionKey,
            EncryptionType encryptionTypeExpected, String encryptionKeyExpected) {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(true);

        InstanceGroupV4Request instanceGroupV4Request = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request = instanceGroupV4Request.getTemplate();
        if (encryptionKey == null) {
            instanceTemplateV4Request.createAws().setEncryption(createAwsEncryptionV4Parameters(encryptionType));
        } else {
            instanceTemplateV4Request.createAws().setEncryption(createAwsEncryptionV4Parameters(encryptionType, encryptionKey));
        }
        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request));

        underTest.setupInstanceVolumeEncryption(stackV4Request, CLOUD_PLATFORM_AWS, ACCOUNT_ID);

        if (encryptionKeyExpected == null) {
            verifyEncryption(instanceTemplateV4Request, encryptionTypeExpected);
        } else {
            verifyEncryption(instanceTemplateV4Request, encryptionTypeExpected, encryptionKeyExpected);
        }
    }

    @Test
    void setupInstanceVolumeEncryptionTestWhenAwsAndEntitledAndTwoInstanceGroupsAndEncryptionTypesNoneCustom() {
        when(entitlementService.freeIpaDlEbsEncryptionEnabled(INTERNAL_ACTOR_CRN, ACCOUNT_ID)).thenReturn(true);

        InstanceGroupV4Request instanceGroupV4Request1 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request1 = instanceGroupV4Request1.getTemplate();
        instanceTemplateV4Request1.createAws().setEncryption(createAwsEncryptionV4Parameters(EncryptionType.NONE));

        InstanceGroupV4Request instanceGroupV4Request2 = createInstanceGroupV4Request();
        InstanceTemplateV4Request instanceTemplateV4Request2 = instanceGroupV4Request2.getTemplate();
        instanceTemplateV4Request2.createAws().setEncryption(createAwsEncryptionV4Parameters(EncryptionType.CUSTOM, ENCRYPTION_KEY));

        when(stackV4Request.getInstanceGroups()).thenReturn(List.of(instanceGroupV4Request1, instanceGroupV4Request2));

        underTest.setupInstanceVolumeEncryption(stackV4Request, CLOUD_PLATFORM_AWS, ACCOUNT_ID);

        verifyEncryption(instanceTemplateV4Request1, EncryptionType.NONE);
        verifyEncryption(instanceTemplateV4Request2, EncryptionType.CUSTOM, ENCRYPTION_KEY);
    }

    private InstanceGroupV4Request createInstanceGroupV4Request() {
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setTemplate(new InstanceTemplateV4Request());
        return instanceGroupV4Request;
    }

    private void verifyEncryption(InstanceTemplateV4Request instanceTemplateV4Request, EncryptionType expectedEncryptionType) {
        verifyEncryption(instanceTemplateV4Request, expectedEncryptionType, null);
    }

    private void verifyEncryption(InstanceTemplateV4Request instanceTemplateV4Request, EncryptionType expectedEncryptionType, String expectedEncryptionKey) {
        AwsInstanceTemplateV4Parameters aws = instanceTemplateV4Request.getAws();
        assertThat(aws).isNotNull();
        AwsEncryptionV4Parameters encryption = aws.getEncryption();
        assertThat(encryption).isNotNull();
        assertThat(encryption.getType()).isEqualTo(expectedEncryptionType);
        assertThat(encryption.getKey()).isEqualTo(expectedEncryptionKey);
    }

    private AwsEncryptionV4Parameters createAwsEncryptionV4Parameters(EncryptionType encryptionType) {
        return createAwsEncryptionV4Parameters(encryptionType, null);
    }

    private AwsEncryptionV4Parameters createAwsEncryptionV4Parameters(EncryptionType encryptionType, String encryptionKey) {
        AwsEncryptionV4Parameters awsEncryptionV4Parameters = new AwsEncryptionV4Parameters();
        awsEncryptionV4Parameters.setType(encryptionType);
        awsEncryptionV4Parameters.setKey(encryptionKey);
        return awsEncryptionV4Parameters;
    }

}
