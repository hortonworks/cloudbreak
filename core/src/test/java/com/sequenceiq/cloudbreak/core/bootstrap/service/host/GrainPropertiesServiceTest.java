package com.sequenceiq.cloudbreak.core.bootstrap.service.host;


import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.GrainProperties;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.common.model.CloudIdentityType;

@ExtendWith(MockitoExtension.class)
class GrainPropertiesServiceTest {

    @Mock
    private ComponentLocatorService componentLocator;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private ExposedServiceCollector exposedServiceCollector;

    @InjectMocks
    private GrainPropertiesService underTest;

    private CollectionMatcher<String> namenodeMatcher = new CollectionMatcher<>(List.of("NAMENODE"));

    private CollectionMatcher<String> knoxGatewayMatcher = new CollectionMatcher<>(List.of("KNOX_GATEWAY"));

    private CollectionMatcher<String> idBrokerMatcher = new CollectionMatcher<>(List.of("IDBROKER"));

    private Cluster cluster = new Cluster();

    @BeforeEach
    public void init() {
        ExposedService namenode = new ExposedService();
        namenode.setName("NAMENODE");
        namenode.setServiceName("NAMENODE");
        when(exposedServiceCollector.getNameNodeService()).thenReturn(namenode);
        Stack stack = new Stack();
        stack.setId(1L);
        cluster.setStack(stack);
    }

    @Test
    public void testGwAddressSet() {
        List<GatewayConfig> gatewayConfigs = setupGwAddresses();
        List<GrainProperties> result = underTest.createGrainProperties(gatewayConfigs, cluster, Set.of());
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(
                hasEntry("GWHOSTNAME", Map.of("gateway-address", "GWPUBADDR")),
                hasEntry("GWHOSTNAME2", Map.of("gateway-address", "GWPUBADDR2"))
                )))));
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(hasKey("roles"))))))));
    }

    @Test
    public void testGwAddressSetDuringTargetedUpscale() {
        List<GatewayConfig> gatewayConfigs = setupGwAddresses();
        List<GrainProperties> result = underTest.createGrainPropertiesForTargetedUpscale(gatewayConfigs, cluster,
                Set.of(getNode("GWHOSTNAME", "GWPRIVADDR", "GWPUBADDR")));
        assertTrue(result.stream().filter(gp -> gp.getProperties().keySet().contains("GWHOSTNAME")).findFirst().isPresent());
        assertFalse(result.stream().filter(gp -> gp.getProperties().keySet().contains("GWHOSTNAME2")).findFirst().isPresent());
        assertTrue(result.stream().filter(gp -> gp.getProperties().values()
                .contains(Map.of("gateway-address", "GWPUBADDR"))).findFirst().isPresent());
        assertFalse(result.stream().filter(gp -> gp.getProperties().values()
                .contains(Map.of("gateway-address", "GWPUBADDR2"))).findFirst().isPresent());
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(hasKey("roles"))))))));
    }

    private List<GatewayConfig> setupGwAddresses() {
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(namenodeMatcher))).thenReturn(Map.of());
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(knoxGatewayMatcher))).thenReturn(Map.of());
        GatewayConfig gwconfig = mock(GatewayConfig.class);
        when(gwconfig.getPublicAddress()).thenReturn("GWPUBADDR");
        lenient().when(gwconfig.getPrivateAddress()).thenReturn("GWPRIVADDR");
        when(gwconfig.getHostname()).thenReturn("GWHOSTNAME");
        GatewayConfig gwconfig2 = mock(GatewayConfig.class);
        lenient().when(gwconfig2.getPublicAddress()).thenReturn("GWPUBADDR2");
        lenient().when(gwconfig2.getPrivateAddress()).thenReturn("GWPRIVADDR2");
        lenient().when(gwconfig2.getHostname()).thenReturn("GWHOSTNAME2");
        return List.of(gwconfig, gwconfig2);
    }

    @Test
    public void testNameNodeRoleSet() {
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(namenodeMatcher))).thenReturn(Map.of("NAMENODE", List.of("NMHOST")));
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(knoxGatewayMatcher))).thenReturn(Map.of());
        List<GrainProperties> result = underTest.createGrainProperties(List.of(), cluster, Set.of());
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(hasKey("gateway-address"))))))));
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("NMHOST", Map.of("roles", "namenode")))))));
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(Map.of("roles", "knox"))))))));
    }

    @Test
    public void testNameNodeRoleSetDuringTargetedUpscale() {
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(namenodeMatcher))).thenReturn(
                Map.of("NAMENODE", List.of("NMHOST", "NMHOST2")));
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(knoxGatewayMatcher))).thenReturn(Map.of());
        List<GrainProperties> result = underTest.createGrainPropertiesForTargetedUpscale(List.of(), cluster,
                Set.of(getNode("NMHOST", null, null)));
        assertTrue(result.stream().filter(gp -> gp.getProperties().keySet().contains("NMHOST")).findFirst().isPresent());
        assertFalse(result.stream().filter(gp -> gp.getProperties().keySet().contains("NMHOST2")).findFirst().isPresent());
        assertTrue(result.stream().filter(gp -> gp.getProperties().values()
                .contains(Map.of("roles", "namenode"))).findFirst().isPresent());
    }

    @Test
    public void testKnoxGwRoleSet() {
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(namenodeMatcher))).thenReturn(Map.of());
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(knoxGatewayMatcher))).thenReturn(Map.of("KNOX_GATEWAY", List.of("KWGHOST")));
        List<GrainProperties> result = underTest.createGrainProperties(List.of(), cluster, Set.of());
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(hasKey("gateway-address"))))))));
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("KWGHOST", Map.of("roles", "knox")))))));
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(Map.of("roles", "namenode"))))))));
    }

    @Test
    public void testKnoxGwRoleSetDuringTargetedUpscale() {
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(namenodeMatcher))).thenReturn(Map.of());
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(knoxGatewayMatcher))).thenReturn(Map.of("KNOX_GATEWAY",
                List.of("KWGHOST", "KWGHOST2")));
        List<GrainProperties> result = underTest.createGrainPropertiesForTargetedUpscale(List.of(), cluster,
                Set.of(getNode("KWGHOST", null, null)));
        assertTrue(result.stream().filter(gp -> gp.getProperties().keySet().contains("KWGHOST")).findFirst().isPresent());
        assertFalse(result.stream().filter(gp -> gp.getProperties().keySet().contains("KWGHOST2")).findFirst().isPresent());
        assertTrue(result.stream().filter(gp -> gp.getProperties().values()
                .contains(Map.of("roles", "knox"))).findFirst().isPresent());
    }

    @Test
    public void testIdBrokerSetDuringTargetedUpscale() {
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(namenodeMatcher))).thenReturn(Map.of());
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(knoxGatewayMatcher))).thenReturn(Map.of());
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(idBrokerMatcher))).thenReturn(Map.of("IDBROKER",
                List.of("IDBH1", "IDBH2")));
        List<GrainProperties> result = underTest.createGrainPropertiesForTargetedUpscale(List.of(), cluster,
                Set.of(getNode("IDBH1", null, null)));
        assertTrue(result.stream().filter(gp -> gp.getProperties().keySet().contains("IDBH1")).findFirst().isPresent());
        assertFalse(result.stream().filter(gp -> gp.getProperties().keySet().contains("IDBH2")).findFirst().isPresent());
        assertTrue(result.stream().filter(gp -> gp.getProperties().values()
                .contains(Map.of("roles", "idbroker"))).findFirst().isPresent());
    }

    @Test
    public void testCloudIdentityRoleSet() {
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(namenodeMatcher))).thenReturn(Map.of());
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(knoxGatewayMatcher))).thenReturn(Map.of());
        Node node1 = new Node("", "", "", "", "node1fqdn", "");
        Node node2 = new Node("", "", "", "", "node2fqdn", "");
        Node node3 = new Node("", "", "", "", "node3fqdn", "");
        Node node4 = new Node("", "", "", "", "node4fqdn", "");

        InstanceMetaData im1 = new InstanceMetaData();
        im1.setDiscoveryFQDN(node1.getHostname());
        InstanceGroup logIg = new InstanceGroup();
        logIg.setAttributes(new Json("{}"));
        logIg.setCloudIdentityType(CloudIdentityType.LOG);
        im1.setInstanceGroup(logIg);

        InstanceMetaData im2 = new InstanceMetaData();
        im2.setDiscoveryFQDN(node2.getHostname());
        InstanceGroup idBroker = new InstanceGroup();
        idBroker.setAttributes(new Json("{}"));
        idBroker.setCloudIdentityType(CloudIdentityType.ID_BROKER);
        im2.setInstanceGroup(idBroker);

        InstanceMetaData im3 = new InstanceMetaData();
        im3.setDiscoveryFQDN(node3.getHostname());
        InstanceGroup noCloudIdentity = new InstanceGroup();
        im3.setInstanceGroup(noCloudIdentity);

        InstanceMetaData im4 = new InstanceMetaData();
        im4.setDiscoveryFQDN("invalid");

        when(instanceMetaDataService.getAllInstanceMetadataByStackId(1L)).thenReturn(Set.of(im1, im2, im3, im4));

        List<GrainProperties> result = underTest.createGrainProperties(List.of(), cluster, Set.of(node1, node2, node3, node4));

        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(hasKey("gateway-address"))))))));
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(Map.of("roles", "namenode"))))))));
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasValue(Map.of("roles", "knox"))))))));
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("node1fqdn", Map.of("roles", "LOG_CLOUD_IDENTITY_ROLE")))))));
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("node2fqdn", Map.of("roles", "ID_BROKER_CLOUD_IDENTITY_ROLE")))))));
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("node3fqdn", Map.of("roles", "LOG_CLOUD_IDENTITY_ROLE")))))));
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasKey("invalid")))))));
        assertThat(result, not(hasItem(allOf(hasProperty("properties", allOf(hasKey("node4fqdn")))))));
    }

    @Test
    public void testAllInOne() {
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(namenodeMatcher))).thenReturn(Map.of("NAMENODE", List.of("NMHOST")));
        when(componentLocator.getComponentLocationByHostname(eq(cluster), argThat(knoxGatewayMatcher))).thenReturn(Map.of("KNOX_GATEWAY", List.of("KWGHOST")));
        GatewayConfig gwconfig = mock(GatewayConfig.class);
        when(gwconfig.getPublicAddress()).thenReturn("GWPUBADDR");
        when(gwconfig.getHostname()).thenReturn("GWHOSTNAME");
        Node node1 = new Node("", "", "", "", "node1fqdn", "");
        InstanceMetaData im1 = new InstanceMetaData();
        im1.setDiscoveryFQDN(node1.getHostname());
        InstanceGroup logIg = new InstanceGroup();
        logIg.setAttributes(new Json("{}"));
        logIg.setCloudIdentityType(CloudIdentityType.LOG);
        im1.setInstanceGroup(logIg);
        when(instanceMetaDataService.getAllInstanceMetadataByStackId(1L)).thenReturn(Set.of(im1));

        List<GrainProperties> result = underTest.createGrainProperties(List.of(gwconfig), cluster, Set.of(node1));

        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("GWHOSTNAME", Map.of("gateway-address", "GWPUBADDR")))))));
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("NMHOST", Map.of("roles", "namenode")))))));
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("KWGHOST", Map.of("roles", "knox")))))));
        assertThat(result, hasItem(allOf(hasProperty("properties", allOf(hasEntry("node1fqdn", Map.of("roles", "LOG_CLOUD_IDENTITY_ROLE")))))));
    }

    private Node getNode(String host, String privateIp, String publicIp) {
        return new Node(privateIp, publicIp, null, null, host, null);
    }

    public static class CollectionMatcher<T> implements ArgumentMatcher<Collection<T>> {

        private final Collection<T> elements;

        CollectionMatcher(Collection<T> elements) {
            this.elements = elements;
        }

        @Override
        public boolean matches(Collection<T> argument) {
            return argument != null && argument.containsAll(elements);
        }
    }

}