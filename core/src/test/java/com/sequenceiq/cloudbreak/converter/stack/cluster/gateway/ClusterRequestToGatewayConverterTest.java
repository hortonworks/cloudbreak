package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.conf.ConversionConfig;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.ExposedServiceListValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {ClusterRequestToGatewayConverter.class, ConversionConfig.class, GatewayConvertUtil.class,
        GatewayV4RequestValidator.class, GatewayTopologyJsonToGatewayTopologyConverter.class, GatewayTopologyV4RequestValidator.class,
        ExposedServiceListValidator.class})
public class ClusterRequestToGatewayConverterTest {

    private static final String DEPRECATED_TOPOLOGY = "deprecated topology";

    private static final String PATH = "path";

    private static final String SSO_PROVIDER = "ssoProvider";

    private static final String TOKEN_CERT = "tokenCert";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Inject
    private ClusterRequestToGatewayConverter underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertSsoType() {
        GatewayJson source = new GatewayJson();
        source.setTopologies(getTopologies());

        Gateway result = underTest.convert(generateClusterRequest(source));

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
        source.setTopologies(getTopologies());

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

        Gateway result = underTest.convert(generateClusterRequest(source));

        assertFalse(result.getTopologies().isEmpty());
    }

    @Test
    public void testConvertLegacyGatewayWithoutExposed() {
        GatewayJson source = new GatewayJson();
        source.setEnableGateway(true);
        source.setTopologyName(DEPRECATED_TOPOLOGY);
        Gateway result = underTest.convert(generateClusterRequest(source));

        assertEquals(result.getTopologies().iterator().next().getTopologyName(), DEPRECATED_TOPOLOGY);
    }

    @Test(expected = BadRequestException.class)
    public void testThrowsExceptionWhenRequestIsInvalid() {
        GatewayJson source = new GatewayJson();

        underTest.convert(generateClusterRequest(source));
    }

    @Test
    public void testWithFalseGatewayEnabled() {
        GatewayJson source = new GatewayJson();
        source.setEnableGateway(false);
        source.setTopologyName(DEPRECATED_TOPOLOGY);
        Gateway result = underTest.convert(generateClusterRequest(source));

        assertNull(result);
    }

    @Test
    public void testWithNullGatewayEnabled() {
        GatewayJson source = new GatewayJson();
        source.setEnableGateway(null);
        source.setTopologyName(DEPRECATED_TOPOLOGY);
        Gateway result = underTest.convert(generateClusterRequest(source));

        assertNotNull(result);
        assertEquals(DEPRECATED_TOPOLOGY, result.getTopologies().iterator().next().getTopologyName());
    }

    @Test
    public void testWithEnableFalseAndDefinedTopologyInList() {
        GatewayJson source = new GatewayJson();
        source.setEnableGateway(false);
        source.setTopologies(getTopologies());
        Gateway result = underTest.convert(generateClusterRequest(source));

        assertNotNull(result);
        assertEquals("topologyName", result.getTopologies().iterator().next().getTopologyName());
    }

    private List<GatewayTopologyJson> getTopologies() {
        GatewayTopologyJson gatewayTopologyJson = new GatewayTopologyJson();
        gatewayTopologyJson.setTopologyName("topologyName");
        return Collections.singletonList(gatewayTopologyJson);
    }

    private ClusterRequest generateClusterRequest(GatewayJson source) {
        ClusterRequest cluster = new ClusterRequest();
        cluster.setGateway(source);
        return cluster;
    }
}