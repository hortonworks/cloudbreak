package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ExposedService;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToExposedServicesConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToGatewayTopologyConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@RunWith(MockitoJUnitRunner.class)
public class GatewayTopologyV4RequestToGatewayTopologyConverterTest {

    private static final String TOPOLOGY_NAME = "topologyName";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ConversionService conversionService;

    @Spy
    private final GatewayTopologyV4RequestValidator validator = new GatewayTopologyV4RequestValidator(new ExposedServiceListValidator());

    @InjectMocks
    private final GatewayTopologyV4RequestToGatewayTopologyConverter underTest = new GatewayTopologyV4RequestToGatewayTopologyConverter();

    @Spy
    private final ExposedServiceListValidator exposedServiceListValidator = new ExposedServiceListValidator();

    @InjectMocks
    private final GatewayTopologyV4RequestToExposedServicesConverter gatewayTopologyJsonToExposedServicesConverter
            = new GatewayTopologyV4RequestToExposedServicesConverter();

    @Test
    public void testConvertWithNoTopologyName() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setExposedServices(Collections.singletonList(ExposedService.AMBARI.getKnoxService()));
        thrown.expect(BadRequestException.class);

        underTest.convert(gatewayTopologyJson);
    }

    @Test
    public void testConvertWithNoService() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);

        GatewayTopology result = underTest.convert(gatewayTopologyJson);

        assertEquals(TOPOLOGY_NAME, result.getTopologyName());
        assertNotNull(validator);
    }

    @Test
    public void testConvertWithAllKnoxServices() throws IOException {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        gatewayTopologyJson.setExposedServices(Collections.singletonList(ExposedService.ALL.getServiceName()));
        ExposedServices expectedExposedServices = gatewayTopologyJsonToExposedServicesConverter.convert(gatewayTopologyJson);
        when(conversionService.convert(any(GatewayTopologyV4Request.class), eq(ExposedServices.class))).thenReturn(expectedExposedServices);

        GatewayTopology result = underTest.convert(gatewayTopologyJson);

        assertEquals(TOPOLOGY_NAME, result.getTopologyName());
        assertTrue(result.getExposedServices().get(ExposedServices.class).getServices().containsAll(ExposedService.getAllKnoxExposed()));
    }

    @Test
    public void testConvertWithInvalidService() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        String invalidService = "INVALID_SERVICE";
        gatewayTopologyJson.setExposedServices(Arrays.asList(ExposedService.AMBARI.getKnoxService(), invalidService));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage(invalidService);

        underTest.convert(gatewayTopologyJson);
    }
}