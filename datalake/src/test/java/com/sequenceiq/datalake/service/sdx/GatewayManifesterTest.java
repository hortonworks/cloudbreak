package com.sequenceiq.datalake.service.sdx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;

@ExtendWith(MockitoExtension.class)
@DisplayName("SDX gateway provider service tests")
public class GatewayManifesterTest {

    @InjectMocks
    private GatewayManifester underTest;

    @BeforeEach
    void initMocks() {
        ReflectionTestUtils.setField(underTest, "defaultSsoType", SSOType.SSO_PROVIDER_FROM_UMS);

        MockitoAnnotations.initMocks(this);
    }

    @Test
    void prepareGatewayWhenRequestedHasNullValueForGatewayShouldReturnWithFullGateConfig() {
        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(new ClusterV4Request());

        StackV4Request result = underTest.configureGatewayForSdxCluster(stackV4Request);

        assertNotNull(result.getCluster());
        assertNotNull(result.getCluster().getGateway());
        assertEquals(result.getCluster().getGateway().getSsoType(), SSOType.SSO_PROVIDER_FROM_UMS);
    }

    @Test
    void prepareGatewayWhenRequestedHasValueForGatewayShouldReturnWithoutModifications() {
        GatewayV4Request gatewayV4Request = getGatewayV4Request();

        StackV4Request stackV4Request = new StackV4Request();
        stackV4Request.setCluster(new ClusterV4Request());
        stackV4Request.getCluster().setGateway(gatewayV4Request);

        underTest.configureGatewayForSdxCluster(stackV4Request);

        assertEquals(stackV4Request.getCluster().getGateway(), gatewayV4Request);
    }

    private GatewayV4Request getGatewayV4Request() {
        GatewayV4Request gatewayV4Request = new GatewayV4Request();

        GatewayTopologyV4Request gatewayTopologyV4Request = new GatewayTopologyV4Request();
        gatewayTopologyV4Request.setTopologyName("soo-deep");
        gatewayTopologyV4Request.setExposedServices(List.of("ALL"));
        gatewayV4Request.setSsoType(SSOType.SSO_PROVIDER);

        gatewayV4Request.setTopologies(List.of(gatewayTopologyV4Request));

        return gatewayV4Request;
    }

}