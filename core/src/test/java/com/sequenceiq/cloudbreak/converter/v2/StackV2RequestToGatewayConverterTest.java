package com.sequenceiq.cloudbreak.converter.v2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.GeneralSettings;
import com.sequenceiq.cloudbreak.api.model.v2.StackV2Request;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

public class StackV2RequestToGatewayConverterTest {

    @Spy
    private GatewayConvertUtil convertUtil;

    @InjectMocks
    private StackV2RequestToGatewayConverter converter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldGenerateSignCertWhenConvertingFromStackV2Request() {
        GatewayJson gateWayJson =  new GatewayJson();
        StackV2Request source = generateStackV2Request(gateWayJson, "funnyCluster");
        Gateway result = converter.convert(source);
        assertTrue(result.getSignCert().startsWith("-----BEGIN CERTIFICATE-----"));
        assertTrue(result.getSignCert().endsWith("-----END CERTIFICATE-----\n"));
    }

    @Test
    public void shouldCreateCorrectSsoUrlWhenClusterNameisProvided() {
        GatewayJson gateWayJson =  new GatewayJson();
        StackV2Request source = generateStackV2Request(gateWayJson, "funnyCluster");
        Gateway result = converter.convert(source);
        assertEquals("/funnyCluster/sso/api/v1/websso", result.getSsoProvider());
    }

    private StackV2Request generateStackV2Request(GatewayJson gateWayJson, String clusterName) {
        AmbariV2Request ambariV2Request = new AmbariV2Request();
        ambariV2Request.setGateway(gateWayJson);
        ClusterV2Request clusterRequest = new ClusterV2Request();
        clusterRequest.setAmbari(ambariV2Request);
        GeneralSettings generalConfig = new GeneralSettings();
        generalConfig.setName(clusterName);
        StackV2Request source = new StackV2Request();
        source.setCluster(clusterRequest);
        source.setGeneral(generalConfig);
        return source;
    }

}