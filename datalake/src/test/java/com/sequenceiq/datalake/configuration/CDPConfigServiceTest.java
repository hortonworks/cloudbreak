package com.sequenceiq.datalake.configuration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class CDPConfigServiceTest {

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
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void cdpEnableStackRequests() {
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(true);
        when(supportedRuntimes.contains("7.2.12")).thenReturn(false);
        cdpConfigService.initCdpStackRequests();
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void cdpEnableEveryStackRequests() {
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(true);
        cdpConfigService.initCdpStackRequests();
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.2.12")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, "7.1.0")));
    }

    @Test
    void testEmptyAdvertisedRuntimes() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", "7.1.0");

        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(false);
        when(supportedRuntimes.contains("7.2.12")).thenReturn(true);
        when(supportedRuntimes.contains("7.1.0")).thenReturn(true);

        when(advertisedRuntimes.isEmpty()).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt("7.2.12", false), rt("7.1.0", true));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testFilteredDlVersions() {
        List<String> supportedVersions = List.of("7.2.6", "7.2.12");
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        assertArrayEquals(supportedVersions.toArray(), cdpConfigService.getDatalakeVersions("").toArray());
    }

    @Test
    void testFilteredDlVersionsForGcp() {
        List<String> supportedVersions = List.of("7.2.6", "7.2.2");
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        List<String> gcpSupportedVersions = List.of("7.2.6");
        assertArrayEquals("Templates are only available from 7.2.6",
                gcpSupportedVersions.toArray(), cdpConfigService.getDatalakeVersions("GCP").toArray());
    }

    @Test
    void testFilteredDlVersionsForAws() {
        List<String> supportedVersions = List.of("7.2.6", "7.2.12");
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        List<String> gcpSupportedVersions = List.of("7.2.6", "7.2.12");
        assertArrayEquals(gcpSupportedVersions.toArray(), cdpConfigService.getDatalakeVersions("AWS").toArray());
    }

    @Test
    void testAdvertisedRuntimes() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", "7.1.0");

        List<String> supportedVersions = List.of("7.1.0", "7.2.12");
        mockSupportedRuntimes(supportedVersions);

        when(advertisedRuntimes.isEmpty()).thenReturn(false);
        when(advertisedRuntimes.contains("7.2.12")).thenReturn(false);
        when(advertisedRuntimes.contains("7.1.0")).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt("7.1.0", true));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testAdvertisedRuntimesForGcp() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", "7.2.6");

        List<String> supportedVersions = List.of("7.2.6", "7.2.2");
        mockSupportedRuntimes(supportedVersions);

        List<String> advertisedVersions = List.of("7.2.6");
        mockAdvertisedRuntimes(advertisedVersions);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt("7.2.6", true));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes("GCP");

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    private void mockAdvertisedRuntimes(List<String> advertisedVersions) {
        when(advertisedRuntimes.isEmpty()).thenReturn(false);
        when(advertisedRuntimes.contains(anyString())).thenAnswer(invocation -> {
            if (advertisedVersions.contains(invocation.getArgument(0))) {
                return true;
            }
            return false;
        });
    }

    private void mockSupportedRuntimes(List<String> supportedVersions) {
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(anyString())).thenAnswer(invocation -> {
            if (supportedVersions.contains(invocation.getArgument(0))) {
                return true;
            }
            return false;
        });
    }

    private AdvertisedRuntime rt(String version, boolean defaultVersion) {
        AdvertisedRuntime advertisedRuntime = new AdvertisedRuntime();
        advertisedRuntime.setRuntimeVersion(version);
        advertisedRuntime.setDefaultRuntimeVersion(defaultVersion);
        return advertisedRuntime;
    }
}