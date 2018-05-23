package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

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

import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@RunWith(MockitoJUnitRunner.class)
public class ClusterRequestToGatewayConverterTest {

    private static final String DEPRECATED_TOPOLOGY = "deprecated topology";

    private static final String PATH = "path";

    private static final String SSO_PROVIDER = "ssoProvider";

    private static final String TOKEN_CERT = "tokenCert";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ConversionService conversionService;

    @Spy
    private GatewayConvertUtil gatewayConvertUtil;

    @InjectMocks
    private final ClusterRequestToGatewayConverter underTest = new ClusterRequestToGatewayConverter();

    @Test
    public void testConvertSsoType() {
        GatewayJson source = new GatewayJson();

        Gateway result = underTest.convert(generateClusterRequest(source));

        assertEquals(SSOType.NONE, result.getSsoType());
    }

    private ClusterRequest generateClusterRequest(GatewayJson source) {
        ClusterRequest cluster = new ClusterRequest();
        cluster.setGateway(source);
        return cluster;
    }

    @Test
    public void testConvertBasicProperties() {
        GatewayJson source = new GatewayJson();
        source.setPath(PATH);
        source.setSsoProvider(SSO_PROVIDER);
        source.setSsoType(SSOType.SSO_PROVIDER);
        source.setTokenCert(TOKEN_CERT);
        source.setGatewayType(GatewayType.CENTRAL);

        Gateway result = underTest.convert(generateClusterRequest(source));

        assertEquals(SSOType.SSO_PROVIDER, result.getSsoType());
        assertEquals(TOKEN_CERT, result.getTokenCert());
        assertEquals(GatewayType.CENTRAL, result.getGatewayType());
    }

    @Test
    public void testConvertLegacyTopologyWithoutAnyService() {
        GatewayJson source = new GatewayJson();
        source.setEnableGateway(true);
        source.setTopologyName(DEPRECATED_TOPOLOGY);

        Gateway result = underTest.convert(generateClusterRequest(source));
        assertEquals(DEPRECATED_TOPOLOGY, result.getTopologies().iterator().next().getTopologyName());
    }

    @Test
    public void testConvertNewTopologies() {
        GatewayJson source = new GatewayJson();
        GatewayTopologyJson topology1 = new GatewayTopologyJson();
        topology1.setTopologyName("topology1");
        source.setTopologies(Collections.singletonList(topology1));
        doNothing().when(gatewayConvertUtil).setTopologyList(any(), any());
        underTest.convert(generateClusterRequest(source));

        verify(gatewayConvertUtil).setTopologyList(any(), any());
    }

    @Test
    public void testConvertLegacyGatewayWithAllExposed() {
        GatewayJson source = new GatewayJson();
        source.setEnableGateway(true);
        source.setTopologyName("topology1");
        source.setExposedServices(Collections.singletonList(ExposedService.ALL.getServiceName()));
        doNothing().when(gatewayConvertUtil).setLegacyTopology(any(), any(), any());
        underTest.convert(generateClusterRequest(source));

        verify(gatewayConvertUtil).setLegacyTopology(any(), any(), any());
    }
}