package com.sequenceiq.cloudbreak.converter.stack.cluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintToBlueprintV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.database.RDSConfigToDatabaseV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ClusterToClusterV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager.ClusterToClouderaManagerV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.GatewayToGatewayV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.v4.workspaces.WorkspaceToWorkspaceResourceV4ResponseConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigWithoutClusterService;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.model.StringToSecretResponseConverter;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.CertExpirationState;

@RunWith(MockitoJUnitRunner.class)
public class ClusterToClusterV4ResponseConverterTest extends AbstractEntityConverterTest<Cluster> {

    public static final String CERT_EXPIRATION_DETAILS = "Cert will expire in 60 days";

    @InjectMocks
    private ClusterToClusterV4ResponseConverter underTest;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock
    private WorkspaceToWorkspaceResourceV4ResponseConverter workspaceToWorkspaceResourceV4ResponseConverter;

    @Mock
    private BlueprintToBlueprintV4ResponseConverter blueprintToBlueprintV4ResponseConverter;

    @Mock
    private GatewayToGatewayV4ResponseConverter gatewayToGatewayV4ResponseConverter;

    @Mock
    private StringToSecretResponseConverter stringToSecretResponseConverter;

    @Mock
    private RDSConfigToDatabaseV4ResponseConverter rdsConfigToDatabaseV4ResponseConverter;

    @Mock
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Mock
    private ClusterToClouderaManagerV4ResponseConverter clusterToClouderaManagerV4ResponseConverter;

    @Before
    public void setUp() {
        when(workspaceToWorkspaceResourceV4ResponseConverter.convert(any(Workspace.class)))
                .thenReturn(new WorkspaceResourceV4Response());
    }

    @Test
    public void testConvert() {
        // GIVEN
        StackDtoDelegate stackDtoDelegate = mock(StackDtoDelegate.class);
        Cluster source = getSource();
        source.setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        source.setBlueprint(new Blueprint());
        source.setExtendedBlueprintText("asdf");
        source.setFqdn("some.fqdn");
        source.setDbSslRootCertBundle("bundle");
        source.setCertExpirationState(CertExpirationState.HOST_CERT_EXPIRING);
        source.setCertExpirationDetails(CERT_EXPIRATION_DETAILS);
        source.setEncryptionProfileCrn("epCrn");
        Blueprint blueprint = source.getBlueprint();

        // WHEN
        when(stackDtoDelegate.getBlueprint()).thenReturn(blueprint);
        when(stackDtoDelegate.getStack()).thenReturn(source.getStack());
        when(stackUtil.extractClusterManagerIp(any(StackDtoDelegate.class))).thenReturn("10.0.0.1");
        when(stackUtil.extractClusterManagerAddress(any(StackDtoDelegate.class))).thenReturn("some.fqdn");
        TestUtil.setSecretField(Cluster.class, "cloudbreakClusterManagerUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "cloudbreakClusterManagerPassword", source, "pass", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpClusterManagerUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpClusterManagerPassword", source, "pass", "secret/path");
        when(stringToSecretResponseConverter.convert("secret/path")).thenReturn(new SecretResponse("kv", "pass", 3));
        when(blueprintToBlueprintV4ResponseConverter.convert(blueprint)).thenReturn(new BlueprintV4Response());
        when(serviceEndpointCollector.getManagerServerUrl(any(StackDtoDelegate.class), anyString())).thenReturn("http://server/");
        when(proxyConfigDtoService.getByCrn(anyString())).thenReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());
        when(stackDtoDelegate.getWorkspace()).thenReturn(source.getWorkspace());
        when(stackDtoDelegate.getCluster()).thenReturn(source);

        ClusterV4Response result = underTest.convert(stackDtoDelegate);

        // THEN
        assertEquals(1L, (long) result.getId());
        assertEquals(getSource().getExtendedBlueprintText(), result.getExtendedBlueprintText());
        assertEquals(CertExpirationState.HOST_CERT_EXPIRING, result.getCertExpirationState());
        assertEquals(CERT_EXPIRATION_DETAILS, result.getCertExpirationDetails());
        assertEquals("epCrn", result.getEncryptionProfileCrn());
        List<String> skippedFields = Lists.newArrayList("customContainers", "cm", "creationFinished", "cloudStorage", "gateway", "customConfigurationsName",
                "customConfigurationsCrn");
        assertAllFieldsNotNull(result, skippedFields);
    }

    @Test
    public void testConvertWithoutUpSinceField() {
        // GIVEN
        StackDtoDelegate stackDtoDelegate = mock(StackDtoDelegate.class);
        when(proxyConfigDtoService.getByCrn(anyString())).thenReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());
        Cluster source = getSource();
        source.setUpSince(null);
        when(stackDtoDelegate.getStack()).thenReturn(source.getStack());
        when(stackDtoDelegate.getCluster()).thenReturn(source);
        // WHEN
        ClusterV4Response result = underTest.convert(stackDtoDelegate);
        // THEN
        assertEquals(0L, result.getMinutesUp());
    }

    @Test
    public void testConvertWithoutMasterComponent() {
        // GIVEN
        StackDtoDelegate stackDtoDelegate = mock(StackDtoDelegate.class);
        when(stackDtoDelegate.getStack()).thenReturn(new Stack());
        when(proxyConfigDtoService.getByCrn(anyString())).thenReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());
        Cluster source = getSource();
        when(stackDtoDelegate.getCluster()).thenReturn(source);
        // WHEN
        ClusterV4Response result = underTest.convert(stackDtoDelegate);
        // THEN
        assertEquals(1L, (long) result.getId());
    }

    @Test
    public void testExposedServices() {
        StackDtoDelegate stackDtoDelegate = mock(StackDtoDelegate.class);
        Map<String, Collection<ClusterExposedServiceV4Response>> exposedServiceResponseMap = new HashMap<>();
        exposedServiceResponseMap.put("topology1", getExpiosedServices());
        Cluster source = getSource();
        when(stackDtoDelegate.getCluster()).thenReturn(source);
        when(serviceEndpointCollector.prepareClusterExposedServices(any(), anyString())).thenReturn(exposedServiceResponseMap);
        when(proxyConfigDtoService.getByCrn(anyString())).thenReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());

        when(stackDtoDelegate.getStack()).thenReturn(source.getStack());
        when(stackUtil.extractClusterManagerIp(any(StackDtoDelegate.class))).thenReturn("10.0.0.1");
        when(stackUtil.extractClusterManagerAddress(any(StackDtoDelegate.class))).thenReturn("some.fqdn");
        ClusterV4Response clusterResponse = underTest.convert(stackDtoDelegate);
        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesForTopologies = clusterResponse.getExposedServices();
        assertEquals(1L, clusterExposedServicesForTopologies.keySet().size());
        Collection<ClusterExposedServiceV4Response> topology1ServiceList = clusterExposedServicesForTopologies.get("topology1");
        assertEquals(2L, topology1ServiceList.size());
    }

    @Override
    public Cluster createSource() {
        Stack stack = TestUtil.stack();
        Blueprint blueprint = TestUtil.blueprint();
        Cluster cluster = TestUtil.cluster(blueprint, stack, 1L);
        cluster.setProxyConfigCrn("test-CRN");
        stack.setCluster(cluster);
        Gateway gateway = new Gateway();
        cluster.setGateway(gateway);
        return cluster;
    }

    private List<ClusterExposedServiceV4Response> getExpiosedServices() {
        List<ClusterExposedServiceV4Response> clusterExposedServiceResponseList = new ArrayList<>();
        ClusterExposedServiceV4Response firstClusterExposedServiceV4Response = new ClusterExposedServiceV4Response();
        firstClusterExposedServiceV4Response.setOpen(true);
        firstClusterExposedServiceV4Response.setServiceUrl("http://service1");
        firstClusterExposedServiceV4Response.setServiceName("serviceName1");
        firstClusterExposedServiceV4Response.setKnoxService("knoxService1");
        firstClusterExposedServiceV4Response.setDisplayName("displayName1");
        ClusterExposedServiceV4Response secondClusterExposedServiceV4Response = new ClusterExposedServiceV4Response();
        clusterExposedServiceResponseList.add(firstClusterExposedServiceV4Response);
        secondClusterExposedServiceV4Response.setOpen(false);
        secondClusterExposedServiceV4Response.setServiceUrl("http://service2");
        secondClusterExposedServiceV4Response.setServiceName("serviceName2");
        secondClusterExposedServiceV4Response.setKnoxService("knoxService2");
        secondClusterExposedServiceV4Response.setDisplayName("displayName2");
        clusterExposedServiceResponseList.add(secondClusterExposedServiceV4Response);
        return clusterExposedServiceResponseList;
    }

}
