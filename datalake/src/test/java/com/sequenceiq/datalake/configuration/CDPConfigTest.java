package com.sequenceiq.datalake.configuration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class CDPConfigTest {

    @Spy
    private Set<String> advertisedRuntimes;

    @Spy
    private Set<String> supportedRuntimes;

    @InjectMocks
    private CDPConfigService cdpConfigService;

    @Test
    void cdpStackRequests() {
        when(supportedRuntimes.isEmpty()).thenReturn(true);
        cdpConfigService.initCdpStackRequests();
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void cdpEnableStackRequests() {
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains("7.0.2")).thenReturn(false);
        when(supportedRuntimes.contains("7.1.0")).thenReturn(true);
        cdpConfigService.initCdpStackRequests();
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void cdpEnableEveryStackRequests() {
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains("7.0.2")).thenReturn(true);
        when(supportedRuntimes.contains("7.1.0")).thenReturn(true);
        cdpConfigService.initCdpStackRequests();
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.0.2")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.MEDIUM_DUTY_HA, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void testEmptyAdvertisedRuntimes() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", "7.1.0");

        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains("7.0.2")).thenReturn(true);
        when(supportedRuntimes.contains("7.1.0")).thenReturn(true);

        when(advertisedRuntimes.isEmpty()).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt("7.0.2", false), rt("7.1.0", true));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes();

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testAdvertisedRuntimes() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", "7.1.0");

        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains("7.0.2")).thenReturn(true);
        when(supportedRuntimes.contains("7.1.0")).thenReturn(true);

        when(advertisedRuntimes.isEmpty()).thenReturn(false);
        when(advertisedRuntimes.contains("7.0.2")).thenReturn(false);
        when(advertisedRuntimes.contains("7.1.0")).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt("7.1.0", true));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes();

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    private AdvertisedRuntime rt(String version, boolean defaultVersion) {
        AdvertisedRuntime advertisedRuntime = new AdvertisedRuntime();
        advertisedRuntime.setRuntimeVersion(version);
        advertisedRuntime.setDefaultRuntimeVersion(defaultVersion);
        return advertisedRuntime;
    }
}