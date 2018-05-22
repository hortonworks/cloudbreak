package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.ExposedService;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@RunWith(MockitoJUnitRunner.class)
public class GatewayJsonToGatewayConverterTest {

    private static final String DEPRECATED_TOPOLOGY = "deprecated topology";

    private static final String PATH = "path";

    private static final String SSO_PROVIDER = "ssoProvider";

    private static final String TOKEN_CERT = "tokenCert";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private final GatewayJsonToGatewayConverter underTest = new GatewayJsonToGatewayConverter();

    @Test
    public void testConvertSsoType() {
        GatewayJson source = new GatewayJson();

        Gateway result = underTest.convert(source);

        assertEquals(SSOType.NONE, result.getSsoType());
    }

    @Test
    public void testConvertBasicProperties() {
        GatewayJson source = new GatewayJson();
        source.setPath(PATH);
        source.setSsoProvider(SSO_PROVIDER);
        source.setSsoType(SSOType.SSO_PROVIDER);
        source.setTokenCert(TOKEN_CERT);
        source.setGatewayType(GatewayType.CENTRAL);

        Gateway result = underTest.convert(source);

        // path is converted by ClusterRequestToClusterConverter because it depends on the Cluster's properties
        assertNull(result.getPath());
        // ssoProvider is converted by ClusterRequestToClusterConverter because it depends on the Cluster's properties
        assertNull(result.getSsoProvider());
        assertEquals(SSOType.SSO_PROVIDER, result.getSsoType());
        assertEquals(TOKEN_CERT, result.getTokenCert());
        assertEquals(GatewayType.CENTRAL, result.getGatewayType());
    }

    @Test
    public void testConvertLegacyTopologyWithoutAnyService() {
        GatewayJson source = new GatewayJson();
        source.setEnableGateway(true);
        source.setTopologyName(DEPRECATED_TOPOLOGY);

        Gateway result = underTest.convert(source);
        assertEquals(DEPRECATED_TOPOLOGY, result.getTopologies().iterator().next().getTopologyName());
    }

    @Test
    public void testConvertNewTopologies() {
        GatewayJson source = new GatewayJson();
        GatewayTopologyJson topology1 = new GatewayTopologyJson();
        topology1.setTopologyName("topology1");
        source.setTopologies(Collections.singletonList(topology1));
        when(conversionService.convert(any(GatewayTopologyJson.class), eq(GatewayTopology.class))).thenReturn(new GatewayTopology());

        Gateway result = underTest.convert(source);

        assertEquals(1, result.getTopologies().size());
    }

    @Test
    public void testConvertLegacyGatewayWithAllExposed() {
        GatewayJson source = new GatewayJson();
        source.setEnableGateway(true);
        source.setTopologyName("topology1");
        source.setExposedServices(Collections.singletonList(ExposedService.ALL.getServiceName()));
        when(conversionService.convert(any(GatewayTopologyJson.class), eq(GatewayTopology.class))).thenReturn(new GatewayTopology());

        Gateway result = underTest.convert(source);

        GatewayTopology resultTopology = result.getTopologies().iterator().next();
        assertNotNull(resultTopology);
    }
}