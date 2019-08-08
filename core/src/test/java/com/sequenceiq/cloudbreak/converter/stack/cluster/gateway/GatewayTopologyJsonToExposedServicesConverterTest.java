package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToExposedServicesConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;

@RunWith(MockitoJUnitRunner.class)
public class GatewayTopologyJsonToExposedServicesConverterTest {

    private static final String TOPOLOGY_NAME = "topologyName";

    private static final String CLOUDERA_MANAGER_UI = "CM-UI";

    private static final String INVALID = "INVALID";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Spy
    private final ExposedServiceListValidator exposedServiceListValidator = new ExposedServiceListValidator();

    @InjectMocks
    private final GatewayTopologyV4RequestToExposedServicesConverter underTest = new GatewayTopologyV4RequestToExposedServicesConverter();

    @Test
    public void testWithFullServiceList() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        gatewayTopologyJson.setExposedServices(Collections.singletonList("ALL"));

        ExposedServices exposedServices = underTest.convert(gatewayTopologyJson);

        assertEquals(ExposedService.getAllKnoxExposed().size(), exposedServices.getFullServiceList().size());
        assertEquals(1, exposedServices.getServices().size());
        assertEquals("ALL", exposedServices.getServices().get(0));
    }

    @Test
    public void testWithInvalidExposedService() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        gatewayTopologyJson.setExposedServices(Collections.singletonList(INVALID));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(INVALID);

        underTest.convert(gatewayTopologyJson);
    }

    @Test
    public void testWithSingleExposedService() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        gatewayTopologyJson.setExposedServices(Collections.singletonList(CLOUDERA_MANAGER_UI));

        ExposedServices exposedServices = underTest.convert(gatewayTopologyJson);

        assertEquals(1L, exposedServices.getServices().size());
        assertEquals(CLOUDERA_MANAGER_UI, exposedServices.getServices().get(0));

        assertEquals(1L, exposedServices.getFullServiceList().size());
        assertEquals(CLOUDERA_MANAGER_UI, exposedServices.getFullServiceList().get(0));
    }
}