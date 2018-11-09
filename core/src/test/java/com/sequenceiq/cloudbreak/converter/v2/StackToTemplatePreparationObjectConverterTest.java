package com.sequenceiq.cloudbreak.converter.v2;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.blueprint.BlueprintViewProvider;
import com.sequenceiq.cloudbreak.blueprint.GeneralClusterConfigsProvider;
import com.sequenceiq.cloudbreak.blueprint.nifi.HdfConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.sharedservice.SharedServiceConfigsViewProvider;
import com.sequenceiq.cloudbreak.blueprint.utils.StackInfoService;
import com.sequenceiq.cloudbreak.cloud.model.StackInputs;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.core.bootstrap.service.container.postgres.PostgresConfigService;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.Secret;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.ambari.InstanceGroupMetadataCollector;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.CachedUserDetailsService;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.filesystem.BaseFileSystemConfigurationsView;
import com.sequenceiq.cloudbreak.template.filesystem.FileSystemConfigurationProvider;
import com.sequenceiq.cloudbreak.template.model.BlueprintStackInfo;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.model.HdfConfigs;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.SharedServiceConfigsView;

public class StackToTemplatePreparationObjectConverterTest {

    private static final Long TEST_CLUSTER_ID = 1L;

    private static final String CLUSTER_OWNER = "owner";

    private static final String TEST_BLUEPRINT_TEXT = "{}";

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private StackToTemplatePreparationObjectConverter underTest;

    @Mock
    private HostGroupService hostGroupService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private CachedUserDetailsService cachedUserDetailsService;

    @Mock
    private InstanceGroupMetadataCollector instanceGroupMetadataCollector;

    @Mock
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Mock
    private StackInfoService stackInfoService;

    @Mock
    private HdfConfigProvider hdfConfigProvider;

    @Mock
    private PostgresConfigService postgresConfigService;

    @Mock
    private FileSystemConfigurationProvider fileSystemConfigurationProvider;

    @Mock
    private StackService stackService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private GeneralClusterConfigsProvider generalClusterConfigsProvider;

    @Mock
    private SharedServiceConfigsViewProvider sharedServiceConfigProvider;

    @Mock
    private Stack source;

    @Mock
    private Cluster cluster;

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
    private BlueprintStackInfo blueprintStackInfo;

    @Mock
    private BlueprintViewProvider blueprintViewProvider;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        when(clusterService.getById(any(Long.class))).thenReturn(cluster);
        when(source.getCluster()).thenReturn(sourceCluster);
        when(sourceCluster.getId()).thenReturn(TEST_CLUSTER_ID);
        when(cluster.getId()).thenReturn(TEST_CLUSTER_ID);
        when(clusterComponentConfigProvider.getHDPRepo(TEST_CLUSTER_ID)).thenReturn(stackRepoDetails);
        when(instanceGroupMetadataCollector.collectMetadata(source)).thenReturn(groupInstances);
        when(cluster.getBlueprint()).thenReturn(blueprint);
        when(blueprint.getBlueprintText()).thenReturn(new Secret(TEST_BLUEPRINT_TEXT));
        when(blueprint.getOwner()).thenReturn(CLUSTER_OWNER);
        when(cluster.getOwner()).thenReturn(CLUSTER_OWNER);
        when(cachedUserDetailsService.getDetails(CLUSTER_OWNER, UserFilterField.USERID)).thenReturn(user);
        when(source.getInputs()).thenReturn(stackInputs);
        when(stackInputs.get(StackInputs.class)).thenReturn(null);
        when(stackInfoService.blueprintStackInfo(TEST_BLUEPRINT_TEXT)).thenReturn(blueprintStackInfo);
    }

    @Test
    public void testConvertWhenThereIsNoSmartSenseSubscriptionThenNullShouldBePlaced() {
        when(smartSenseSubscriptionService.getDefault()).thenReturn(Optional.empty());

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getSmartSenseSubscription().isPresent());
    }

    @Test
    public void testConvertWhenThereIsASmartSenseSubscriptionThenNullShouldBePlaced() {
        String subscriptionId = "1234567";
        SmartSenseSubscription subscription = new SmartSenseSubscription();
        subscription.setSubscriptionId(subscriptionId);
        when(smartSenseSubscriptionService.getDefault()).thenReturn(Optional.of(subscription));

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getSmartSenseSubscription().isPresent());
        assertEquals(subscriptionId, result.getSmartSenseSubscription().get().getSubscriptionId());
    }

    @Test
    public void testConvertWhenClusterGivesGatewayThenNotNullShouldBeStored() {
        Gateway gateway = mock(Gateway.class);
        when(gateway.getSignKey()).thenReturn(Secret.EMPTY);
        when(cluster.getGateway()).thenReturn(gateway);

        TemplatePreparationObject result = underTest.convert(source);

        assertNotNull(result.getGatewayView());
    }

    @Test
    public void testConvertWhenClusterDoesNotGivesAGatewayThenNullShouldBeStored() {
        when(cluster.getGateway()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertNull(result.getGatewayView());
    }

    @Test
    public void testConvertWhenClusterProvidesFileSystemThenBaseFileSystemConfigurationsViewShouldBeExists() throws IOException {
        FileSystem sourceFileSystem = new FileSystem();
        FileSystem clusterServiceFileSystem = new FileSystem();
        BaseFileSystemConfigurationsView expected = mock(BaseFileSystemConfigurationsView.class);
        when(sourceCluster.getFileSystem()).thenReturn(sourceFileSystem);
        when(cluster.getFileSystem()).thenReturn(clusterServiceFileSystem);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(clusterServiceFileSystem, source)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getFileSystemConfigurationView().isPresent());
        assertEquals(expected, result.getFileSystemConfigurationView().get());
        verify(fileSystemConfigurationProvider, times(1)).fileSystemConfiguration(clusterServiceFileSystem, source);
    }

    @Test
    public void testConvertWhenClusterDoesNotProvidesFileSystemThenBaseFileSystemConfigurationsViewShouldBeEmpty() throws IOException {
        FileSystem clusterServiceFileSystem = new FileSystem();
        BaseFileSystemConfigurationsView expected = mock(BaseFileSystemConfigurationsView.class);
        when(sourceCluster.getFileSystem()).thenReturn(null);
        when(cluster.getFileSystem()).thenReturn(clusterServiceFileSystem);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(clusterServiceFileSystem, source)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getFileSystemConfigurationView().isPresent());
        verify(fileSystemConfigurationProvider, times(0)).fileSystemConfiguration(clusterServiceFileSystem, source);
    }

    @Test
    public void testConvertIfTheAttemptOfObtainingBaseFileSystemConfigurationsViewThrowsIOExceptionThenCloudbreakServiceExceptionShouldComeOutside()
            throws IOException {
        String iOExceptionMessage = "Unable to obtain BaseFileSystemConfigurationsView";
        IOException invokedException = new IOException(iOExceptionMessage);
        FileSystem sourceFileSystem = new FileSystem();
        FileSystem clusterServiceFileSystem = new FileSystem();
        when(sourceCluster.getFileSystem()).thenReturn(sourceFileSystem);
        when(cluster.getFileSystem()).thenReturn(clusterServiceFileSystem);
        when(fileSystemConfigurationProvider.fileSystemConfiguration(clusterServiceFileSystem, source)).thenThrow(invokedException);

        expectedException.expect(CloudbreakServiceException.class);
        expectedException.expectMessage(iOExceptionMessage);
        expectedException.expectCause(is(invokedException));

        underTest.convert(source);
    }

    @Test
    public void testConvertWhenClusterFromClusterServiceHasLdapConfigThenItShouldBeStored() {
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setProtocol("");
        ldapConfig.setBindDn("admin<>");
        when(cluster.getLdapConfig()).thenReturn(ldapConfig);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getLdapConfig().isPresent());
    }

    @Test
    public void testConvertWhenClusterFromClusterServiceHasNoLdapConfigThenTheOptionalShouldBeEmpty() {
        when(cluster.getLdapConfig()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getLdapConfig().isPresent());
    }

    @Test
    public void testConvertWhenHdpRepoNotNullThenItsVersionShouldBeSet() {
        String hdpVersion = "2.6";
        when(clusterComponentConfigProvider.getHDPRepo(TEST_CLUSTER_ID)).thenReturn(stackRepoDetails);
        when(stackRepoDetails.getHdpVersion()).thenReturn(hdpVersion);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getStackRepoDetailsHdpVersion().isPresent());
        assertEquals(hdpVersion, result.getStackRepoDetailsHdpVersion().get());
    }

    @Test
    public void testConvertWhenHdpRepoNullThenEmptyVersionShouldBeSet() {
        when(clusterComponentConfigProvider.getHDPRepo(TEST_CLUSTER_ID)).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getStackRepoDetailsHdpVersion().isPresent());
    }

    @Test
    public void testConvertWhenHdfConfigProviderProvidedThenItShouldBeStored() {
        HdfConfigs expected = mock(HdfConfigs.class);
        Set<HostGroup> hostGroups = new LinkedHashSet<>();
        when(hdfConfigProvider.createHdfConfig(hostGroups, groupInstances, TEST_BLUEPRINT_TEXT)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getHdfConfigs().isPresent());
        assertEquals(expected, result.getHdfConfigs().get());
    }

    @Test
    public void testConvertWhenHdfConfigIsNullThenOptionalShouldBeEmpty() {
        Set<HostGroup> hostGroups = new LinkedHashSet<>();
        when(hdfConfigProvider.createHdfConfig(hostGroups, groupInstances, TEST_BLUEPRINT_TEXT)).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertFalse(result.getHdfConfigs().isPresent());
    }

    @Test
    public void testConvertWhenProvidingStackAndBlueprintStackInfoThenExpectedBlueprintViewShouldBeStored() {
        String type = "HDF";
        String version = "2.6";
        BlueprintView expected = new BlueprintView(blueprintStackInfo, cluster.getBlueprint().getBlueprintText().getRaw());
        when(blueprintStackInfo.getType()).thenReturn(type);
        when(blueprintStackInfo.getVersion()).thenReturn(version);
        when(blueprintViewProvider.getBlueprintView(blueprint)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(expected, result.getBlueprintView());
    }

    @Test
    public void testConvertWhenGeneralClusterConfigsProvidedThenThisShouldBeStored() {
        GeneralClusterConfigs expected = mock(GeneralClusterConfigs.class);
        when(generalClusterConfigsProvider.generalClusterConfigs(source, cluster, user)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(expected, result.getGeneralClusterConfigs());
    }

    @Test
    public void testConvertWhenDataLakeIdNotNullThenExpectedSharedServiceConfigsShouldBeStored() {
        // just in case adding one to avoid matching with the class variable
        Long testDataLakeId = TEST_CLUSTER_ID + 1L;
        Stack dataLakeStack = new Stack();
        SharedServiceConfigsView expected = new SharedServiceConfigsView();
        when(source.getDatalakeId()).thenReturn(testDataLakeId);
        when(stackService.getByIdWithListsInTransaction(testDataLakeId)).thenReturn(dataLakeStack);
        when(sharedServiceConfigProvider.createSharedServiceConfigs(source, dataLakeStack)).thenReturn(expected);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getSharedServiceConfigs().isPresent());
        assertEquals(expected, result.getSharedServiceConfigs().get());
    }

    @Test
    public void testConvertWhenStackInputsContainsCustomInputsThenThisShouldBeStored() throws IOException {
        StackInputs stackInputs = mock(StackInputs.class);
        Map<String, Object> customInputs = new LinkedHashMap<>();
        when(this.stackInputs.get(StackInputs.class)).thenReturn(stackInputs);
        when(stackInputs.getCustomInputs()).thenReturn(customInputs);

        TemplatePreparationObject result = underTest.convert(source);

        assertEquals(customInputs, result.getCustomInputs());
    }

    @Test
    public void testConvertWhenStackInputsDoesNotContainsCustomInputsThenEmptyMapBeStored() throws IOException {
        StackInputs stackInputs = mock(StackInputs.class);
        when(this.stackInputs.get(StackInputs.class)).thenReturn(stackInputs);
        when(stackInputs.getCustomInputs()).thenReturn(null);

        TemplatePreparationObject result = underTest.convert(source);

        assertTrue(result.getCustomInputs().isEmpty());
    }

    @Test
    public void testConvertWhenUnableToGetStackInputsThenCloudbreakServiceExceptionWouldCome() throws IOException {
        String iOexceptionMessage = "unable to get inputs";
        IOException invokedException = new IOException(iOexceptionMessage);
        when(stackInputs.get(StackInputs.class)).thenThrow(invokedException);

        expectedException.expect(CloudbreakServiceException.class);
        expectedException.expectMessage(iOexceptionMessage);
        expectedException.expectCause(is(invokedException));

        underTest.convert(source);
    }

}