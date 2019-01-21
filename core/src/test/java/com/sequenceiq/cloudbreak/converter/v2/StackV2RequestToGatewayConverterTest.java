package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.conf.ConversionConfig;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.StackV4RequestToGatewayConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {StackV4RequestToGatewayConverter.class, ConversionConfig.class, GatewayConvertUtil.class,
        GatewayV4RequestValidator.class})
public class StackV2RequestToGatewayConverterTest {

    @Inject
    private StackV4RequestToGatewayConverter converter;

    @Test(expected = BadRequestException.class)
    public void testWithInvalidGatewayRequest() {
        GatewayJson gatewayJson = new GatewayJson();
        StackV2Request source = generateStackV2Request(gatewayJson);
        converter.convert(source);
    }

    @Test
    public void shouldGenerateSignCertWhenConvertingFromStackV2Request() {
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setTopologyName("anyName");
        StackV2Request source = generateStackV2Request(gatewayJson);
        Gateway result = converter.convert(source);
        assertTrue(result.getSignCert().startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue(result.getSignCert().endsWith("-----END CERTIFICATE-----\n"));
    }

    @Test
    public void shouldCreateCorrectSsoUrlWhenClusterNameisProvided() {
        GatewayJson gatewayJson = new GatewayJson();
        gatewayJson.setTopologyName("anyName");
        StackV2Request source = generateStackV2Request(gatewayJson);
        Gateway result = converter.convert(source);
        assertEquals("/funnyCluster/sso/api/v1/websso", result.getSsoProvider());
    }

    private StackV2Request generateStackV2Request(GatewayJson gateWayJson) {
        AmbariV2Request ambariV2Request = new AmbariV2Request();
        ambariV2Request.setGateway(gateWayJson);
        ClusterV2Request clusterRequest = new ClusterV2Request();
        clusterRequest.setAmbari(ambariV2Request);
        GeneralSettings generalConfig = new GeneralSettings();
        generalConfig.setName("funnyCluster");
        StackV2Request source = new StackV2Request();
        source.setCluster(clusterRequest);
        source.setGeneral(generalConfig);
        return source;
    }
}