package com.sequenceiq.cloudbreak.template.views;

import static com.sequenceiq.cloudbreak.template.views.ExposedServiceUtil.exposedService;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

class GatewayViewTest {

    private static final Set<String> ALL_SERVICES = Set.of(
            "CM-API",
            "CM-UI",
            "HUE_LOAD_BALANCER",
            "NAMENODE",
            "RESOURCEMANAGER",
            "JOBHISTORY",
            "HIVESERVER2",
            "ATLAS_SERVER",
            "SPARK_YARN_HISTORY_SERVER",
            "ZEPPELIN_SERVER",
            "RANGER_ADMIN",
            "LIVY_SERVER",
            "OOZIE_SERVER",
            "SOLR_SERVER",
            "MASTER",
            "HBASERESTSERVER",
            "NIFI_NODE",
            "NIFI_REGISTRY_SERVER",
            "STREAMS_MESSAGING_MANAGER_UI",
            "STREAMS_MESSAGING_MANAGER_SERVER",
            "SCHEMA_REGISTRY_SERVER",
            "IMPALAD",
            "IMPALA_DEBUG_UI",
            "DATA_DISCOVERY_SERVICE_AGENT",
            "PROFILER_ADMIN_AGENT",
            "PROFILER_METRICS_AGENT",
            "PROFILER_SCHEDULER_AGENT",
            "DAS_WEBAPP",
            "KUDU_MASTER",
            "KUDU_TSERVER",
            "QUERY_PROCESSOR"
    );

    @Test
    void testInitializeGatewayView() {
        GatewayView gatewayView = new GatewayView(TestUtil.gatewayEnabled(), "/cb/secret/signkey", ALL_SERVICES);

        assertEquals("/path", gatewayView.getPath());
        assertEquals("simple", gatewayView.getSsoProvider());
        assertEquals("tokencert", gatewayView.getTokenCert());
        assertEquals("topology", gatewayView.getTopologyName());
        assertEquals(Collections.emptySet(), gatewayView.getExposedServices());
        assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    void testInitializeGatewayViewWithExposedServiceSetElement() {
        GatewayTopology gatewayTopology = gatewayTopologyExposedServicesAsSet(exposedService("NAMENODE").getKnoxService());
        GatewayView gatewayView = new GatewayView(gatewayEnabled(gatewayTopology), "/cb/secret/signkey", ALL_SERVICES);

        assertEquals("/path", gatewayView.getPath());
        assertEquals("simple", gatewayView.getSsoProvider());
        assertEquals("tokencert", gatewayView.getTokenCert());
        assertEquals("topology", gatewayView.getTopologyName());
        assertTrue(gatewayView.getExposedServices().contains(exposedService("NAMENODE").getKnoxService()));
        assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    void testInitializeGatewayViewWithEmptyExposedServiceSet() {
        GatewayView gatewayView = new GatewayView(gatewayEnabled(gatewayTopologyExposedServicesAsSet()), "/cb/secret/signkey", ALL_SERVICES);

        assertEquals("/path", gatewayView.getPath());
        assertEquals("simple", gatewayView.getSsoProvider());
        assertEquals("tokencert", gatewayView.getTokenCert());
        assertEquals("topology", gatewayView.getTopologyName());
        assertEquals(Collections.emptySet(), gatewayView.getExposedServices());
        assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    void testInitializeGatewayViewWithService() {
        GatewayView gatewayView = new GatewayView(
                gatewayEnabled(gatewayTopology(exposedService("NAMENODE").getKnoxService())), "/cb/secret/signkey", ALL_SERVICES);

        assertEquals("/path", gatewayView.getPath());
        assertEquals("simple", gatewayView.getSsoProvider());
        assertEquals("tokencert", gatewayView.getTokenCert());
        assertEquals("topology", gatewayView.getTopologyName());
        assertEquals(1, gatewayView.getExposedServices().size());
        assertTrue(gatewayView.getExposedServices().contains(exposedService("NAMENODE").getKnoxService()));
        assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    void testInitializeGatewayViewWithAll() {
        GatewayView gatewayView = new GatewayView(
                gatewayEnabled(gatewayTopology(exposedService("ALL").getServiceName())), "/cb/secret/signkey", ALL_SERVICES);

        assertEquals("/path", gatewayView.getPath());
        assertEquals("simple", gatewayView.getSsoProvider());
        assertEquals("tokencert", gatewayView.getTokenCert());
        assertEquals("topology", gatewayView.getTopologyName());
        assertEquals(ALL_SERVICES.size(), gatewayView.getExposedServices().size());
        assertTrue(gatewayView.getExposedServices().contains(exposedService("NAMENODE").getKnoxService()));
        assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    void testInitializeGatewayViewWithAllAndOtherServiceThenFullListInExposedServices() {
        GatewayTopology gatewayTopology = gatewayTopology(exposedService("ALL").getServiceName(), exposedService("NAMENODE").getKnoxService());
        GatewayView gatewayView = new GatewayView(gatewayEnabled(gatewayTopology), "/cb/secret/signkey", ALL_SERVICES);

        assertEquals("/path", gatewayView.getPath());
        assertEquals("simple", gatewayView.getSsoProvider());
        assertEquals("tokencert", gatewayView.getTokenCert());
        assertEquals("topology", gatewayView.getTopologyName());
        assertEquals(ALL_SERVICES.size(), gatewayView.getExposedServices().size());
        assertTrue(gatewayView.getExposedServices().contains(exposedService("NAMENODE").getKnoxService()));
        assertTrue(gatewayView.getExposedServices().contains(exposedService("RANGER_ADMIN").getKnoxService()));
        assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    void testInitializeGatewayViewWithEmptyGatewayTopology() {
        GatewayView gatewayView = new GatewayView(gatewayEnabled(), "/cb/secret/signkey", ALL_SERVICES);

        assertEquals("/path", gatewayView.getPath());
        assertEquals("simple", gatewayView.getSsoProvider());
        assertEquals("tokencert", gatewayView.getTokenCert());
        assertNull(gatewayView.getTopologyName(), "topology is not null");
        assertNull(gatewayView.getExposedServices(), "exposed services not null");
        assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    void testInitializeGatewayViewWithEmptyExposedServices() {
        GatewayView gatewayView = new GatewayView(gatewayEnabled(gatewayTopology()), "/cb/secret/signkey", ALL_SERVICES);

        assertEquals("/path", gatewayView.getPath());
        assertEquals("simple", gatewayView.getSsoProvider());
        assertEquals("tokencert", gatewayView.getTokenCert());
        assertEquals("topology", gatewayView.getTopologyName());
        assertEquals(Collections.emptySet(), gatewayView.getExposedServices());
        assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    private GatewayTopology gatewayTopology(String... services) {
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName("topology");
        ExposedServices exposedServices = new ExposedServices();
        exposedServices.setServices(List.of(services));
        gatewayTopology.setExposedServices(Json.silent(exposedServices));
        return gatewayTopology;
    }

    private GatewayTopology gatewayTopologyExposedServicesAsSet(String... services) {
        GatewayTopology gatewayTopology = new GatewayTopology();
        gatewayTopology.setTopologyName("topology");
        gatewayTopology.setExposedServices(Json.silent(Set.of(services)));
        return gatewayTopology;
    }

    public Gateway gatewayEnabled(GatewayTopology... gatewayTopology) {
        Gateway gateway = new Gateway();
        gateway.setPath("/path");
        gateway.setTopologies(Set.of(gatewayTopology));
        gateway.setSsoProvider("simple");
        gateway.setSsoType(SSOType.SSO_PROVIDER);
        gateway.setGatewayType(GatewayType.CENTRAL);
        gateway.setSignCert("signcert");
        gateway.setSignKey("signkey");
        gateway.setTokenCert("tokencert");
        gateway.setSignPub("signpub");
        return gateway;
    }
}
