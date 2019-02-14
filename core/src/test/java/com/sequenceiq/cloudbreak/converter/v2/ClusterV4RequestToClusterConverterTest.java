package com.sequenceiq.cloudbreak.converter.v2;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterV4RequestToClusterConverterTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private ClusterV4RequestToClusterConverter underTest;

    @Mock
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private ProxyConfigService proxyConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private ClusterDefinitionService clusterDefinitionService;

    @Mock
    private GatewayConvertUtil gatewayConvertUtil;

    @Mock
    private ConversionService conversionService;

    private Workspace workspace;

    @Before
    public void before() {

        workspace = new Workspace();
        workspace.setId(100L);
        workspace.setName("TEST_WS_NAME");
        workspace.setDescription("TEST_WS_DESC");

        when(workspaceService.getForCurrentUser()).thenReturn(workspace);

        when(cloudStorageValidationUtil.isCloudStorageConfigured(any(CloudStorageV4Request.class))).thenReturn(false);
    }

    @Test
    public void testConvertWhenCloudStorageConfiguredAndRdsAndLdapAndProxyExistsAnd() {
        CloudStorageV4Request cloudStorageRequest = mock(CloudStorageV4Request.class);

        String rdsConfigName = "rds-name";
        String proxyName = "proxy-name";
        String ldapName = "ldap-name";

        FileSystem fileSystem = new FileSystem();

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(rdsConfigName);

        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setName(proxyName);

        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setName(ldapName);

        ClusterV4Request source = new ClusterV4Request();
        source.setCloudStorage(cloudStorageRequest);
        source.setDatabases(singleton(rdsConfigName));
        source.setProxyName(proxyName);
        source.setLdapName(ldapName);
        source.setAmbari(new AmbariV4Request());

        when(cloudStorageValidationUtil.isCloudStorageConfigured(cloudStorageRequest)).thenReturn(true);
        when(conversionService.convert(cloudStorageRequest, FileSystem.class)).thenReturn(fileSystem);
        when(rdsConfigService.findByNamesInWorkspace(singleton(rdsConfigName), workspace.getId())).thenReturn(singleton(rdsConfig));
        when(proxyConfigService.getByNameForWorkspace(proxyName, workspace)).thenReturn(proxyConfig);
        when(ldapConfigService.getByNameForWorkspace(ldapName, workspace)).thenReturn(ldapConfig);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getFileSystem(), is(fileSystem));
        assertThat(actual.getName(), is(source.getName()));
        assertThat(actual.getRdsConfigs().size(), is(1));
        assertThat(actual.getRdsConfigs().stream().findFirst().get().getName(), is(rdsConfigName));
        assertThat(actual.getProxyConfig().getName(), is(proxyName));
        assertThat(actual.getLdapConfig().getName(), is(ldapName));

        verify(cloudStorageValidationUtil, times(1)).isCloudStorageConfigured(cloudStorageRequest);
        verify(conversionService, times(1)).convert(cloudStorageRequest, FileSystem.class);
        verify(rdsConfigService, times(1)).findByNamesInWorkspace(singleton(rdsConfigName), workspace.getId());
        verify(proxyConfigService, times(1)).getByNameForWorkspace(proxyName, workspace);
        verify(ldapConfigService, times(1)).getByNameForWorkspace(ldapName, workspace);
    }

    @Test
    public void testConvertWhenNoRdsConfig() {
        ClusterV4Request source = new ClusterV4Request();
        source.setAmbari(new AmbariV4Request());

        Set<String> rdsConfigNames = emptySet();

        Cluster actual = underTest.convert(source);

        assertThat(actual.getRdsConfigs(), is(nullValue()));

        verify(rdsConfigService, times(0)).findByNamesInWorkspace(rdsConfigNames, workspace.getId());
    }

    @Test
    public void testConvertWhenRdsConfigNotExists() {
        ClusterV4Request source = new ClusterV4Request();
        source.setAmbari(new AmbariV4Request());

        Set<String> rdsConfigNames = singleton("fake-rds-name");
        when(rdsConfigService.findByNamesInWorkspace(rdsConfigNames, workspace.getId())).thenReturn(emptySet());

        source.setDatabases(rdsConfigNames);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("RDS config names dont exists");

        underTest.convert(source);

        verify(rdsConfigService, times(1)).findByNamesInWorkspace(rdsConfigNames, workspace.getId());
    }

    @Test
    public void testConvertWhenNoBlueprint() {
        ClusterV4Request source = new ClusterV4Request();

        AmbariV4Request ambariV4Request = new AmbariV4Request();
        source.setAmbari(ambariV4Request);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getClusterDefinition(), is(nullValue()));

        verify(clusterDefinitionService, times(0)).getByNameForWorkspace(anyString(), eq(workspace));
    }

    @Test
    public void testConvertWheBlueprintDoesNotExists() {
        String blueprintName = "bp-name";

        ClusterV4Request source = new ClusterV4Request();

        AmbariV4Request ambariV4Request = new AmbariV4Request();
        ambariV4Request.setBlueprintName(blueprintName);
        source.setAmbari(ambariV4Request);

        when(clusterDefinitionService.getByNameForWorkspace(blueprintName, workspace)).thenReturn(null);

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("Blueprint does not exists by name: bp-name");

        underTest.convert(source);
    }

    @Test
    public void testConvertWheBlueprintExists() {
        String blueprintName = "bp-name";

        ClusterDefinition clusterDefinition = new ClusterDefinition();
        clusterDefinition.setName(blueprintName);

        ClusterV4Request source = new ClusterV4Request();

        AmbariV4Request ambariV4Request = new AmbariV4Request();
        ambariV4Request.setBlueprintName(blueprintName);
        source.setAmbari(ambariV4Request);

        when(clusterDefinitionService.getByNameForWorkspace(blueprintName, workspace)).thenReturn(clusterDefinition);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getClusterDefinition(), is(clusterDefinition));

        verify(clusterDefinitionService, times(1)).getByNameForWorkspace(blueprintName, workspace);
    }

    @Test
    public void testConvertWhenAmbariRepoDetailsNotNull() throws IOException {
        String baseUrl = "base-url";

        ClusterV4Request source = new ClusterV4Request();
        AmbariRepositoryV4Request ambariRepoDetailsJson = new AmbariRepositoryV4Request();
        AmbariRepo ambariRepo = new AmbariRepo();
        ambariRepo.setBaseUrl(baseUrl);

        AmbariV4Request ambariV4Request = new AmbariV4Request();
        ambariV4Request.setRepository(ambariRepoDetailsJson);
        source.setAmbari(ambariV4Request);

        when(conversionService.convert(ambariRepoDetailsJson, AmbariRepo.class)).thenReturn(ambariRepo);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getComponents().size(), is(1));
        ClusterComponent ambariRepoComponent = actual.getComponents().iterator().next();
        assertThat(ambariRepoComponent.getComponentType(), is(ComponentType.AMBARI_REPO_DETAILS));
        assertThat(ambariRepoComponent.getCluster(), is(actual));
        assertThat(ambariRepoComponent.getAttributes().get(AmbariRepo.class).getBaseUrl(), is(baseUrl));

        verify(conversionService, times(1)).convert(ambariRepoDetailsJson, AmbariRepo.class);
    }

    @Test
    public void testConvertWhenAmbariStackDetailsNotNull() throws IOException {
        String version = "2.6";

        ClusterV4Request source = new ClusterV4Request();
        StackRepositoryV4Request ambariStackDetailsJson = new StackRepositoryV4Request();
        StackRepoDetails stackRepoDetails = new StackRepoDetails();
        stackRepoDetails.setHdpVersion(version);

        AmbariV4Request ambariV4Request = new AmbariV4Request();
        ambariV4Request.setStackRepository(ambariStackDetailsJson);
        source.setAmbari(ambariV4Request);

        when(conversionService.convert(ambariStackDetailsJson, StackRepoDetails.class)).thenReturn(stackRepoDetails);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getComponents().size(), is(1));
        ClusterComponent stackRepoDetailsComponent = actual.getComponents().iterator().next();
        assertThat(stackRepoDetailsComponent.getComponentType(), is(ComponentType.HDP_REPO_DETAILS));
        assertThat(stackRepoDetailsComponent.getCluster(), is(actual));
        assertThat(stackRepoDetailsComponent.getAttributes().get(StackRepoDetails.class).getHdpVersion(), is(version));

        verify(conversionService, times(1)).convert(ambariStackDetailsJson, StackRepoDetails.class);
    }

    @Test
    public void testConvertWhenGatewayExists() {
        String clusterName = "cluster-name";

        ClusterV4Request source = new ClusterV4Request();

        AmbariV4Request ambariV4Request = new AmbariV4Request();
        GatewayV4Request gatewayJson = new GatewayV4Request();
        source.setGateway(gatewayJson);
        source.setAmbari(ambariV4Request);
        Gateway gateway = new Gateway();

        when(conversionService.convert(gatewayJson, Gateway.class)).thenReturn(gateway);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getGateway(), is(gateway));

        verify(conversionService, times(1)).convert(gatewayJson, Gateway.class);
    }
}
