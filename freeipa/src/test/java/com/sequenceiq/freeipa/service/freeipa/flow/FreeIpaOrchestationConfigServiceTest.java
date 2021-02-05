package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.model.SaltConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.proxy.ProxyConfigService;
import com.sequenceiq.freeipa.service.tag.TagConfigService;
import com.sequenceiq.freeipa.service.telemetry.TelemetryConfigService;

@ExtendWith(MockitoExtension.class)
public class FreeIpaOrchestationConfigServiceTest {

    private static final String CLOUD_PLATFORM = "AWS";

    @Mock
    private FreeIpaConfigService freeIpaConfigService;

    @Mock
    private TelemetryConfigService telemetryConfigService;

    @Mock
    private ProxyConfigService proxyConfigService;

    @Mock
    private TagConfigService tagConfigService;

    @InjectMocks
    private FreeIpaOrchestrationConfigService underTest;

    @Test
    public void testGetSaltConfig() throws Exception {
        Stack stack = mock(Stack.class);
        FreeIpaConfigView freeIpaConfigView = mock(FreeIpaConfigView.class);
        Map freeIpaConfigViewMap = mock(Map.class);
        when(freeIpaConfigService.createFreeIpaConfigs(any(), any())).thenReturn(freeIpaConfigView);
        when(stack.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        when(freeIpaConfigView.toMap()).thenReturn(freeIpaConfigViewMap);
        when(telemetryConfigService.createTelemetryPillarConfig(any())).thenReturn(Map.of());
        when(proxyConfigService.createProxyPillarConfig(any())).thenReturn(Map.of());
        when(tagConfigService.createTagsPillarConfig(any())).thenReturn(Map.of());

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
    }
}
