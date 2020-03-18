package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient.INTERNAL_ACTOR_CRN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.idbmms.GrpcIdbmmsClient;
import com.sequenceiq.cloudbreak.idbmms.exception.IdbmmsOperationException;
import com.sequenceiq.cloudbreak.idbmms.model.MappingsConfig;
import com.sequenceiq.cloudbreak.telemetry.fluent.FluentConfigView;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.StorageIdentityBase;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.model.CloudIdentityType;
import com.sequenceiq.datalake.controller.exception.BadRequestException;
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

    @Mock
    private GrpcIdbmmsClient idbmmsClient;

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
        MockitoAnnotations.initMocks(this);
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
    public void testAddAzureIdbrokerMsiToTelemetrWithoutCloudStoragey() {
        Map<String, Object> attributes = new HashMap<>();
        underTest.addAzureIdbrokerMsiToTelemetry(attributes, stackV4Request);

        assertThat(clusterV4Request.getCloudStorage()).isNull();
        assertThat(attributes.size()).isEqualTo(0);
    }

}
