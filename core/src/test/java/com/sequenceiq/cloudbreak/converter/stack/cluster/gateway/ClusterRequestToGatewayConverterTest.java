package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.config.ConversionConfig;
import com.sequenceiq.cloudbreak.conf.ConverterMockProvider;
import com.sequenceiq.cloudbreak.conf.RepositoryMockProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayTopologyV4RequestValidator;
import com.sequenceiq.cloudbreak.controller.validation.stack.cluster.gateway.GatewayV4RequestValidator;
import com.sequenceiq.cloudbreak.converter.util.GatewayConvertUtil;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.GatewayV4RequestToGatewayConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToExposedServicesConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway.topology.GatewayTopologyV4RequestToGatewayTopologyConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GatewayV4RequestToGatewayConverter.class, ConversionConfig.class, GatewayConvertUtil.class,
        GatewayV4RequestValidator.class, GatewayTopologyV4RequestToGatewayTopologyConverter.class,
        GatewayTopologyV4RequestValidator.class,
        GatewayTopologyV4RequestToExposedServicesConverter.class,
        ConverterMockProvider.class, RepositoryMockProvider.class})
public class ClusterRequestToGatewayConverterTest {

    private static final String PATH = "path";

    private static final String SSO_PROVIDER = "ssoProvider";

    private static final String TOKEN_CERT = "tokenCert";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Inject
    private GatewayV4RequestToGatewayConverter underTest;

    @Test
    public void testConvertSsoType() {
        GatewayV4Request source = new GatewayV4Request();
        source.setTopologies(getTopologies());

        Gateway result = underTest.convert(source);

        assertEquals(SSOType.NONE, result.getSsoType());
    }

    @Test
    public void testConvertBasicProperties() {
        GatewayV4Request source = new GatewayV4Request();
        source.setPath(PATH);
        source.setSsoProvider(SSO_PROVIDER);
        source.setSsoType(SSOType.SSO_PROVIDER);
        source.setTokenCert(TOKEN_CERT);
        source.setGatewayType(GatewayType.CENTRAL);
        source.setTopologies(getTopologies());

        Gateway result = underTest.convert(source);

        assertEquals(SSOType.SSO_PROVIDER, result.getSsoType());
        assertEquals(TOKEN_CERT, result.getTokenCert());
        assertEquals(GatewayType.CENTRAL, result.getGatewayType());
    }

    @Test
    public void testConvertNewTopologies() {
        GatewayV4Request source = new GatewayV4Request();
        GatewayTopologyV4Request topology1 = new GatewayTopologyV4Request();
        topology1.setTopologyName("topology1");
        source.setTopologies(Collections.singletonList(topology1));

        Gateway result = underTest.convert(source);

        assertFalse(result.getTopologies().isEmpty());
    }

    @Test(expected = BadRequestException.class)
    public void testThrowsExceptionWhenRequestIsInvalid() {
        GatewayV4Request source = new GatewayV4Request();

        underTest.convert(source);
    }

    @Test
    public void testWithEnableFalseAndDefinedTopologyInList() {
        GatewayV4Request source = new GatewayV4Request();
        source.setTopologies(getTopologies());
        Gateway result = underTest.convert(source);

        assertNotNull(result);
        assertEquals("topologyName", result.getTopologies().iterator().next().getTopologyName());
    }

    private List<GatewayTopologyV4Request> getTopologies() {
        GatewayTopologyV4Request gatewayTopologyJson = new GatewayTopologyV4Request();
        gatewayTopologyJson.setTopologyName("topologyName");
        return Collections.singletonList(gatewayTopologyJson);
    }
}