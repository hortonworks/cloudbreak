package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.StackV4RequestToGatewayConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
class StackV4RequestToGatewayConverterTest {

    private static final Set<GatewayTopology> GATEWAY_TOPOLOGY = Set.of(new GatewayTopology());

    @Mock
    private GatewayConvertUtil convertUtil;

    @Mock
    private GatewayV4RequestValidator gatewayJsonValidator;

    @InjectMocks
    private StackV4RequestToGatewayConverter underTest;

    @Test
    void testWithInvalidGatewayRequest() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        StackV4Request source = generateStackV4Request(gatewayJson);
        when(gatewayJsonValidator.validate(gatewayJson)).thenReturn(ValidationResult.builder().error("invalid").build());
        assertThrows(BadRequestException.class, () -> underTest.convert(source));
    }

    @Test
    void shouldCreateCorrectSsoUrlWhenClusterNameisProvided() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        gatewayJson.setPath("funnyPath");
        gatewayJson.setTopologies(Arrays.asList(getGatewayTopologyV4Request()));
        StackV4Request source = generateStackV4Request(gatewayJson);
        when(gatewayJsonValidator.validate(gatewayJson)).thenReturn(ValidationResult.builder().build());
        doAnswer(i -> {
            Gateway gw = i.getArgument(1);
            gw.setSsoProvider("SSOPROVIDER");
            gw.setPath(gatewayJson.getPath());
            gw.setTopologies(GATEWAY_TOPOLOGY);
            gw.setGatewayType(GatewayType.CENTRAL);
            return null;
        }).when(convertUtil).setBasicProperties(eq(gatewayJson), any(Gateway.class));

        Gateway result = underTest.convert(source);

        assertEquals("SSOPROVIDER", result.getSsoProvider());
        assertEquals("funnyPath", result.getPath());
        assertTrue(EqualsBuilder.reflectionEquals(GATEWAY_TOPOLOGY, result.getTopologies()));
        assertEquals(GatewayType.CENTRAL, result.getGatewayType());
    }

    private GatewayTopologyV4Request getGatewayTopologyV4Request() {
        GatewayTopologyV4Request gatewayTopologyV4Request = new GatewayTopologyV4Request();
        gatewayTopologyV4Request.setTopologyName("topology-name");
        return gatewayTopologyV4Request;
    }

    private StackV4Request generateStackV4Request(GatewayV4Request gateWayJson) {
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setGateway(gateWayJson);
        StackV4Request source = new StackV4Request();
        source.setName("funnyCluster");
        source.setCluster(clusterRequest);
        return source;
    }
}
