package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToExposedServicesConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@RunWith(MockitoJUnitRunner.class)
public class GatewayTopologyJsonToExposedServicesConverterTest {

    private static final String TOPOLOGY_NAME = "topologyName";

    private static final String CLOUDERA_MANAGER_UI = "CM-UI";

    private static final String INVALID = "INVALID";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ExposedServiceListValidator exposedServiceListValidator;

    @InjectMocks
    private GatewayTopologyV4RequestToExposedServicesConverter underTest;

    @Test
    public void testWithInvalidExposedService() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        gatewayTopologyJson.setExposedServices(Collections.singletonList(INVALID));
        when(exposedServiceListValidator.validate(anyList())).thenReturn(new ValidationResultBuilder().error(INVALID).build());

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(INVALID);

        underTest.convert(gatewayTopologyJson);
    }

    @Test
    public void testWithSingleExposedService() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        gatewayTopologyJson.setExposedServices(Collections.singletonList(CLOUDERA_MANAGER_UI));

        when(exposedServiceListValidator.validate(anyList())).thenReturn(new ValidationResultBuilder().build());

        ExposedServices exposedServices = underTest.convert(gatewayTopologyJson);

        assertEquals(1L, exposedServices.getServices().size());
        assertEquals(CLOUDERA_MANAGER_UI, exposedServices.getServices().get(0));
    }
}
