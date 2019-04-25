package com.sequenceiq.cloudbreak.converter.v2;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.ambarirepository.AmbariRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.StackRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.CloudStorageV4Request;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@ExtendWith(MockitoExtension.class)
public class ClusterV4RequestToClusterConverterTest {

    private static final String BLUEPRINT = "my-blueprint";

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
    private BlueprintService blueprintService;

    @Mock
    private GatewayConvertUtil gatewayConvertUtil;

    @Mock
    private ConversionService conversionService;

    private Workspace workspace;

    private Blueprint blueprint;

    @BeforeEach
    public void before() {

        blueprint = new Blueprint();
        blueprint.setStackType(StackType.HDP.name());

        workspace = new Workspace();
        workspace.setId(100L);
        workspace.setName("TEST_WS_NAME");
        workspace.setDescription("TEST_WS_DESC");

        when(workspaceService.getForCurrentUser()).thenReturn(workspace);

        when(cloudStorageValidationUtil.isCloudStorageConfigured(nullable(CloudStorageV4Request.class))).thenReturn(false);
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
        source.setBlueprintName(BLUEPRINT);
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);

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
        source.setBlueprintName(BLUEPRINT);
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);
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

        Exception exception = assertThrows(NotFoundException.class, () -> underTest.convert(source));
        assertEquals("RDS config names dont exists", exception.getMessage());

        verify(rdsConfigService, times(1)).findByNamesInWorkspace(rdsConfigNames, workspace.getId());
    }

    @Test
    public void testConvertWheBlueprintDoesNotExists() {
        Mockito.reset(cloudStorageValidationUtil);

        String blueprintName = "bp-name";

        ClusterV4Request source = new ClusterV4Request();
        source.setAmbari(new AmbariV4Request());
        source.setBlueprintName(blueprintName);

        AmbariV4Request ambariV4Request = new AmbariV4Request();
        source.setAmbari(ambariV4Request);

        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace)).thenReturn(null);

        Exception exception = assertThrows(NotFoundException.class, () -> underTest.convert(source));
        assertEquals("Cluster definition does not exists by name: bp-name", exception.getMessage());
    }

    @Test
    public void testConvertWheBlueprintExists() {
        String blueprintName = "bp-name";

        Blueprint blueprint = new Blueprint();
        blueprint.setName(blueprintName);

        ClusterV4Request source = new ClusterV4Request();
        source.setBlueprintName(blueprintName);
        source.setAmbari(new AmbariV4Request());

        AmbariV4Request ambariV4Request = new AmbariV4Request();
        source.setAmbari(ambariV4Request);

        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace)).thenReturn(blueprint);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getBlueprint(), is(blueprint));

        verify(blueprintService, times(1)).getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace);
    }

    @Test
    public void testConvertWhenAmbariRepoDetailsNotNull() throws IOException {
        String baseUrl = "base-url";

        ClusterV4Request source = new ClusterV4Request();
        source.setBlueprintName(BLUEPRINT);
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);
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
        source.setBlueprintName(BLUEPRINT);
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);
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
        source.setBlueprintName(BLUEPRINT);
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);
        Gateway gateway = new Gateway();

        when(conversionService.convert(gatewayJson, Gateway.class)).thenReturn(gateway);
        when(blueprintService.isAmbariBlueprint(any())).thenReturn(Boolean.TRUE);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getGateway(), is(gateway));

        verify(conversionService, times(1)).convert(gatewayJson, Gateway.class);
    }

    @Test
    public void testConvertWhenMultipleClusterManagersProvided() {
        ClusterV4Request request = new ClusterV4Request();
        request.setAmbari(new AmbariV4Request());
        request.setCm(new ClouderaManagerV4Request());

        Exception exception = assertThrows(BadRequestException.class, () -> underTest.convert(request));
        assertEquals("Cannot determine cluster manager. More than one provided", exception.getMessage());
    }

    @Test
    public void testConvertClouderaManagerRequestWithNullProductList() throws JsonProcessingException {
        ClusterV4Request request = new ClusterV4Request();
        request.setBlueprintName(BLUEPRINT);
        blueprint.setStackType(StackType.CDH.name());
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);
        ClouderaManagerV4Request cm = new ClouderaManagerV4Request();

        ClouderaManagerRepositoryV4Request repository = new ClouderaManagerRepositoryV4Request();
        repository.setBaseUrl("base.url");
        repository.setVersion("1.0");
        repository.setGpgKeyUrl("gpg.key.url");
        cm.setRepository(repository);
        request.setCm(cm);

        Cluster cluster = underTest.convert(request);

        assertFalse(cluster.getComponents().isEmpty());
        assertEquals(1, cluster.getComponents().size());

        ClusterComponent component = cluster.getComponents().iterator().next();
        assertEquals(ComponentType.CM_REPO_DETAILS, component.getComponentType());

        Json expectedRepoJson = new Json(repository);
        assertEquals(expectedRepoJson.getMap().size() + 1, component.getAttributes().getMap().size());
    }

    @Test
    public void testConvertClouderaManagerRequestWithNullRepo() throws JsonProcessingException {
        ClusterV4Request request = new ClusterV4Request();
        request.setBlueprintName(BLUEPRINT);
        blueprint.setStackType(StackType.CDH.name());
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);
        ClouderaManagerV4Request cm = new ClouderaManagerV4Request();

        ClouderaManagerProductV4Request cdp = new ClouderaManagerProductV4Request();
        cdp.setName("cdp");
        cdp.setParcel("cdp.parcel");
        cdp.setVersion("cdp.version");

        ClouderaManagerProductV4Request cdf = new ClouderaManagerProductV4Request();
        cdf.setName("cdf");
        cdf.setParcel("cdf.parcel");
        cdf.setVersion("cdf.version");

        List<ClouderaManagerProductV4Request> products = List.of(cdp, cdf);
        cm.setProducts(products);
        request.setCm(cm);

        Cluster cluster = underTest.convert(request);
        assertFalse(cluster.getComponents().isEmpty());
        assertEquals(2, cluster.getComponents().size());

        assertAll(cluster.getComponents()
                .stream()
                .map(component -> () -> assertEquals(ComponentType.CDH_PRODUCT_DETAILS, component.getComponentType())));

        List<Json> cdps = cluster.getComponents()
                .stream().map(ClusterComponent::getAttributes).filter(attr -> attr.getValue().contains("cdp")).collect(Collectors.toList());

        Json cdpJson = new Json(cdp);
        assertAll(
                () -> assertEquals(1, cdps.size()),
                () -> assertEquals(cdpJson, cdps.iterator().next()));

        List<Json> cdfs = cluster.getComponents()
                .stream().map(ClusterComponent::getAttributes).filter(attr -> attr.getValue().contains("cdf")).collect(Collectors.toList());

        Json cdfJson = new Json(cdf);
        assertAll(
                () -> assertEquals(1, cdfs.size()),
                () -> assertEquals(cdfJson, cdfs.iterator().next()));
    }
}
