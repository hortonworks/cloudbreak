package com.sequenceiq.cloudbreak.template.views;

import static com.sequenceiq.cloudbreak.template.views.ExposedServiceUtil.exposedService;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

public class GatewayViewTest {

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
    public void testInitializeGatewayView() {
        GatewayView gatewayView = new GatewayView(TestUtil.gatewayEnabled(), "/cb/secret/signkey", ALL_SERVICES);

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertEquals(Collections.emptySet(), gatewayView.getExposedServices());
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    public void testInitializeGatewayViewWithExposedServiceSetElement() {
        GatewayTopology gatewayTopology = gatewayTopologyExposedServicesAsSet(exposedService("NAMENODE").getKnoxService());
        GatewayView gatewayView = new GatewayView(gatewayEnabled(gatewayTopology), "/cb/secret/signkey", ALL_SERVICES);

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertTrue(gatewayView.getExposedServices().contains(exposedService("NAMENODE").getKnoxService()));
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    public void testInitializeGatewayViewWithEmptyExposedServiceSet() {
        GatewayView gatewayView = new GatewayView(gatewayEnabled(gatewayTopologyExposedServicesAsSet()), "/cb/secret/signkey", ALL_SERVICES);

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertEquals(Collections.emptySet(), gatewayView.getExposedServices());
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    public void testInitializeGatewayViewWithService() {
        GatewayView gatewayView = new GatewayView(
                gatewayEnabled(gatewayTopology(exposedService("NAMENODE").getKnoxService())), "/cb/secret/signkey", ALL_SERVICES);

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertEquals(1, gatewayView.getExposedServices().size());
        Assert.assertTrue(gatewayView.getExposedServices().contains(exposedService("NAMENODE").getKnoxService()));
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    public void testInitializeGatewayViewWithAll() {
        GatewayView gatewayView = new GatewayView(
                gatewayEnabled(gatewayTopology(exposedService("ALL").getServiceName())), "/cb/secret/signkey", ALL_SERVICES);

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertEquals(ALL_SERVICES.size(), gatewayView.getExposedServices().size());
        Assert.assertTrue(gatewayView.getExposedServices().contains(exposedService("NAMENODE").getKnoxService()));
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    public void testInitializeGatewayViewWithAllAndOtherServiceThenFullListInExposedServices() {
        GatewayTopology gatewayTopology = gatewayTopology(exposedService("ALL").getServiceName(), exposedService("NAMENODE").getKnoxService());
        GatewayView gatewayView = new GatewayView(gatewayEnabled(gatewayTopology), "/cb/secret/signkey", ALL_SERVICES);

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertEquals(ALL_SERVICES.size(), gatewayView.getExposedServices().size());
        Assert.assertTrue(gatewayView.getExposedServices().contains(exposedService("NAMENODE").getKnoxService()));
        Assert.assertTrue(gatewayView.getExposedServices().contains(exposedService("RANGER_ADMIN").getKnoxService()));
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    public void testInitializeGatewayViewWithEmptyGatewayTopology() {
        GatewayView gatewayView = new GatewayView(gatewayEnabled(), "/cb/secret/signkey", ALL_SERVICES);

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertNull("topology is not null", gatewayView.getTopologyName());
        Assert.assertNull("exposed services not null", gatewayView.getExposedServices());
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
    }

    @Test
    public void testInitializeGatewayViewWithEmptyExposedServices() {
        GatewayView gatewayView = new GatewayView(gatewayEnabled(gatewayTopology()), "/cb/secret/signkey", ALL_SERVICES);

        Assert.assertEquals("/path", gatewayView.getPath());
        Assert.assertEquals("simple", gatewayView.getSsoProvider());
        Assert.assertEquals("tokencert", gatewayView.getTokenCert());
        Assert.assertEquals("topology", gatewayView.getTopologyName());
        Assert.assertEquals(Collections.emptySet(), gatewayView.getExposedServices());
        Assert.assertEquals(GatewayType.CENTRAL, gatewayView.getGatewayType());
        Assert.assertEquals(SSOType.SSO_PROVIDER, gatewayView.getSsoType());
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
