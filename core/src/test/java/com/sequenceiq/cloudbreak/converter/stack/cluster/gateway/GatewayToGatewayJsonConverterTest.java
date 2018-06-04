package com.sequenceiq.cloudbreak.converter.stack.cluster.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.model.GatewayType;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayTopologyJson;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.SSOType;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;

@RunWith(MockitoJUnitRunner.class)
public class GatewayToGatewayJsonConverterTest {

    private static final String PATH = "path";

    private static final String SSO_PROVIDER = "ssoProvider";

    private static final String TOKEN_CERT = "tokenCert";

    private static final String SIGN_KEY = "signKey";

    private static final String SIGN_CERT = "signCert";

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private final GatewayToGatewayJsonConverter underTest = new GatewayToGatewayJsonConverter();

    @Before
    public void setup() {
        when(conversionService.convert(any(GatewayTopology.class), eq(GatewayTopologyJson.class))).thenReturn(new GatewayTopologyJson());
    }

    @Test
    public void testConvert() {
        Gateway gateway = new Gateway();
        gateway.setPath(PATH);
        gateway.setSsoProvider(SSO_PROVIDER);
        gateway.setSsoType(SSOType.SSO_PROVIDER);
        gateway.setGatewayType(GatewayType.CENTRAL);
        gateway.setTokenCert(TOKEN_CERT);
        gateway.setSignKey(SIGN_KEY);
        gateway.setSignCert(SIGN_CERT);
        gateway.setTopologies(Sets.newHashSet(new GatewayTopology(), new GatewayTopology()));

        GatewayJson result = underTest.convert(gateway);

        assertEquals(SSOType.SSO_PROVIDER, result.getSsoType());
        assertEquals(TOKEN_CERT, result.getTokenCert());
        assertEquals(GatewayType.CENTRAL, result.getGatewayType());
        assertTrue(CollectionUtils.isEmpty(result.getExposedServices()));
        assertTrue(StringUtils.isEmpty(result.getTopologyName()));
        assertEquals(SSO_PROVIDER, result.getSsoProvider());
        assertEquals(PATH, result.getPath());
        assertEquals(2, result.getTopologies().size());
    }
}