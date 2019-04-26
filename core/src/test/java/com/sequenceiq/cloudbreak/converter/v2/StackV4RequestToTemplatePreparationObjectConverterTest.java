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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.blueprint.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackV4RequestToTemplatePreparationObjectConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintTextProcessorFactory;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

public class StackV4RequestToTemplatePreparationObjectConverterTest {

    private static final String TEST_CREDENTIAL_NAME = "testCred";

    private static final String TEST_BLUEPRINT_NAME = "testBp";

    private static final String TEST_BLUEPRINT_TEXT = "{}";

    private static final int GENERAL_TEST_QUANTITY = 2;

    private static final String TEST_VERSION = "2.6";

    private static final String TEST_KERBEROS_NAME = "somename";

    @InjectMocks
    private StackV4RequestToTemplatePreparationObjectConverter underTest;

    @Mock
    private LdapConfigService ldapConfigService;

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
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackV4Request source;

    @Mock
    private UserService userService;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private User user;

    @Mock
    private Workspace workspace;

    @Mock
    private EnvironmentSettingsV4Request environment;

    @Mock
    private Credential credential;

    @Mock
    private ClusterV4Request cluster;

    @Mock
    private AmbariV4Request ambari;

    @Mock
    private Blueprint blueprint;

    @Mock
    private BlueprintStackInfo blueprintStackInfo;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private BlueprintTextProcessorFactory blueprintTextProcessorFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(source.getEnvironment()).thenReturn(environment);
        when(environment.getCredentialName()).thenReturn(TEST_CREDENTIAL_NAME);
        when(source.getCluster()).thenReturn(cluster);
        when(cluster.getAmbari()).thenReturn(ambari);
        when(cluster.getBlueprintName()).thenReturn(TEST_BLUEPRINT_NAME);
        when(blueprintService.getByNameForWorkspace(TEST_BLUEPRINT_NAME, workspace)).thenReturn(blueprint);
        when(blueprintService.getBlueprintVariant(any())).thenReturn("AMBARI");
        when(blueprint.getBlueprintText()).thenReturn(TEST_BLUEPRINT_TEXT);
        when(stackInfoService.blueprintStackInfo(TEST_BLUEPRINT_TEXT)).thenReturn(blueprintStackInfo);
        when(userService.getOrCreate(eq(cloudbreakUser))).thenReturn(user);
        when(cloudbreakUser.getEmail()).thenReturn("test@hortonworks.com");
        when(workspaceService.get(anyLong(), eq(user))).thenReturn(workspace);
        when(credentialService.getByNameForWorkspace(TEST_CREDENTIAL_NAME, workspace)).thenReturn(credential);
        when(blueprintTextProcessorFactory.createBlueprintTextProcessor(anyString())).thenReturn(new AmbariBlueprintTextProcessor(""));
    }

    @Test
    public void testConvertWhenKerberosNameIsNullInAmbariThenEmptyKerberosShouldBeStored() {
        when(cluster.getKerberosName()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertNotNull(result);
        assertFalse(result.getKerberosConfig().isPresent());
    }

    @Test
    public void testConvertWhenKerberosNameIsNotNullInAmbariButSecurityFalseThenEmptyKerberosShouldBeStored() {
        when(cluster.getKerberosName()).thenReturn(TEST_KERBEROS_NAME);

        TemplatePreparationObject result = underTest.convert(source);

        assertNotNull(result);
        assertFalse(result.getKerberosConfig().isPresent());
    }

    @Test
    public void testConvertWhenKerberosNameIsNotNullInAmbariAndSecurityTrueThenExpectedKerberosConfigShouldBeStored() {
        KerberosConfig expected = new KerberosConfig();
        when(cluster.getKerberosName()).thenReturn(TEST_KERBEROS_NAME);
        when(kerberosConfigService.getByNameForWorkspaceId(eq(TEST_KERBEROS_NAME), anyLong())).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertNotNull(result);
        assertTrue(result.getKerberosConfig().isPresent());
        assertEquals(expected, result.getKerberosConfig().get());
        verify(kerberosConfigService, times(1)).getByNameForWorkspaceId(anyString(), anyLong());
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
        BlueprintView expected = new BlueprintView(TEST_BLUEPRINT_TEXT, stackVersion, stackType,
                new AmbariBlueprintTextProcessor(TEST_BLUEPRINT_TEXT));

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
        CloudStorageV4Request cloudStorageRequest = new CloudStorageV4Request();
        FileSystem fileSystem = new FileSystem();
        String account = "testAccount";
        when(cloudStorageValidationUtil.isCloudStorageConfigured(cloudStorageRequest)).thenReturn(true);
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
        when(generalClusterConfigsProvider.generalClusterConfigs(eq(source), anyString(), anyString())).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(expected, result.getGeneralClusterConfigs());
    }

    @Test
    public void testConvertWhenLdapConfigNameIsNullThenStoredLdapConfigShouldBeEmpty() {
        when(cluster.getLdapName()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getLdapConfig().isPresent());
    }

    @Test
    public void testConvertWhenLdapConfigNameIsNotNullThenPublicConfigFromLdapConfigServiceShouldBeStored() {
        LdapConfig expected = new LdapConfig();
        expected.setProtocol("");
        expected.setBindDn("");
        expected.setBindPassword("");
        String ldapConfigName = "configName";
        when(cluster.getLdapName()).thenReturn(ldapConfigName);
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