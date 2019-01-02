package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.conf.ConversionConfig;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.StackV4RequestToGatewayConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {StackV4RequestToGatewayConverter.class, ConversionConfig.class, GatewayConvertUtil.class,
        GatewayV4RequestValidator.class})
public class StackV4RequestToGatewayConverterTest {

    @Inject
    private StackV4RequestToGatewayConverter converter;

    @Test(expected = BadRequestException.class)
    public void testWithInvalidGatewayRequest() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        StackV4Request source = generateStackV2Request(gatewayJson);
        converter.convert(source);
    }

    @Test
    public void shouldGenerateSignCertWhenConvertingFromStackV2Request() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        StackV4Request source = generateStackV2Request(gatewayJson);
        Gateway result = converter.convert(source);
        assertTrue(result.getSignCert().startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue(result.getSignCert().endsWith("-----END CERTIFICATE-----\n"));
    }

    @Test
    public void shouldCreateCorrectSsoUrlWhenClusterNameisProvided() {
        GatewayV4Request gatewayJson = new GatewayV4Request();
        StackV4Request source = generateStackV2Request(gatewayJson);
        Gateway result = converter.convert(source);
        assertEquals("/funnyCluster/sso/api/v1/websso", result.getSsoProvider());
    }

    private StackV4Request generateStackV2Request(GatewayV4Request gateWayJson) {
        AmbariV4Request ambariV2Request = new AmbariV4Request();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        clusterRequest.setGateway(gateWayJson);
        clusterRequest.setAmbari(ambariV2Request);
        StackV4Request source = new StackV4Request();
        source.setName("funnyCluster");
        source.setCluster(clusterRequest);
        return source;
    }
}