package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.config.ConversionConfig;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyV4RequestValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.StackV4RequestToGatewayConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToGatewayTopologyConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.exception.BadRequestException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {StackV4RequestToGatewayConverter.class, ConversionConfig.class, GatewayConvertUtil.class, GatewayV4RequestValidator.class,
        ConverterUtil.class, GatewayTopologyV4RequestToGatewayTopologyConverter.class, GatewayTopologyV4RequestValidator.class,
        ExposedServiceListValidator.class})
public class StackV4RequestToGatewayConverterTest {

    @Inject
    private StackV4RequestToGatewayConverter converter;

    @Test(expected = BadRequestException.class)
    public void testWithInvalidGatewayRequest() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        StackV4Request source = generateStackV4Request(gatewayJson);
        converter.convert(source);
    }

    @Test
    public void shouldCreateCorrectSsoUrlWhenClusterNameisProvided() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        gatewayJson.setPath("funnyPath");
        gatewayJson.setTopologies(Arrays.asList(getGatewayTopologyV4Request()));
        StackV4Request source = generateStackV4Request(gatewayJson);
        Gateway result = converter.convert(source);
        assertEquals("/funnyPath/sso/api/v1/websso", result.getSsoProvider());
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