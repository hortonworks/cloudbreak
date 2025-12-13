package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToExposedServicesConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.ExposedServices;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;

@ExtendWith(MockitoExtension.class)
class GatewayTopologyJsonToExposedServicesConverterTest {

    private static final String TOPOLOGY_NAME = "topologyName";

    private static final String CLOUDERA_MANAGER_UI = "CM-UI";

    private static final String INVALID = "INVALID";

    @Mock
    private ExposedServiceListValidator exposedServiceListValidator;

    @InjectMocks
    private GatewayTopologyV4RequestToExposedServicesConverter underTest;

    @Test
    void testWithInvalidExposedService() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        gatewayTopologyJson.setExposedServices(Collections.singletonList(INVALID));
        when(exposedServiceListValidator.validate(anyList())).thenReturn(new ValidationResultBuilder().error(INVALID).build());

        assertThrows(BadRequestException.class, () -> underTest.convert(gatewayTopologyJson), INVALID);
    }

    @Test
    void testWithSingleExposedService() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName(TOPOLOGY_NAME);
        gatewayTopologyJson.setExposedServices(Collections.singletonList(CLOUDERA_MANAGER_UI));

        when(exposedServiceListValidator.validate(anyList())).thenReturn(new ValidationResultBuilder().build());

        ExposedServices exposedServices = underTest.convert(gatewayTopologyJson);

        assertEquals(1L, exposedServices.getServices().size());
        assertEquals(CLOUDERA_MANAGER_UI, exposedServices.getServices().get(0));
    }
}
