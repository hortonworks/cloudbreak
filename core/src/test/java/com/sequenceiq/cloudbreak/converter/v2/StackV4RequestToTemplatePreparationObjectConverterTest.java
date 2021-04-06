package com.sequenceiq.cloudbreak.converter.v2;

import static com.sequenceiq.cloudbreak.TestUtil.rdsConfig;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.cloudstorage.CmCloudStorageConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.general.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.cmtemplate.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintViewProvider;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialConverter;
import com.sequenceiq.cloudbreak.service.identitymapping.AwsMockAccountMappingService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.AccountMappingView;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.ProductDetailsView;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.cloudstorage.AccountMappingBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.common.api.cloudstorage.query.ConfigQueryEntries;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

public class StackV4RequestToTemplatePreparationObjectConverterTest {

    private static final String TEST_CREDENTIAL_NAME = "testCred";

    private static final String TEST_BLUEPRINT_NAME = "testBp";

    private static final String TEST_BLUEPRINT_TEXT = "{}";

    private static final int GENERAL_TEST_QUANTITY = 2;

    private static final String TEST_VERSION = "2.6";

    private static final String TEST_ENVIRONMENT_CRN = TestConstants.CRN;

    private static final String TEST_CLUSTER_NAME = "clustername";

    private static final String ADMIN_GROUP_NAME = "mockAdmins";

    private static final Map<String, String> MOCK_GROUP_MAPPINGS = Map.of(ADMIN_GROUP_NAME, "mockGroupRole");

    private static final Map<String, String> MOCK_USER_MAPPINGS = Map.of("mockUser", "mockUserRole");

    private static final Map<String, String> GROUP_MAPPINGS = Map.of("group", "groupRole");

    private static final Map<String, String> USER_MAPPINGS = Map.of("user", "userRole");

    private static final String REGION = "region-1";

    private static final String AVAILABILITY_ZONE = "az-1";

    @InjectMocks
    private StackV4RequestToTemplatePreparationObjectConverter underTest;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Mock
    private StackInfoService stackInfoService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackV4Request source;

    @Mock
    private UserService userService;

    // TODO: mocking POJOs is a terrible practice and must be stopped!!!
    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private Credential credential;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private PlacementSettingsV4Request placementSettings;

    @Mock
    private ClusterV4Request cluster;

    @Mock
    private Blueprint blueprint;

    @Mock
    private BlueprintStackInfo blueprintStackInfo;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private BlueprintViewProvider blueprintViewProvider;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private CredentialConverter credentialConverter;

    @Mock
    private CredentialResponse credentialResponse;

    @Mock
    private AwsMockAccountMappingService awsMockAccountMappingService;

    @Mock
    private CloudStorageConverter cloudStorageConverter;

    @Mock
    private CmCloudStorageConfigProvider cmCloudStorageConfigProvider;

    @Mock
    private ExposedServiceCollector exposedServiceCollector;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(source.getEnvironmentCrn()).thenReturn(TEST_ENVIRONMENT_CRN);
        when(source.getCluster()).thenReturn(cluster);
        when(source.getCloudPlatform()).thenReturn(CloudPlatform.AWS);
        when(source.getType()).thenReturn(StackType.DATALAKE);
        when(source.getPlacement()).thenReturn(placementSettings);
        when(source.getName()).thenReturn(TEST_CLUSTER_NAME);
        when(cluster.getBlueprintName()).thenReturn(TEST_BLUEPRINT_NAME);
        when(cluster.getName()).thenReturn(TEST_CLUSTER_NAME);
        when(blueprintService.getByNameForWorkspace(TEST_BLUEPRINT_NAME, workspace)).thenReturn(blueprint);
        when(blueprintService.getBlueprintVariant(any())).thenReturn("AMBARI");
        when(blueprint.getBlueprintText()).thenReturn(TEST_BLUEPRINT_TEXT);
        when(stackInfoService.blueprintStackInfo(TEST_BLUEPRINT_TEXT)).thenReturn(blueprintStackInfo);
        when(userService.getOrCreate(eq(cloudbreakUser))).thenReturn(user);
        when(cloudbreakUser.getEmail()).thenReturn("test@hortonworks.com");
        when(workspaceService.get(anyLong(), eq(user))).thenReturn(workspace);
        when(credentialConverter.convert(credentialResponse)).thenReturn(credential);
        when(environmentResponse.getCredential()).thenReturn(credentialResponse);
        when(environmentResponse.getAdminGroupName()).thenReturn(ADMIN_GROUP_NAME);
        when(environmentResponse.getIdBrokerMappingSource()).thenReturn(IdBrokerMappingSource.MOCK);
        when(environmentResponse.getCrn()).thenReturn(TEST_ENVIRONMENT_CRN);
        when(environmentResponse.getName()).thenReturn("testEnvironment");
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(credentialClientService.getByName(TEST_CREDENTIAL_NAME)).thenReturn(credential);
        when(credentialClientService.getByCrn(TEST_CREDENTIAL_NAME)).thenReturn(credential);
        when(credential.getName()).thenReturn(TEST_CREDENTIAL_NAME);
        when(placementSettings.getRegion()).thenReturn(REGION);
        when(placementSettings.getAvailabilityZone()).thenReturn(AVAILABILITY_ZONE);
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(awsMockAccountMappingService.getGroupMappings(REGION, cloudCredential, ADMIN_GROUP_NAME)).thenReturn(MOCK_GROUP_MAPPINGS);
        when(awsMockAccountMappingService.getUserMappings(REGION, cloudCredential)).thenReturn(MOCK_USER_MAPPINGS);
        when(exposedServiceCollector.getAllKnoxExposed()).thenReturn(Set.of());
    }

    @Test
    public void testConvertWhenGatewayExistsInStack() {
        when(conversionService.convert(source, Gateway.class)).thenReturn(new Gateway());
        when(cluster.getGateway()).thenReturn(new GatewayV4Request());

        TemplatePreparationObject result = underTest.convert(source);

        assertNotNull(result.getGatewayView());
        verify(conversionService, times(1)).convert(source, Gateway.class);
    }

    @Test
    public void testConvertWhenClusterHasEmptyRdsConfigNamesThenEmptyRdsConfigShouldBeStored() {
        when(cluster.getDatabases()).thenReturn(Collections.emptySet());

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getRdsConfigs().isEmpty());
    }

    @Test
    public void testConvertWhenClusterHasSomeRdsConfigNamesThenTheSameAmountOfRdsConfigShouldBeStored() {
        Set<String> rdsConfigNames = createRdsConfigNames();
        when(cluster.getDatabases()).thenReturn(rdsConfigNames);
        int i = 0;
        for (String rdsConfigName : rdsConfigNames) {
            RDSConfig rdsConfig = rdsConfig(DatabaseType.values()[i++]);
            when(rdsConfigService.getByNameForWorkspace(rdsConfigName, workspace)).thenReturn(rdsConfig);
        }
        TemplatePreparationObject result = underTest.convert(source);
        assertEquals(rdsConfigNames.size(), result.getRdsConfigs().size());
    }

    @Test
    public void testConvertWhenStackHasNoInstanceGroupThenTheHostGroupViewParameterShouldContainOnlyAnEmptySet() {
        when(source.getInstanceGroups()).thenReturn(Collections.emptyList());

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getHostgroupViews().isEmpty());
    }

    @Test
    public void testConvertWhenStackContainsSomeInstanceGroupThenTheSameAmountOfHostGroupViewShouldBeStored() {
        List<InstanceGroupV4Request> instanceGroups = createInstanceGroups();
        when(source.getInstanceGroups()).thenReturn(instanceGroups);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(instanceGroups.size(), result.getHostgroupViews().size());
    }

    @Test
    public void testConvertWhenProvidingDataThenBlueprintWithExpectedDataShouldBeStored() {
        String stackVersion = TEST_VERSION;
        String stackType = "HDP";
        when(blueprintStackInfo.getVersion()).thenReturn(stackVersion);
        when(blueprintStackInfo.getType()).thenReturn(stackType);
        BlueprintView expected = mock(BlueprintView.class);
        when(blueprintViewProvider.getBlueprintView(blueprint)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertSame(expected, result.getBlueprintView());
    }

    @Test
    public void testConvertWhenClusterHasNoCloudStorageThenFileSystemConfigurationViewShouldBeEmpty() {
        when(cluster.getCloudStorage()).thenReturn(null);
        when(cloudStorageValidationUtil.isCloudStorageConfigured(null)).thenReturn(false);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getFileSystemConfigurationView().isPresent());
    }

    @Test
    public void testConvertWhenClusterHasCloudStorageThenConvertedFileSystemShouldBeStoredComingFromFileSystemConfigurationProvider() throws IOException {
        BaseFileSystemConfigurationsView expected = mock(BaseFileSystemConfigurationsView.class);
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        FileSystem fileSystem = new FileSystem();
        ConfigQueryEntries configQueryEntries = new ConfigQueryEntries();
        when(cloudStorageValidationUtil.isCloudStorageConfigured(cloudStorageRequest)).thenReturn(true);
        when(cluster.getCloudStorage()).thenReturn(cloudStorageRequest);
        when(cloudStorageConverter.requestToFileSystem(cloudStorageRequest)).thenReturn(fileSystem);
        when(cmCloudStorageConfigProvider.getConfigQueryEntries()).thenReturn(configQueryEntries);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source, credential.getAttributes(),
                configQueryEntries)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getFileSystemConfigurationView().isPresent());
        assertEquals(expected, result.getFileSystemConfigurationView().get());
    }

    @Test
    public void testConvertWhenObtainingGeneralClusterConfigsFromGeneralClusterConfigsProviderThenItsReturnValueShouldBeStored() {
        GeneralClusterConfigs expected = new GeneralClusterConfigs();
        when(generalClusterConfigsProvider.generalClusterConfigs(eq(source), anyString(), anyString())).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(expected, result.getGeneralClusterConfigs());
    }

    @Test
    public void testConvertCloudPlatformMatches() {
        TemplatePreparationObject result = underTest.convert(source);
        assertEquals(CloudPlatform.AWS, result.getCloudPlatform());
    }

    @Test
    public void testConvertWhenCmAndProductDetailsPresent() {
        final String cmBaseUrl = "http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/1677091/cm7/7.0.2/redhat7/yum/";
        final String cmGpgUrl = "http://cloudera-build-us-west-1.vpc.cloudera.com/s3/build/1677091/cm7/7.0.2/redhat7/yum/RPM-GPG-KEY-cloudera";
        final String cmVersion = "7.0.2";
        ClouderaManagerRepositoryV4Request cmRepo = new ClouderaManagerRepositoryV4Request()
                .withBaseUrl(cmBaseUrl)
                .withGpgKeyUrl(cmGpgUrl)
                .withVersion(cmVersion);

        final String smmName = "STREAMS_MESSAGING_MANAGER";
        final String smmVersion = "2.1.0.3.0.0.0-97";
        final String smmParcel = "http://s3.amazonaws.com/dev.hortonworks.com/CSP/centos7/3.x/BUILDS/3.0.0.0-97/tars/parcel/";
        final String smmCsd = "http://s3.amazonaws.com/dev.hortonworks.com/CSP/centos7/3.x/BUILDS/3.0.0.0-97/tars/parcel/STREAMS_MESSAGING_MANAGER-2.1.0.jar";
        ClouderaManagerProductV4Request smm = new ClouderaManagerProductV4Request()
                .withName(smmName)
                .withVersion(smmVersion)
                .withParcel(smmParcel)
                .withCsd(List.of(smmCsd));

        ClouderaManagerV4Request cm = new ClouderaManagerV4Request()
                .withEnableAutoTls(true)
                .withRepository(cmRepo)
                .withProducts(List.of(smm));

        when(cluster.getCm()).thenReturn(cm);
        TemplatePreparationObject result = underTest.convert(source);

        ProductDetailsView products = result.getProductDetailsView();
        assertNotNull(products);
        assertEquals(cmBaseUrl, products.getCm().getBaseUrl());
        assertEquals(cmVersion, products.getCm().getVersion());
        assertEquals(cmGpgUrl, products.getCm().getGpgKeyUrl());
        assertEquals(1, products.getProducts().size());
        ClouderaManagerProduct smmResult = products.getProducts().get(0);
        assertEquals(smmName, smmResult.getName());
        assertEquals(smmVersion, smmResult.getVersion());
        assertEquals(smmParcel, smmResult.getParcel());
        assertEquals(List.of(smmCsd), smmResult.getCsd());
    }

    @Test
    public void testMockAccountMappings() {
        TemplatePreparationObject result = underTest.convert(source);

        AccountMappingView accountMappingView = result.getAccountMappingView();
        assertNotNull(accountMappingView);
        assertEquals(MOCK_GROUP_MAPPINGS, accountMappingView.getGroupMappings());
        assertEquals(MOCK_USER_MAPPINGS, accountMappingView.getUserMappings());
    }

    @Test
    public void testStackInputAccountMappings() {
        when(cloudStorageValidationUtil.isCloudStorageConfigured(any(CloudStorageRequest.class))).thenReturn(true);
        CloudStorageRequest cloudStorage = mock(CloudStorageRequest.class);
        when(cluster.getCloudStorage()).thenReturn(cloudStorage);
        AccountMappingBase accountMapping = new AccountMappingBase();
        accountMapping.setGroupMappings(GROUP_MAPPINGS);
        accountMapping.setUserMappings(USER_MAPPINGS);
        when(cloudStorage.getAccountMapping()).thenReturn(accountMapping);

        TemplatePreparationObject result = underTest.convert(source);

        AccountMappingView accountMappingView = result.getAccountMappingView();
        assertNotNull(accountMappingView);
        assertEquals(GROUP_MAPPINGS, accountMappingView.getGroupMappings());
        assertEquals(USER_MAPPINGS, accountMappingView.getUserMappings());
    }

    @Test
    public void testStackPlacement() {
        TemplatePreparationObject result = underTest.convert(source);
        assertTrue(result.getPlacementView().isPresent());
        assertEquals(REGION, result.getPlacementView().get().getRegion());
        assertEquals(AVAILABILITY_ZONE, result.getPlacementView().get().getAvailabilityZone());
    }

    private Set<String> createRdsConfigNames() {
        Set<String> rdsConfigNames = new LinkedHashSet<>(GENERAL_TEST_QUANTITY);
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            rdsConfigNames.add(String.format("rds-%d", i));
        }
        return rdsConfigNames;
    }

    private List<InstanceGroupV4Request> createInstanceGroups() {
        List<InstanceGroupV4Request> instanceGroups = new ArrayList<>(GENERAL_TEST_QUANTITY);
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
            InstanceTemplateV4Request template = new InstanceTemplateV4Request();
            template.setAttachedVolumes(Collections.EMPTY_SET);
            instanceGroup.setName(String.format("group-%d", i));
            instanceGroup.setTemplate(template);
            instanceGroup.setType(InstanceGroupType.CORE);
            instanceGroup.setNodeCount(i);
            instanceGroups.add(instanceGroup);
        }
        return instanceGroups;
    }

}
