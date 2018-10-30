package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.CloudStorageRequest;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.InstanceGroupV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.sharedservice.SharedServiceConfigsViewProvider;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.filesystem.FileSystemConfigService;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.VaultService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationsViewProvider;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

public class StackRequestToTemplatePreparationObjectConverterTest {

    private static final Long BLUEPRINT_ID = 1L;

    private static final String TEST_CREDENTIAL_NAME = "testCred";

    private static final String TEST_BLUEPRINT_TEXT = "{}";

    private static final int GENERAL_TEST_QUANTITY = 2;

    private static final String TEST_VERSION = "2.6";

    @InjectMocks
    private StackRequestToTemplatePreparationObjectConverter underTest;

    @Mock
    private FlexSubscriptionService flexSubscriptionService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private StackService stackService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Mock
    private StackInfoService stackInfoService;

    @Mock
    private SharedServiceConfigsViewProvider sharedServiceConfigsViewProvider;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private FileSystemConfigService fileSystemConfigService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private FileSystemConfigurationsViewProvider fileSystemConfigurationsViewProvider;

    @Mock
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackV2Request source;

    @Mock
    private GeneralSettings generalSettings;

    @Mock
    private UserService userService;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private ClusterV2Request cluster;

    @Mock
    private AmbariV2Request ambari;

    @Mock
    private Blueprint blueprint;

    @Mock
    private BlueprintStackInfo blueprintStackInfo;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private VaultService vaultService;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(source.getGeneral()).thenReturn(generalSettings);
        when(generalSettings.getCredentialName()).thenReturn(TEST_CREDENTIAL_NAME);
        when(source.getCluster()).thenReturn(cluster);
        when(cluster.getAmbari()).thenReturn(ambari);
        when(ambari.getBlueprintId()).thenReturn(BLUEPRINT_ID);
        when(blueprintService.get(BLUEPRINT_ID)).thenReturn(blueprint);
        when(blueprint.getBlueprintText()).thenReturn(TEST_BLUEPRINT_TEXT);
        when(stackInfoService.blueprintStackInfo(TEST_BLUEPRINT_TEXT)).thenReturn(blueprintStackInfo);
        when(userService.getOrCreate(eq(cloudbreakUser))).thenReturn(user);
        when(cloudbreakUser.getUsername()).thenReturn("test@hortonworks.com");
        when(workspaceService.get(anyLong(), eq(user))).thenReturn(workspace);
    }

    @Test
    public void testConvertWhenFlexSubscriptionExistsThenItShouldbeStored() {
        Long flexId = 2L;
        FlexSubscription expected = new FlexSubscription();
        when(source.getFlexId()).thenReturn(flexId);
        when(flexSubscriptionService.get(flexId)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getFlexSubscription().isPresent());
        assertEquals(expected, result.getFlexSubscription().get());
    }

    @Test
    public void testConvertWhenFlexSubscriptioDoesNotExistsThenEmptyOptionalShouldBeStored() {
        when(source.getFlexId()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getFlexSubscription().isPresent());
    }

    @Test
    public void testConvertWhenFlexSubscriptionExistsThenItsSubscriptionIdShouldBeStoredAsSmartsenseSubscriptionId() {
        Long flexId = 2L;
        FlexSubscription flexSubscription = new FlexSubscription();
        SmartSenseSubscription expected = new SmartSenseSubscription();
        flexSubscription.setSmartSenseSubscription(expected);
        expected.setSubscriptionId(String.valueOf(flexId));
        when(source.getFlexId()).thenReturn(flexId);
        when(flexSubscriptionService.get(flexId)).thenReturn(flexSubscription);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getSmartSenseSubscription().isPresent());
        assertEquals(String.valueOf(flexId), result.getSmartSenseSubscription().get().getSubscriptionId());
    }

    @Test
    public void testConvertWhenFlexSubscriptionDoesNotExistsThenEmptyOptionalShouldBeStoredAsSmartsenseSubscriptionId() {
        when(source.getFlexId()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getSmartSenseSubscription().isPresent());
    }

    @Test
    public void testConvertWhenKerberosRequestIsNullInAmbariV2RequestThenEmptyKerberosShouldBeStored() {
        when(ambari.getKerberos()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getKerberosConfig().isPresent());
        verify(conversionService, times(0)).convert(any(KerberosRequest.class), eq(KerberosConfig.class));
    }

    @Test
    public void testConvertWhenKerberosRequestIsNotNullInAmbariV2RequestButSecurityFalseThenEmptyKerberosShouldBeStored() {
        when(ambari.getKerberos()).thenReturn(new KerberosRequest());
        when(ambari.getEnableSecurity()).thenReturn(false);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getKerberosConfig().isPresent());
        verify(conversionService, times(0)).convert(any(KerberosRequest.class), eq(KerberosConfig.class));
    }

    @Test
    public void testConvertWhenKerberosRequestIsNotNullInAmbariV2RequestAndSecurityTrueThenExpectedKerberosConfigShouldBeStored() {
        KerberosConfig expected = new KerberosConfig();
        KerberosRequest kerberosRequest = new KerberosRequest();
        when(ambari.getKerberos()).thenReturn(kerberosRequest);
        when(ambari.getEnableSecurity()).thenReturn(true);
        when(conversionService.convert(kerberosRequest, KerberosConfig.class)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getKerberosConfig().isPresent());
        assertEquals(expected, result.getKerberosConfig().get());
        verify(conversionService, times(1)).convert(kerberosRequest, KerberosConfig.class);
        verify(conversionService, times(1)).convert(any(KerberosRequest.class), eq(KerberosConfig.class));
    }

    @Test
    public void testConvertWhenGatewayComingFromStackV2RequestConversion() {
        when(conversionService.convert(source, Gateway.class)).thenReturn(new Gateway());
        when(ambari.getGateway()).thenReturn(new GatewayJson());


        TemplatePreparationObject result = underTest.convert(source);

        assertNotNull(result.getGatewayView());
        verify(conversionService, times(1)).convert(source, Gateway.class);
    }

    @Test
    public void testConvertWhenClusterHasEmptyRdsConfigNamesThenEmptyRdsConfigShouldBeStored() {
        when(cluster.getRdsConfigNames()).thenReturn(Collections.emptySet());

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getRdsConfigs().isEmpty());
    }

    @Test
    public void testConvertWhenClusterHaveSomeRdsConfigNamesThenTheSameAmountOfRdsConfigShouldBeStored() {
        Set<String> rdsConfigNames = createRdsConfigNames();
        when(cluster.getRdsConfigNames()).thenReturn(rdsConfigNames);
        long id = 0;
        for (String rdsConfigName : rdsConfigNames) {
            RDSConfig rdsConfig = new RDSConfig();
            rdsConfig.setId(id++);
            when(rdsConfigService.getByNameForWorkspace(rdsConfigName, workspace)).thenReturn(rdsConfig);
        }
        TemplatePreparationObject result = underTest.convert(source);
        assertEquals(rdsConfigNames.size(), result.getRdsConfigs().size());
    }

    @Test
    public void testConvertWhenStackV2RequestHasNoInstanceGroupV2RequestThenTheHostGroupViewParameterShouldContainsOnlyAnEmptySet() {
        when(source.getInstanceGroups()).thenReturn(Collections.emptyList());

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getHostgroupViews().isEmpty());
    }

    @Test
    public void testConvertWhenStackV2RequestContainsSomeInstanceGroupV2RequestThenTheSameAmountOfHostGroupViewShouldBeStored() {
        List<InstanceGroupV2Request> instanceGroupV2Requests = createInstanceGroupV2Requests();
        when(source.getInstanceGroups()).thenReturn(instanceGroupV2Requests);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(instanceGroupV2Requests.size(), result.getHostgroupViews().size());
    }

    @Test
    public void testConvertWhenProvidingDataThenBlueprintWithExpectedDataShouldBeStored() {
        String stackVersion = TEST_VERSION;
        String stackType = "HDP";
        when(blueprintStackInfo.getVersion()).thenReturn(stackVersion);
        when(blueprintStackInfo.getType()).thenReturn(stackType);
        BlueprintView expected = new BlueprintView(TEST_BLUEPRINT_TEXT, stackVersion, stackType);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(expected, result.getBlueprintView());
    }

    @Test
    public void testConvertWhenObtainingBlueprintStackInfoThenItsVersionShouldBeStoredAsStackRepoDetailsHdpVersion() {
        String expected = TEST_VERSION;
        when(stackInfoService.blueprintStackInfo(TEST_BLUEPRINT_TEXT)).thenReturn(blueprintStackInfo);
        when(blueprintStackInfo.getVersion()).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getStackRepoDetailsHdpVersion().isPresent());
        assertEquals(expected, result.getStackRepoDetailsHdpVersion().get());
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
        Credential credential = new Credential();
        String account = "testAccount";
        when(cloudStorageValidationUtil.isCloudStorageConfigured(cloudStorageRequest)).thenReturn(true);
        when(cloudbreakUser.getAccount()).thenReturn(account);
        when(credentialService.getByNameForWorkspace(TEST_CREDENTIAL_NAME, workspace)).thenReturn(credential);
        when(cluster.getCloudStorage()).thenReturn(cloudStorageRequest);
        when(conversionService.convert(cloudStorageRequest, FileSystem.class)).thenReturn(fileSystem);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(fileSystem, source, credential)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getFileSystemConfigurationView().isPresent());
        assertEquals(expected, result.getFileSystemConfigurationView().get());
    }

    @Test
    public void testConvertWhenObtainingGeneralClusterConfigsFromGeneralClusterConfigsProviderThenItsReturnValueShouldBeStored() {
        GeneralClusterConfigs expected = new GeneralClusterConfigs();
        when(generalClusterConfigsProvider.generalClusterConfigs(eq(source), eq(user), anyString())).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(expected, result.getGeneralClusterConfigs());
    }

    @Test
    public void testConvertWhenLdapConfigNameIsNullThenStoredLdapConfigShouldBeEmpty() {
        when(cluster.getLdapConfigName()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getLdapConfig().isPresent());
    }

    @Test
    public void testConvertWhenLdapConfigNameIsNotNullThenPublicConfigFromLdapConfigServiceShouldBeStored() {
        LdapConfig expected = new LdapConfig();
        expected.setProtocol("");
        String ldapConfigName = "configName";
        when(cluster.getLdapConfigName()).thenReturn(ldapConfigName);
        when(ldapConfigService.getByNameForWorkspace(eq(ldapConfigName), eq(workspace))).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getLdapConfig().isPresent());
    }

    private Set<String> createRdsConfigNames() {
        Set<String> rdsConfigNames = new LinkedHashSet<>(GENERAL_TEST_QUANTITY);
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            rdsConfigNames.add(String.format("rds-%d", i));
        }
        return rdsConfigNames;
    }

    private List<InstanceGroupV2Request> createInstanceGroupV2Requests() {
        List<InstanceGroupV2Request> requests = new ArrayList<>(GENERAL_TEST_QUANTITY);
        for (int i = 0; i < GENERAL_TEST_QUANTITY; i++) {
            InstanceGroupV2Request request = new InstanceGroupV2Request();
            TemplateV2Request templateV2Request = new TemplateV2Request();
            templateV2Request.setVolumeCount(i);
            request.setGroup(String.format("group-%d", i));
            request.setTemplate(templateV2Request);
            request.setType(InstanceGroupType.CORE);
            request.setNodeCount(i);
            requests.add(request);
        }
        return requests;
    }

}