package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.topology.GatewayTopologyV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.GatewayToGatewayV4RequestConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.cli.GatewayTopologyToGatewayTopologyV4RequestConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@ExtendWith(MockitoExtension.class)
class GatewayToGatewayJsonConverterTest {

    private static final String PATH = "path";

    private static final String SSO_PROVIDER = "ssoProvider";

    private static final String TOKEN_CERT = "tokenCert";

    private static final String SIGN_KEY = "signKey";

    private static final String SIGN_CERT = "signCert";

    @InjectMocks
    private final GatewayToGatewayV4RequestConverter underTest = new GatewayToGatewayV4RequestConverter();

    @Mock
    private GatewayTopologyToGatewayTopologyV4RequestConverter gatewayTopologyToGatewayTopologyV4RequestConverter;

    @BeforeEach
    void setup() {
        when(gatewayTopologyToGatewayTopologyV4RequestConverter.convert(any(GatewayTopology.class))).thenReturn(new GatewayTopologyV4Request());
    }

    @Test
    void testConvert() {
        Gateway gateway = new Gateway();
        gateway.setPath(PATH);
        gateway.setSsoProvider(SSO_PROVIDER);
        gateway.setSsoType(SSOType.SSO_PROVIDER);
        gateway.setGatewayType(GatewayType.CENTRAL);
        gateway.setTokenCert(TOKEN_CERT);
        gateway.setSignKey(SIGN_KEY);
        gateway.setSignCert(SIGN_CERT);
        gateway.setTopologies(Sets.newHashSet(new GatewayTopology(), new GatewayTopology()));

        GatewayV4Request result = underTest.convert(gateway);

        assertEquals(SSOType.SSO_PROVIDER, result.getSsoType());
        assertEquals(TOKEN_CERT, result.getTokenCert());
        assertEquals(GatewayType.CENTRAL, result.getGatewayType());
        assertEquals(result.getTopologies().size(), 2);
        assertEquals(SSO_PROVIDER, result.getSsoProvider());
        assertEquals(PATH, result.getPath());
        assertEquals(2L, result.getTopologies().size());
    }
}