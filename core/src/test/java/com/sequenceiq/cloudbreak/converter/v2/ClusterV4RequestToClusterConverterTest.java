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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.cloud.model.component.StackType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.CloudStorageConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterV4RequestToClusterConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.cloudstorage.CloudStorageBase;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;

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
    private ConversionService conversionService;

    @Mock
    private CloudStorageConverter cloudStorageConverter;

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
        String ldapName = "ldap-name";

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

        Cluster actual = underTest.convert(source);

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
        assertEquals("RDS config names dont exists", exception.getMessage());

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
        assertEquals("Cluster definition does not exists by name: bp-name", exception.getMessage());
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

        when(conversionService.convert(gatewayJson, Gateway.class)).thenReturn(gateway);

        Cluster actual = underTest.convert(source);

        assertThat(actual.getGateway(), is(gateway));

        verify(conversionService, times(1)).convert(gatewayJson, Gateway.class);
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
