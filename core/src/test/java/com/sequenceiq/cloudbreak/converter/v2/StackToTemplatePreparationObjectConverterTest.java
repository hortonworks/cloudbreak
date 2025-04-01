package com.sequenceiq.cloudbreak.converter.v2;

import static com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight.CLOUDER_MANAGER_ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ranger.RangerCloudStorageServiceConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.general.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.converter.StackToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.StorageLocation;
import com.sequenceiq.cloudbreak.domain.cloudstorage.AccountMapping;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.dto.LdapView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.ldap.LdapConfigService;
import com.sequenceiq.cloudbreak.sdx.TargetPlatform;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintViewProvider;
import com.sequenceiq.cloudbreak.service.cluster.DatabaseSslService;
import com.sequenceiq.cloudbreak.service.cluster.InstanceGroupMetadataCollector;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsService;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.environment.tag.AccountTagClientService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.identitymapping.AwsMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.AzureMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.identitymapping.GcpMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerFqdnUtil;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.tag.CostTagging;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.filesystem.StorageLocationView;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.CustomConfigurationsView;
import com.sequenceiq.cloudbreak.template.views.DatabaseType;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.backup.response.BackupResponse;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StackToTemplatePreparationObjectConverterTest {

    private static final Long TEST_CLUSTER_ID = 1L;

    private static final String TEST_BLUEPRINT_TEXT = "{}";

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    private static final String TEST_PLATFORM_VARIANT = "AWS_VARIANT";

    private static final String ADMIN_GROUP_NAME = "mockAdmins";

    private static final Map<String, String> MOCK_GROUP_MAPPINGS = Map.of(ADMIN_GROUP_NAME, "mockGroupRole");

    private static final Map<String, String> MOCK_USER_MAPPINGS = Map.of("mockUser", "mockUserRole");

    private static final Map<String, String> GROUP_MAPPINGS = Map.of("group", "groupRole");

    private static final Map<String, String> USER_MAPPINGS = Map.of("user", "userRole");

    private static final String REGION = "region-1";

    private static final String AVAILABILITY_ZONE = "az-1";

    private static final String SSL_CERTS_FILE_PATH = "/foo/bar.pem";

    private static final String DB_SERVER_CRN = "crn:cdp:redbeams:us-west-1:default:databaseServer:e63520c8-aaf0-4bf3-b872-5613ce496ac3";

    @InjectMocks
    private StackToTemplatePreparationObjectConverter underTest;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private InstanceGroupMetadataCollector instanceGroupMetadataCollector;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private DatabaseSslService databaseSslService;

    @Mock
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Mock
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Mock
    private CustomConfigurationsViewProvider customConfigurationsViewProvider;

    @Mock
    private Stack stackMock;

    @Mock
    private Credential credentialMock;

    @Mock
    private Cluster sourceCluster;

    @Mock
    private StackRepoDetails stackRepoDetails;

    @Mock
    private Map<String, List<InstanceMetaData>> groupInstances;

    @Mock
    private Blueprint blueprint;

    @Mock
    private CloudbreakUser user;

    @Mock
    private Json stackInputs;

    @Mock
    private BlueprintViewProvider blueprintViewProvider;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private AwsMockAccountMappingService awsMockAccountMappingService;

    @Mock
    private AzureMockAccountMappingService azureMockAccountMappingService;

    @Mock
    private GcpMockAccountMappingService gcpMockAccountMappingService;

    @Mock
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

    @Mock
    private CostTagging defaultCostTaggingService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CustomConfigurations customConfigurations;

    @Mock
    private VirtualGroupService virtualGroupService;

    @Mock
    private ExposedServiceCollector exposedServiceCollector;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private AccountTagClientService accountTagClientService;

    @Mock
    private IdBrokerService idBrokerService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private DatalakeService datalakeService;

    @Mock
    private CustomConfigurationsService customConfigurationsService;

    @Spy
    @SuppressFBWarnings(value = "UrF", justification = "This gets injected")
    private IdBrokerConverterUtil idBrokerConverterUtil = new IdBrokerConverterUtil();

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private LoadBalancerFqdnUtil loadBalancerFqdnUtil;

    @BeforeEach
    public void setUp() throws IOException, TransactionService.TransactionExecutionException {
        MockitoAnnotations.openMocks(this);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(transactionService).required(any(Runnable.class));
        User user = new User();
        user.setUserName("applebob@apple.com");
        user.setUserCrn("user-crn");
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("account");
        user.setTenant(tenant);
        when(stackMock.getCreator()).thenReturn(user);
        when(stackMock.getEnvironmentCrn()).thenReturn("env");
        when(stackMock.getCloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(stackMock.cloudPlatform()).thenReturn(TEST_CLOUD_PLATFORM);
        when(stackMock.getPlatformVariant()).thenReturn(TEST_PLATFORM_VARIANT);
        when(stackMock.getType()).thenReturn(StackType.DATALAKE);
        when(stackMock.getRegion()).thenReturn(REGION);
        when(stackMock.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(stackMock.getName()).thenReturn("stackname");
        when(sourceCluster.getId()).thenReturn(TEST_CLUSTER_ID);
        when(instanceGroupMetadataCollector.collectMetadata(stackMock)).thenReturn(groupInstances);
        when(stackMock.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getBlueprintJsonText()).thenReturn(TEST_BLUEPRINT_TEXT);
        when(blueprint.getStackVersion()).thenReturn("7.2.11");
        when(stackMock.getInputs()).thenReturn(stackInputs);
        when(stackInputs.get(StackInputs.class)).thenReturn(null);
        when(stackMock.getEnvironmentCrn()).thenReturn(TestConstants.CRN);
        when(stackMock.getCluster()).thenReturn(sourceCluster);
        when(sourceCluster.getCustomConfigurations()).thenReturn(customConfigurations);
        when(customConfigurations.getCrn()).thenReturn("test-custom-configs-crn");
        when(stackMock.getResourceCrn()).thenReturn("crn:cdp:datahub:us-west-1:account:cluster:cluster");
        when(stackMock.getCluster()).thenReturn(sourceCluster);
        when(accountTagClientService.list()).thenReturn(new HashMap<>());
        when(entitlementService.internalTenant(anyString())).thenReturn(true);
        when(loadBalancerFqdnUtil.getLoadBalancerUserFacingFQDN(anyLong())).thenReturn(null);
        Credential credential = Credential.builder()
                .crn("aCredentialCRN")
                .attributes(new Json(""))
                .build();
        DetailedEnvironmentResponse environmentResponse = DetailedEnvironmentResponse.builder()
                .withIdBrokerMappingSource(IdBrokerMappingSource.MOCK)
                .withCredential(new CredentialResponse())
                .withAdminGroupName(ADMIN_GROUP_NAME)
                .withCrn(TestConstants.CRN)
                .build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(credentialConverter.convert(any(CredentialResponse.class))).thenReturn(credential);
        lenient().when(awsMockAccountMappingService.getGroupMappings(REGION, cloudCredential, ADMIN_GROUP_NAME)).thenReturn(MOCK_GROUP_MAPPINGS);
        lenient().when(awsMockAccountMappingService.getUserMappings(REGION, cloudCredential)).thenReturn(MOCK_USER_MAPPINGS);
        lenient().when(azureMockAccountMappingService.getGroupMappings("msi", cloudCredential, ADMIN_GROUP_NAME)).thenReturn(MOCK_GROUP_MAPPINGS);
        lenient().when(azureMockAccountMappingService.getUserMappings("msi", cloudCredential)).thenReturn(MOCK_USER_MAPPINGS);
        lenient().when(gcpMockAccountMappingService.getGroupMappings(REGION, cloudCredential, ADMIN_GROUP_NAME)).thenReturn(MOCK_GROUP_MAPPINGS);
        lenient().when(gcpMockAccountMappingService.getUserMappings(REGION, cloudCredential)).thenReturn(MOCK_USER_MAPPINGS);
        when(ldapConfigService.get(anyString(), anyString())).thenReturn(Optional.empty());
        when(exposedServiceCollector.getAllKnoxExposed(any())).thenReturn(Set.of());
        when(stackMock.getResources()).thenReturn(Collections.EMPTY_SET);
        IdBroker idbroker = idBrokerConverterUtil.generateIdBrokerSignKeys(TEST_CLUSTER_ID, null);
        when(idBrokerService.getByCluster(anyLong())).thenReturn(idbroker);
        when(idBrokerService.save(any(IdBroker.class))).thenReturn(idbroker);
        when(grpcUmsClient.listServicePrincipalCloudIdentities(anyString(), anyString())).thenReturn(Collections.EMPTY_LIST);
        when(databaseSslService.getSslCertsFilePath()).thenReturn(SSL_CERTS_FILE_PATH);
        when(stackMock.getId()).thenReturn(1L);
        when(generalClusterConfigsProvider.generalClusterConfigs(any(StackDtoDelegate.class), any(Credential.class)))
                .thenReturn(new GeneralClusterConfigs());
    }

    @Test
    public void testConvertWhenClusterGivesGatewayThenNotNullShouldBeStored() {
        Gateway gateway = mock(Gateway.class);
        when(gateway.getSignKey()).thenReturn(null);
        when(stackMock.getGateway()).thenReturn(gateway);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getGatewayView()).isNotNull();
    }

    @Test
    public void testConvertWhenClusterDoesNotGivesAGatewayThenNullShouldBeStored() {
        when(stackMock.getGateway()).thenReturn(null);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getGatewayView()).isNull();
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenSecretEncryptionIsEnabled() {
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);
        DetailedEnvironmentResponse environmentResponse = DetailedEnvironmentResponse.builder()
                .withIdBrokerMappingSource(IdBrokerMappingSource.MOCK)
                .withCredential(new CredentialResponse())
                .withAdminGroupName(ADMIN_GROUP_NAME)
                .withCrn(TestConstants.CRN)
                .withEnableSecretEncryption(true)
                .build();
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertTrue(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenClusterProvidesFileSystemThenBaseFileSystemConfigurationsViewShouldBeExists() throws IOException {
        FileSystem sourceFileSystem = new FileSystem();
        ConfigQueryEntries configQueryEntries = new ConfigQueryEntries();
        BaseFileSystemConfigurationsView expected = mock(BaseFileSystemConfigurationsView.class);
        when(sourceCluster.getFileSystem()).thenReturn(sourceFileSystem);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(eq(sourceFileSystem), eq(stackMock), any(),
                eq(new Json("")), eq(configQueryEntries))).thenReturn(expected);
        when(cmCloudStorageConfigProvider.getConfigQueryEntries()).thenReturn(configQueryEntries);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getFileSystemConfigurationView().isPresent()).isTrue();
        assertThat(result.getFileSystemConfigurationView().get()).isEqualTo(expected);
        verify(fileSystemConfigurationProvider, times(1)).fileSystemConfiguration(eq(sourceFileSystem),
                eq(stackMock), any(), eq(new Json("")), eq(configQueryEntries));
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenClusterProvidesFileSystemThenBaseFileSystemConfigurationsViewShouldBeExistsAzure() throws IOException {
        FileSystem sourceFileSystem = new FileSystem();
        ConfigQueryEntries configQueryEntries = new ConfigQueryEntries();
        BaseFileSystemConfigurationsView expected = mock(BaseFileSystemConfigurationsView.class);
        when(sourceCluster.getFileSystem()).thenReturn(sourceFileSystem);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(eq(sourceFileSystem), eq(stackMock), any(),
                eq(new Json("")), eq(configQueryEntries))).thenReturn(expected);
        when(cmCloudStorageConfigProvider.getConfigQueryEntries()).thenReturn(configQueryEntries);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);
        when(stackMock.getCloudPlatform()).thenReturn("AZURE");

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getFileSystemConfigurationView().isPresent()).isTrue();
        assertThat(result.getFileSystemConfigurationView().get()).isEqualTo(expected);
        verify(fileSystemConfigurationProvider, times(1)).fileSystemConfiguration(eq(sourceFileSystem),
                eq(stackMock), any(), eq(new Json("")), eq(configQueryEntries));
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenClusterProvidesFileSystemThenBaseFileSystemConfigurationsViewShouldBeExistsGCP() throws IOException {
        FileSystem sourceFileSystem = new FileSystem();
        ConfigQueryEntries configQueryEntries = new ConfigQueryEntries();
        BaseFileSystemConfigurationsView expected = mock(BaseFileSystemConfigurationsView.class);
        when(sourceCluster.getFileSystem()).thenReturn(sourceFileSystem);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(eq(sourceFileSystem), eq(stackMock), any(),
                eq(new Json("")), eq(configQueryEntries))).thenReturn(expected);
        when(cmCloudStorageConfigProvider.getConfigQueryEntries()).thenReturn(configQueryEntries);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);
        when(stackMock.getCloudPlatform()).thenReturn("GCP");

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getFileSystemConfigurationView().isPresent()).isTrue();
        assertThat(result.getFileSystemConfigurationView().get()).isEqualTo(expected);
        verify(fileSystemConfigurationProvider, times(1)).fileSystemConfiguration(eq(sourceFileSystem),
                eq(stackMock), any(), eq(new Json("")), eq(configQueryEntries));
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenClusterDoesNotProvidesFileSystemThenBaseFileSystemConfigurationsViewShouldBeEmpty() throws IOException {
        FileSystem clusterServiceFileSystem = new FileSystem();
        BaseFileSystemConfigurationsView expected = mock(BaseFileSystemConfigurationsView.class);
        when(sourceCluster.getFileSystem()).thenReturn(null);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(clusterServiceFileSystem, stackMock, resourceType -> Collections.EMPTY_LIST,
                new Json(""), new ConfigQueryEntries())).thenReturn(expected);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getFileSystemConfigurationView().isPresent()).isFalse();
        verify(fileSystemConfigurationProvider, times(0)).fileSystemConfiguration(clusterServiceFileSystem,
                stackMock, resourceType -> Collections.EMPTY_LIST, new Json(""), new ConfigQueryEntries());
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertIfTheAttemptOfObtainingBaseFileSystemConfigurationsViewThrowsIOExceptionThenCloudbreakServiceExceptionShouldComeOutside()
            throws IOException {
        String ioExceptionMessage = "Unable to obtain BaseFileSystemConfigurationsView";
        IOException invokedException = new IOException(ioExceptionMessage);
        FileSystem sourceFileSystem = new FileSystem();
        ConfigQueryEntries configQueryEntries = new ConfigQueryEntries();
        when(sourceCluster.getFileSystem()).thenReturn(sourceFileSystem);
        when(stackMock.getEnvironmentCrn()).thenReturn("envCredentialCRN");
        when(fileSystemConfigurationProvider.fileSystemConfiguration(eq(sourceFileSystem), eq(stackMock), any(), eq(new Json("")),
                eq(configQueryEntries))).thenThrow(invokedException);
        when(cmCloudStorageConfigProvider.getConfigQueryEntries()).thenReturn(configQueryEntries);
        when(stackMock.getStack()).thenReturn(stackMock);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class, () -> underTest.convert(stackMock));
        assertThat(cloudbreakServiceException).hasMessage(ioExceptionMessage).hasCause(invokedException);
    }

    @Test
    public void testConvertWhenEnvironmentBackupLocationDefinedThenBaseFileSystemConfigurationsViewShouldAddIt() throws IOException {
        String backupLocation = "s3a://test";
        FileSystem sourceFileSystem = new FileSystem();
        ConfigQueryEntries configQueryEntries = new ConfigQueryEntries();
        BaseFileSystemConfigurationsView expected = mock(BaseFileSystemConfigurationsView.class);
        List<StorageLocationView> storageLocationViews = mock(List.class);
        BackupResponse backupResponse = new BackupResponse();
        backupResponse.setStorageLocation(backupLocation);
        DetailedEnvironmentResponse environmentResponse = DetailedEnvironmentResponse.builder()
                .withIdBrokerMappingSource(IdBrokerMappingSource.MOCK)
                .withCredential(new CredentialResponse())
                .withAdminGroupName(ADMIN_GROUP_NAME)
                .withCrn(TestConstants.CRN)
                .withBackup(backupResponse)
                .build();
        StorageLocation storageLocation = new StorageLocation();
        storageLocation.setValue(backupLocation);
        storageLocation.setProperty(RangerCloudStorageServiceConfigProvider.DEFAULT_BACKUP_DIR);
        StorageLocationView backupLocationView = new StorageLocationView(storageLocation);
        when(sourceCluster.getFileSystem()).thenReturn(sourceFileSystem);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(eq(sourceFileSystem), eq(stackMock), any(),
                eq(new Json("")), eq(configQueryEntries))).thenReturn(expected);
        when(cmCloudStorageConfigProvider.getConfigQueryEntries()).thenReturn(configQueryEntries);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(expected.getLocations()).thenReturn(storageLocationViews);
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getFileSystemConfigurationView().isPresent()).isTrue();
        assertThat(result.getFileSystemConfigurationView().get()).isEqualTo(expected);
        verify(fileSystemConfigurationProvider, times(1)).fileSystemConfiguration(eq(sourceFileSystem),
                eq(stackMock), any(), eq(new Json("")), eq(configQueryEntries));
        verify(expected, times(1)).getLocations();
        verify(storageLocationViews, times(1)).add(eq(backupLocationView));
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenClusterFromClusterServiceHasLdapConfigThenItShouldBeStored() {
        LdapView ldapView = LdapView.LdapViewBuilder.aLdapView().withProtocol("").withBindDn("admin<>").build();
        when(ldapConfigService.get(anyString(), anyString())).thenReturn(Optional.of(ldapView));
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getLdapConfig().isPresent()).isTrue();
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenClusterFromClusterServiceHasNoLdapConfigThenTheOptionalShouldBeEmpty() {
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getLdapConfig().isPresent()).isFalse();
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertBlueprintViewShouldMatch() {
        BlueprintView expected = mock(BlueprintView.class);
        when(blueprintViewProvider.getBlueprintView(blueprint)).thenReturn(expected);
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getBlueprintView()).isSameAs(expected);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenCustomConfigsProvidedThenItShouldBeInvoked() {
        CustomConfigurationsView expected = mock(CustomConfigurationsView.class);
        when(customConfigurationsService.getByCrn(any())).thenReturn(customConfigurations);
        when(customConfigurationsViewProvider.getCustomConfigurationsView(customConfigurations)).thenReturn(expected);
        when(stackMock.getType()).thenReturn(StackType.WORKLOAD);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);
        SdxBasicView mockSdx = mock(SdxBasicView.class);
        when(platformAwareSdxConnector.getSdxBasicViewByEnvironmentCrn(anyString())).thenReturn(Optional.of(mockSdx));
        when(mockSdx.crn()).thenReturn("crn:cdp:sdxsvc:us-west-1:cloudera:instance:f22e7f31-a98d-424d-917a-a62a36cb3c9e");
        when(mockSdx.dbServerCrn()).thenReturn(DB_SERVER_CRN);
        when(mockSdx.razEnabled()).thenReturn(false);
        StackDto datalakeStack = mock();
        ClusterView cluster = mock();
        when(datalakeStack.getCluster()).thenReturn(cluster);
        when(stackDtoService.getByCrn(any())).thenReturn(datalakeStack);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getCustomConfigurationsView()).isPresent();
        assertThat(result.getCustomConfigurationsView()).isEqualTo(Optional.of(expected));
        assertEquals(result.getDatalakeView().get().getTargetPlatform(), TargetPlatform.CDL);
        assertEquals(result.getDatalakeView().get().isRazEnabled(), false);
        assertEquals(result.getDatalakeView().get().getDatabaseType(), DatabaseType.EXTERNAL_DATABASE);
        verify(customConfigurationsViewProvider, times(1)).getCustomConfigurationsView(customConfigurations);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenClusterHasNoCustomConfigsThenOptionalShouldBeEmpty() {
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getCustomConfigurationsView()).isEmpty();
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenClusterHasCustomConfigsWithEmptyRuntime() {
        CustomConfigurationsView expected = new CustomConfigurationsView("test-name", "test-crn", "", Collections.emptySet());
        when(customConfigurationsService.getByCrn(any())).thenReturn(customConfigurations);
        when(customConfigurationsViewProvider.getCustomConfigurationsView(customConfigurations)).thenReturn(expected);
        when(stackMock.getType()).thenReturn(StackType.WORKLOAD);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getCustomConfigurationsView()).isPresent();
        assertThat(result.getCustomConfigurationsView()).isEqualTo(Optional.of(expected));
        verify(customConfigurationsViewProvider, times(1)).getCustomConfigurationsView(customConfigurations);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenDataLakeIdNotNullThenExpectedSharedServiceConfigsShouldBeStored() {
        // just in case adding one to avoid matching with the class variable
        SharedServiceConfigsView expected = new SharedServiceConfigsView();
        when(sourceCluster.getPassword()).thenReturn("pwd");
        when(stackMock.getDatalakeCrn()).thenReturn("crn");
        when(stackMock.getEnvironmentCrn()).thenReturn("envCrn");

        when(datalakeService.createSharedServiceConfigsView("pwd", StackType.DATALAKE, "envCrn")).thenReturn(expected);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getSharedServiceConfigs().isPresent()).isTrue();
        assertThat(result.getSharedServiceConfigs().get()).isEqualTo(expected);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenStackInputsContainsCustomInputsThenThisShouldBeStored() throws IOException {
        StackInputs stackInputs = mock(StackInputs.class);
        Map<String, Object> customInputs = new LinkedHashMap<>();
        when(this.stackInputs.get(StackInputs.class)).thenReturn(stackInputs);
        when(stackInputs.getCustomInputs()).thenReturn(customInputs);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getCustomInputs()).isEqualTo(customInputs);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenStackInputsDoesNotContainsCustomInputsThenEmptyMapBeStored() throws IOException {
        StackInputs stackInputs = mock(StackInputs.class);
        when(this.stackInputs.get(StackInputs.class)).thenReturn(stackInputs);
        when(stackInputs.getCustomInputs()).thenReturn(null);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getCustomInputs().isEmpty()).isTrue();
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenUnableToGetStackInputsThenCloudbreakServiceExceptionWouldCome() throws IOException {
        String ioExceptionMessage = "unable to get inputs";
        IOException invokedException = new IOException(ioExceptionMessage);
        when(stackInputs.get(StackInputs.class)).thenThrow(invokedException);
        when(stackMock.getStack()).thenReturn(stackMock);

        CloudbreakServiceException cloudbreakServiceException = assertThrows(CloudbreakServiceException.class, () -> underTest.convert(stackMock));
        assertThat(cloudbreakServiceException).hasMessage(ioExceptionMessage).hasCause(invokedException);
    }

    @Test
    public void testConvertCloudPlatformAndVariantMatch() {
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getCloudPlatform()).isEqualTo(CloudPlatform.AWS);
        assertThat(result.getPlatformVariant()).isEqualTo(TEST_PLATFORM_VARIANT);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testMockAccountMappings() {
        when(virtualGroupService.createOrGetVirtualGroup(any(VirtualGroupRequest.class), eq(CLOUDER_MANAGER_ADMIN))).thenReturn("mockAdmins");
        when(stackMock.getCluster().getFileSystem()).thenReturn(new FileSystem());
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        AccountMappingView accountMappingView = result.getAccountMappingView();
        assertThat(accountMappingView).isNotNull();
        assertThat(accountMappingView.getGroupMappings()).isEqualTo(MOCK_GROUP_MAPPINGS);
        assertThat(accountMappingView.getUserMappings()).isEqualTo(MOCK_USER_MAPPINGS);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testMockAccountMappingsWhenNoFileSystemShouldReturnEmptyList() {
        when(virtualGroupService.createOrGetVirtualGroup(any(VirtualGroupRequest.class), eq(CLOUDER_MANAGER_ADMIN))).thenReturn("mockAdmins");
        when(stackMock.getCluster().getFileSystem()).thenReturn(null);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        AccountMappingView accountMappingView = result.getAccountMappingView();
        assertThat(accountMappingView).isNull();
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testStackInputAccountMappings() {
        FileSystem sourceFileSystem = mock(FileSystem.class);
        CloudStorage sourceCloudStorage = mock(CloudStorage.class);
        when(sourceCluster.getFileSystem()).thenReturn(sourceFileSystem);
        when(sourceFileSystem.getCloudStorage()).thenReturn(sourceCloudStorage);
        when(stackMock.getStack()).thenReturn(stackMock);

        AccountMapping accountMapping = new AccountMapping();
        accountMapping.setGroupMappings(GROUP_MAPPINGS);
        accountMapping.setUserMappings(USER_MAPPINGS);
        when(sourceCloudStorage.getAccountMapping()).thenReturn(accountMapping);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());

        TemplatePreparationObject result = underTest.convert(stackMock);

        AccountMappingView accountMappingView = result.getAccountMappingView();
        assertThat(accountMappingView).isNotNull();
        assertThat(accountMappingView.getGroupMappings()).isEqualTo(GROUP_MAPPINGS);
        assertThat(accountMappingView.getUserMappings()).isEqualTo(USER_MAPPINGS);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testStackPlacement() {
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getPlacementView().isPresent()).isTrue();
        assertThat(result.getPlacementView().get().getRegion()).isEqualTo(REGION);
        assertThat(result.getPlacementView().get().getAvailabilityZone()).isEqualTo(AVAILABILITY_ZONE);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testMissingClouderaManagerIp() {
        GeneralClusterConfigs configs = new GeneralClusterConfigs();
        Optional<String> primaryGatewayFqdn = Optional.of("primaryFqdn");
        configs.setPrimaryGatewayInstanceDiscoveryFQDN(primaryGatewayFqdn);
        when(generalClusterConfigsProvider.generalClusterConfigs(any(Stack.class), any(Credential.class))).thenReturn(configs);
        when(gatewayConfigService.getPrimaryGatewayIp(any(Stack.class))).thenReturn("10.0.0.1");
        InstanceMetaData dummyMetadata = new InstanceMetaData();
        when(stackMock.getPrimaryGatewayInstance()).thenReturn(dummyMetadata);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result.getGeneralClusterConfigs().getClusterManagerIp()).isEqualTo("10.0.0.1");
        verify(gatewayConfigService, times(1)).getPrimaryGatewayIp(any(Stack.class));
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    void testRdsSslCertificateFilePath() {
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertThat(result).isNotNull();
        assertThat(result.getRdsSslCertificateFilePath()).isEqualTo(SSL_CERTS_FILE_PATH);
        assertFalse(result.isEnableSecretEncryption());
    }

    @Test
    public void testConvertWhenLoadBalanceExistsFqdnIsSet() {
        String lbUrl = "loadbalancer.domain";
        when(loadBalancerFqdnUtil.getLoadBalancerUserFacingFQDN(anyLong())).thenReturn(lbUrl);
        when(blueprintViewProvider.getBlueprintView(any())).thenReturn(getBlueprintView());
        when(stackMock.getStack()).thenReturn(stackMock);

        TemplatePreparationObject result = underTest.convert(stackMock);

        assertEquals(lbUrl, result.getGeneralClusterConfigs().getLoadBalancerGatewayFqdn().get());
        assertFalse(result.isEnableSecretEncryption());
    }

    private BlueprintView getBlueprintView() {
        BlueprintView blueprint = new BlueprintView();
        blueprint.setVersion("7.2.14");
        return blueprint;
    }

}
