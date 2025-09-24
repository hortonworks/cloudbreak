package com.sequenceiq.cloudbreak.converter.v2;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.cdhProductDetails;
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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.GatewayV4RequestToGatewayConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.cloudstorage.AwsStorageParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
    private BlueprintService blueprintService;

    @Mock
    private GatewayConvertUtil gatewayConvertUtil;

    @Mock
    private CloudStorageConverter cloudStorageConverter;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GatewayV4RequestToGatewayConverter gatewayV4RequestToGatewayConverter;

    @Spy
    @SuppressFBWarnings(value = "UrF", justification = "This gets injected")
    private IdBrokerConverterUtil idBrokerConverterUtil = new IdBrokerConverterUtil();

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

        when(cloudStorageValidationUtil.isCloudStorageConfigured(nullable(CloudStorageBase.class))).thenReturn(false);
    }

    @Test
    public void testConvertWhenCloudStorageConfiguredAndRdsAndLdapAndProxyExistsAnd() {
        CloudStorageRequest cloudStorageRequest = mock(CloudStorageRequest.class);

        String rdsConfigName = "rds-name";
        String proxyConfigCrn = "proxy-config-resource-crn";

        FileSystem fileSystem = new FileSystem();

        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setName(rdsConfigName);

        ClusterV4Request source = new ClusterV4Request();
        source.setCloudStorage(cloudStorageRequest);
        source.setDatabases(singleton(rdsConfigName));
        source.setProxyConfigCrn(proxyConfigCrn);
        source.setBlueprintName(BLUEPRINT);
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);

        when(cloudStorageValidationUtil.isCloudStorageConfigured(cloudStorageRequest)).thenReturn(true);
        when(cloudStorageConverter.requestToFileSystem(cloudStorageRequest)).thenReturn(fileSystem);
        when(rdsConfigService.findByNamesInWorkspace(singleton(rdsConfigName), workspace.getId())).thenReturn(singleton(rdsConfig));

        Cluster actual = ThreadBasedUserCrnProvider.doAs("crn:cdp:iam:us-west-1:1234:user:1", () -> underTest.convert(source));

        assertThat(actual.getFileSystem(), is(fileSystem));
        assertThat(actual.getName(), is(source.getName()));
        assertThat(actual.getRdsConfigs().size(), is(1));
        assertThat(actual.getRdsConfigs().stream().findFirst().get().getName(), is(rdsConfigName));
        assertThat(actual.getProxyConfigCrn(), is(proxyConfigCrn));

        verify(cloudStorageValidationUtil, times(1)).isCloudStorageConfigured(cloudStorageRequest);
        verify(cloudStorageConverter, times(1)).requestToFileSystem(cloudStorageRequest);
        verify(rdsConfigService, times(1)).findByNamesInWorkspace(singleton(rdsConfigName), workspace.getId());
    }

    @Test
    public void testConvertWhenNoRdsConfig() {
        ClusterV4Request source = new ClusterV4Request();
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

        Set<String> rdsConfigNames = singleton("fake-rds-name");
        when(rdsConfigService.findByNamesInWorkspace(rdsConfigNames, workspace.getId())).thenReturn(emptySet());

        source.setDatabases(rdsConfigNames);

        Exception exception = assertThrows(NotFoundException.class, () -> underTest.convert(source));
        assertEquals("RDS config names do not exist", exception.getMessage());

        verify(rdsConfigService, times(1)).findByNamesInWorkspace(rdsConfigNames, workspace.getId());
    }

    @Test
    public void testConvertWheBlueprintDoesNotExists() {
        Mockito.reset(cloudStorageValidationUtil);

        String blueprintName = "bp-name";

        ClusterV4Request source = new ClusterV4Request();
        source.setBlueprintName(blueprintName);

        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace)).thenReturn(null);

        Exception exception = assertThrows(NotFoundException.class, () -> underTest.convert(source));
        assertEquals("Cluster definition does not exist by name: bp-name", exception.getMessage());
    }

    @Test
    public void testConvertWheBlueprintExists() {
        String blueprintName = "bp-name";

        Blueprint blueprint = new Blueprint();
        blueprint.setName(blueprintName);

        ClusterV4Request source = new ClusterV4Request();
        source.setBlueprintName(blueprintName);

        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace)).thenReturn(blueprint);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getBlueprint(), is(blueprint));

        verify(blueprintService, times(1)).getByNameForWorkspaceAndLoadDefaultsIfNecessary(blueprintName, workspace);
    }

    @Test
    public void testConvertWhenGatewayExists() {
        String clusterName = "cluster-name";

        ClusterV4Request source = new ClusterV4Request();

        GatewayV4Request gatewayJson = new GatewayV4Request();
        source.setGateway(gatewayJson);
        source.setBlueprintName(BLUEPRINT);
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);
        Gateway gateway = new Gateway();

        when(gatewayV4RequestToGatewayConverter.convert(gatewayJson)).thenReturn(gateway);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getGateway(), is(gateway));

        verify(gatewayV4RequestToGatewayConverter, times(1)).convert(gatewayJson);
    }

    @Test
    public void testConvertClouderaManagerRequestWithNullProductList() {
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
    public void testConvertClouderaManagerRequestWithNullRepo() {
        ClusterV4Request request = new ClusterV4Request();
        request.setBlueprintName(BLUEPRINT);
        blueprint.setStackType(StackType.CDH.name());
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(eq(BLUEPRINT), any())).thenReturn(blueprint);
        ClouderaManagerV4Request cm = new ClouderaManagerV4Request();

        ClouderaManagerProductV4Request cdp = new ClouderaManagerProductV4Request();
        cdp.setName("cdp");
        cdp.setParcel("cdp.parcel");
        cdp.setVersion("cdp.version");
        cdp.setCsd(List.of("cdp.csd"));

        ClouderaManagerProductV4Request cdf = new ClouderaManagerProductV4Request();
        cdf.setName("cdf");
        cdf.setParcel("cdf.parcel");
        cdf.setVersion("cdf.version");
        cdf.setCsd(List.of("cdf.csd"));

        List<ClouderaManagerProductV4Request> products = List.of(cdp, cdf);
        cm.setProducts(products);
        request.setCm(cm);

        Cluster cluster = underTest.convert(request);
        assertFalse(cluster.getComponents().isEmpty());
        assertEquals(2, cluster.getComponents().size());

        assertAll(cluster.getComponents()
                .stream()
                .map(component -> () -> assertEquals(cdhProductDetails(), component.getComponentType())));

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

    @Test
    public void testConvertClusterV4RequestToCluster() {
        blueprint.setStackType("CDH");
        when(blueprintService.getByNameForWorkspaceAndLoadDefaultsIfNecessary(any(), any())).thenReturn(blueprint);
        when(rdsConfigService.findByNamesInWorkspace(any(), any())).thenReturn(Set.of(new RDSConfig()));
        ClusterV4Request clusterV4Request = new ClusterV4Request();
        clusterV4Request.setName("test-name");
        clusterV4Request.setUserName("username");
        clusterV4Request.setPassword("TestPassword123#!");
        clusterV4Request.setDatabases(Set.of("resource-crn"));
        clusterV4Request.setDatabaseServerCrn("resource-crn");
        clusterV4Request.setProxyConfigCrn("resource-crn");
        CloudStorageRequest cloudStorageRequest = new CloudStorageRequest();
        cloudStorageRequest.setAws(new AwsStorageParameters());
        clusterV4Request.setCloudStorage(cloudStorageRequest);
        ClouderaManagerV4Request cm = new ClouderaManagerV4Request();

        ClouderaManagerRepositoryV4Request repository = new ClouderaManagerRepositoryV4Request();
        repository.setBaseUrl("base.url");
        repository.setVersion("1.0");
        repository.setGpgKeyUrl("gpg.key.url");
        cm.setRepository(repository);
        clusterV4Request.setCm(cm);
        GatewayV4Request gatewayV4Request = new GatewayV4Request();
        gatewayV4Request.setGatewayType(GatewayType.CENTRAL);
        gatewayV4Request.setPath("/");
        clusterV4Request.setGateway(gatewayV4Request);
        clusterV4Request.setCustomQueue("queue");
        clusterV4Request.setBlueprintName("bp-name");
        clusterV4Request.setRangerRazEnabled(true);
        clusterV4Request.setRangerRmsEnabled(true);

        Cluster convert = underTest.convert(clusterV4Request);

        assertEquals(convert.getName(), clusterV4Request.getName());
        assertEquals(convert.getDatabaseServerCrn(), clusterV4Request.getDatabaseServerCrn());
        assertEquals(convert.getProxyConfigCrn(), clusterV4Request.getProxyConfigCrn());
        assertEquals(convert.isRangerRazEnabled(), clusterV4Request.isRangerRazEnabled());
        assertEquals(convert.isRangerRmsEnabled(), clusterV4Request.isRangerRmsEnabled());
    }
}
