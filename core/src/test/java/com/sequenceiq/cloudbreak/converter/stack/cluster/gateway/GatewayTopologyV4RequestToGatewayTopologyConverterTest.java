package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceUtil.exposedService;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToExposedServicesConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToGatewayTopologyConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@RunWith(MockitoJUnitRunner.class)
public class GatewayTopologyV4RequestToGatewayTopologyConverterTest {

    private static final String TOPOLOGY_NAME = "topologyName";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private GatewayTopologyV4RequestValidator gatewayTopologyV4RequestValidator;

    @Mock
    private GatewayTopologyV4RequestToExposedServicesConverter gatewayTopologyV4RequestToExposedServicesConverter;

    @InjectMocks
    private GatewayTopologyV4RequestToGatewayTopologyConverter underTest;

    @Test
    public void testConvertWithValidationError() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setExposedServices(Collections.singletonList(exposedService("CLOUDERA_MANAGER_UI").getKnoxService()));
        when(gatewayTopologyV4RequestValidator.validate(any(GatewayTopologyV4Request.class))).thenReturn(
                new ValidationResultBuilder().error("INVALID").build());

        thrown.expect(BadRequestException.class);

        underTest.convert(gatewayTopologyJson);
    }

    @Test
    public void testConvert() throws IOException {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        List<String> allServices = Collections.singletonList("ALL");
        ExposedServices exposedServices = new ExposedServices();
        exposedServices.setServices(allServices);
        gatewayTopologyJson.setExposedServices(allServices);

        when(gatewayTopologyV4RequestValidator.validate(any(GatewayTopologyV4Request.class))).thenReturn(
                new ValidationResultBuilder().build());
        when(gatewayTopologyV4RequestToExposedServicesConverter.convert(any(GatewayTopologyV4Request.class))).thenReturn(exposedServices);

        GatewayTopology result = underTest.convert(gatewayTopologyJson);

        assertEquals(TOPOLOGY_NAME, result.getTopologyName());
        assertTrue(result.getExposedServices().get(ExposedServices.class).getServices().contains("ALL"));
        // assertTrue(result.getExposedServices().get(ExposedServices.class).getFullServiceList().containsAll(ExposedService.getAllKnoxExposed()));
    }
}
