package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.ccm.cloudinit.CcmConnectivityParameters;
import com.sequenceiq.cloudbreak.ccm.cloudinit.DefaultCcmV2JumpgateParameters;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.proxy.ProxyConfigService;
import com.sequenceiq.freeipa.service.tag.TagConfigService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryConfigService;
import com.sequenceiq.freeipa.service.upgrade.ccm.CcmParametersConfigService;

@ExtendWith(MockitoExtension.class)
class SaltConfigProviderTest {

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String PILLARPATH = "pillarpath";

    private static final String PILLARKEY = "pillarkey";

    private static final String PILLARVALUE = "pillarvalue";

    private static final String PILLAR = "pillar";

    private static final String ENV_CRN = "envCrn";

    @Mock
    private FreeIpaConfigService freeIpaConfigService;

    @Mock
    private TelemetryConfigService telemetryConfigService;

    @Mock
    private ProxyConfigService proxyConfigService;

    @Mock
    private TagConfigService tagConfigService;

    @Mock
    private CcmParametersConfigService ccmParametersConfigService;

    @InjectMocks
    private SaltConfigProvider underTest;

    @Test
    public void testGetSaltConfig() throws Exception {
        Stack stack = mock(Stack.class);
        when(stack.getCcmParameters()).thenReturn(new CcmConnectivityParameters(new DefaultCcmV2JumpgateParameters()));
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        FreeIpaConfigView freeIpaConfigView = mock(FreeIpaConfigView.class);
        Map freeIpaConfigViewMap = mock(Map.class);
        when(freeIpaConfigService.createFreeIpaConfigs(any(), any())).thenReturn(freeIpaConfigView);
        when(stack.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(freeIpaConfigView.toMap()).thenReturn(freeIpaConfigViewMap);
        when(telemetryConfigService.createTelemetryPillarConfig(any())).thenReturn(Map.of());
        when(proxyConfigService.createProxyPillarConfig(any())).thenReturn(Map.of());
        when(tagConfigService.createTagsPillarConfig(any())).thenReturn(Map.of());
        when(ccmParametersConfigService.createCcmParametersPillarConfig(eq(ENV_CRN), any())).thenReturn(
                Map.of(PILLAR, new SaltPillarProperties(PILLARPATH, Map.of(PILLARKEY, PILLARVALUE))));
        SaltConfig saltConfig = underTest.getSaltConfig(stack, Set.of());

        Map<String, SaltPillarProperties> servicePillarConfig = saltConfig.getServicePillarConfig();
        assertNotNull(servicePillarConfig);

        SaltPillarProperties freeIpaProperties = servicePillarConfig.get("freeipa");
        assertNotNull(freeIpaProperties);
        assertEquals("/freeipa/init.sls", freeIpaProperties.getPath());
        assertEquals(freeIpaConfigViewMap, freeIpaProperties.getProperties().get("freeipa"));

        SaltPillarProperties discoveryProperties = servicePillarConfig.get("discovery");
        assertNotNull(discoveryProperties);
        assertEquals("/discovery/init.sls", discoveryProperties.getPath());
        assertEquals(CLOUD_PLATFORM, discoveryProperties.getProperties().get("platform"));

        SaltPillarProperties pillarProperties = servicePillarConfig.get(PILLAR);
        assertNotNull(pillarProperties);
        assertEquals(PILLARPATH, pillarProperties.getPath());
        assertEquals(PILLARVALUE, pillarProperties.getProperties().get(PILLARKEY));
    }
}
