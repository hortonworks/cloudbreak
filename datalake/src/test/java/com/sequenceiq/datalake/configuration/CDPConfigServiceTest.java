package com.sequenceiq.datalake.configuration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.configuration.overlay.RuntimeDutyOverlayLoader;
import com.sequenceiq.datalake.service.imagecatalog.ImageCatalogService;
import com.sequenceiq.datalake.service.sdx.CDPConfigKey;
import com.sequenceiq.sdx.api.model.AdvertisedRuntime;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class CDPConfigServiceTest {
    private static final String USER_CRN = "crn:altus:iam:us-west-1:test-aws:user:cloudbreak@hortonworks.com";

    private static final String RUNTIME_710 = "7.1.0";

    private static final String RUNTIME_722 = "7.2.2";

    private static final String RUNTIME_726 = "7.2.6";

    private static final String RUNTIME_729 = "7.2.9";

    private static final String RUNTIME_7212 = "7.2.12";

    private static final String RUNTIME_7214 = "7.2.14";

    private static final String RUNTIME_7215 = "7.2.15";

    private static final String RUNTIME_7216 = "7.2.16";

    private static final String RUNTIME_7217 = "7.2.17";

    private static final String RUNTIME_7218 = "7.2.18";

    private static final String RUNTIME_731 = "7.3.1";

    private static final String RUNTIME_732 = "7.3.2";

    @Spy
    private Set<String> advertisedRuntimes;

    @Spy
    private Set<String> supportedRuntimes;

    @InjectMocks
    private CDPConfigService cdpConfigService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ProviderPreferencesService preferencesService;

    @Mock
    private CommonGovService commonGovService;

    @Mock
    private RuntimeDutyOverlayLoader runtimeDutyOverlayLoader;

    @BeforeEach
    public void setupAll() {
        lenient().when(entitlementService.isEntitledFor(anyString(), eq(Entitlement.CDP_DATA_LAKE_MEDIUM_DUTY_WITH_PROFILER))).thenReturn(Boolean.TRUE);
        lenient().when(runtimeDutyOverlayLoader.materializeOverlayDuties(any())).thenReturn(Map.of());
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_710);
    }

    @Test
    void overlayRuntimesAreMergedAndSurfaced() {
        ReflectionTestUtils.setField(cdpConfigService, "runtimeOverlayEnabled", true);
        CDPConfigKey overlayKey = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, "7.3.4");
        // A realistic materialized duty (the raw JSON CDPConfigService stores for on-disk versions), not a bare "{}":
        // this proves the loader's output round-trips into a StackV4Request exactly as an on-disk template does.
        when(runtimeDutyOverlayLoader.materializeOverlayDuties(any()))
                .thenReturn(Map.of(overlayKey, "{\"cluster\":{\"blueprintName\":\"7.3.4 - Overlay Medium Duty HA\"}}"));
        when(supportedRuntimes.isEmpty()).thenReturn(true);
        when(advertisedRuntimes.isEmpty()).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        StackV4Request materialized = cdpConfigService.getConfigForKey(overlayKey);
        assertNotNull(materialized, "an overlay-materialized runtime must be looked up like any on-disk one");
        assertEquals("7.3.4 - Overlay Medium Duty HA", materialized.getCluster().getBlueprintName(),
                "the overlay's raw JSON must round-trip into the StackV4Request body, not just resolve to a non-null shell");
        assertTrue(cdpConfigService.getAdvertisedRuntimes(null, null, false).stream()
                        .anyMatch(runtime -> "7.3.4".equals(runtime.getRuntimeVersion())),
                "the overlay runtime version must be advertised");
    }

    @Test
    void onDiskDutyWinsOverAnOverlayOfTheSameKey() {
        ReflectionTestUtils.setField(cdpConfigService, "runtimeOverlayEnabled", true);
        // The overlay is keyed at a runtime/shape that already has a full on-disk duty (AWS LIGHT_DUTY 7.2.12).
        // The merge uses putIfAbsent, so the on-disk template must win and the overlay body must be discarded.
        CDPConfigKey collidingKey = new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212);
        when(runtimeDutyOverlayLoader.materializeOverlayDuties(any()))
                .thenReturn(Map.of(collidingKey, "{\"cluster\":{\"blueprintName\":\"OVERLAY-SHOULD-NOT-WIN\"}}"));
        when(supportedRuntimes.isEmpty()).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        StackV4Request retained = cdpConfigService.getConfigForKey(collidingKey);
        assertNotNull(retained, "the on-disk AWS LIGHT_DUTY 7.2.12 duty must still be present");
        assertNotEquals("OVERLAY-SHOULD-NOT-WIN", retained.getCluster().getBlueprintName(),
                "the on-disk duty must win over an overlay materialized under the same key");
    }

    @Test
    void cdpStackRequests() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(supportedRuntimes.isEmpty()).thenReturn(true);
            cdpConfigService.initCdpStackRequests();
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
            assertFalse(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, RUNTIME_7214))
                    .getCluster().getBlueprintName().contains("Profiler"));
            assertFalse(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, RUNTIME_7212))
                    .getCluster().getBlueprintName().contains("Profiler"));
            assertFalse(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214))
                    .getCluster().getBlueprintName().contains("Profiler"));
            lenient().when(entitlementService.isEntitledFor(anyString(), eq(Entitlement.CDP_DATA_LAKE_MEDIUM_DUTY_WITH_PROFILER))).thenReturn(Boolean.FALSE);
            assertFalse(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, RUNTIME_7214))
                    .getCluster().getBlueprintName().contains("Profiler"));
        });
    }

    @Test
    void cdpEnableStackRequests() {
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(true);
        when(supportedRuntimes.contains(RUNTIME_7212)).thenReturn(false);
        cdpConfigService.initCdpStackRequests();
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
        assertNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
    }

    @Test
    void cdpEnableEveryStackRequests() {
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(true);
        cdpConfigService.initCdpStackRequests();
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_7214)));
    }

    @Test
    void testEmptyAdvertisedRuntimes() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_7217);

        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(false);
        when(supportedRuntimes.contains(RUNTIME_7212)).thenReturn(true);
        when(supportedRuntimes.contains(RUNTIME_7217)).thenReturn(true);

        when(advertisedRuntimes.isEmpty()).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7217, true, Architecture.X86_64), rt(RUNTIME_7212, false, Architecture.X86_64));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null, null, false);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testEmptyDefaultRuntimeAndEmptyAdvertisedRuntimes() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", null);

        when(imageCatalogService.getDefaultImageCatalogRuntimeVersions(any())).thenReturn(List.of(RUNTIME_7212, RUNTIME_729));
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(false);
        when(supportedRuntimes.contains(RUNTIME_7212)).thenReturn(true);

        when(advertisedRuntimes.isEmpty()).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7212, true, Architecture.X86_64));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null, null, false);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testFilteredDlVersions() {
        List<String> supportedVersions = List.of(RUNTIME_7212, RUNTIME_7214);
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        assertArrayEquals(supportedVersions.toArray(), cdpConfigService.getDatalakeVersions("", null).stream().sorted().toArray());
    }

    @Test
    void testFilteredDlVersionsEmptyDefault() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", null);

        when(imageCatalogService.getDefaultImageCatalogRuntimeVersions(any())).thenReturn(List.of(RUNTIME_7212, RUNTIME_726));

        List<String> supportedVersions = List.of(RUNTIME_7212, RUNTIME_726, RUNTIME_722);
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        assertArrayEquals(List.of(RUNTIME_7212).toArray(), cdpConfigService.getDatalakeVersions("", null).toArray());
    }

    @Test
    void testFilteredDlVersionsForGcp() {
        List<String> supportedVersions = List.of(RUNTIME_729, RUNTIME_722);
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        List<String> gcpSupportedVersions = List.of(RUNTIME_729);
        assertArrayEquals(gcpSupportedVersions.toArray(), cdpConfigService.getDatalakeVersions("GCP", null).toArray(),
                "Templates are only available from 7.2.9");
    }

    @Test
    void testFilteredDlVersionsForAws() {
        List<String> supportedVersions = List.of(RUNTIME_7214, RUNTIME_7212);
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        List<String> awsSupportedVersions = List.of(RUNTIME_7212, RUNTIME_7214);
        assertArrayEquals(awsSupportedVersions.toArray(), cdpConfigService.getDatalakeVersions("AWS", null).stream().sorted().toArray());
    }

    @Test
    void testAdvertisedRuntimes() {
        List<String> supportedVersions = List.of(RUNTIME_7217, RUNTIME_7212);
        mockSupportedRuntimes(supportedVersions);

        when(advertisedRuntimes.isEmpty()).thenReturn(false);
        when(advertisedRuntimes.contains(RUNTIME_7212)).thenReturn(false);
        when(advertisedRuntimes.contains(RUNTIME_7217)).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7217, false, Architecture.X86_64));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null, null, false);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testAdvertisedRuntimesWithEmptyDefault() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", null);

        when(imageCatalogService.getDefaultImageCatalogRuntimeVersions(any())).thenReturn(List.of(RUNTIME_7212, RUNTIME_729));

        List<String> supportedVersions = List.of(RUNTIME_710, RUNTIME_729, RUNTIME_7212);
        mockSupportedRuntimes(supportedVersions);

        when(advertisedRuntimes.isEmpty()).thenReturn(false);
        when(advertisedRuntimes.contains(RUNTIME_7212)).thenReturn(true);
        when(advertisedRuntimes.contains(RUNTIME_729)).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7212, true, Architecture.X86_64), rt(RUNTIME_729, false, Architecture.X86_64));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null, null, false);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testAdvertisedRuntimesForGcp() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_7214);

        List<String> supportedVersions = List.of(RUNTIME_7214, RUNTIME_722);
        mockSupportedRuntimes(supportedVersions);

        List<String> advertisedVersions = List.of(RUNTIME_7214);
        mockAdvertisedRuntimes(advertisedVersions);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7214, true, Architecture.X86_64));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes("GCP", null, false);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testAdvertisedRuntimesForCentOS7() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_7217);

        List<String> supportedVersions = List.of(RUNTIME_7218, RUNTIME_7217, RUNTIME_7216, RUNTIME_7215, RUNTIME_7214);
        mockSupportedRuntimes(supportedVersions);

        List<String> advertisedVersions = List.of(RUNTIME_7218, RUNTIME_7217, RUNTIME_7216, RUNTIME_7215);
        mockAdvertisedRuntimes(advertisedVersions);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7217, true, Architecture.X86_64), rt(RUNTIME_7216, false, Architecture.X86_64),
                rt(RUNTIME_7215, false, Architecture.X86_64));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null, "centos7", false);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testAdvertisedRuntimesForRHEL8() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_7217);

        List<String> supportedVersions = List.of(RUNTIME_7218, RUNTIME_7217, RUNTIME_7216, RUNTIME_7215, RUNTIME_7214);
        mockSupportedRuntimes(supportedVersions);

        List<String> advertisedVersions = List.of(RUNTIME_7218, RUNTIME_7217, RUNTIME_7216, RUNTIME_7215);
        mockAdvertisedRuntimes(advertisedVersions);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7218, false, Architecture.X86_64), rt(RUNTIME_7217, true, Architecture.X86_64));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null, "redhat8", false);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testAdvertisedRuntimesForInvalidOS() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_7217);

        List<String> supportedVersions = List.of(RUNTIME_7218, RUNTIME_7217, RUNTIME_7216, RUNTIME_7215, RUNTIME_7214);
        mockSupportedRuntimes(supportedVersions);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = new ArrayList<>();
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null, "invalid", false);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    public void testAdvertisedRuntimesForX86AndArm() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_731);

        List<String> supportedVersions = List.of(RUNTIME_732, RUNTIME_731, RUNTIME_7218, RUNTIME_7217, RUNTIME_7216, RUNTIME_7215);
        mockSupportedRuntimes(supportedVersions);

        List<String> advertisedVersions = List.of(RUNTIME_732, RUNTIME_731, RUNTIME_7218, RUNTIME_7217, RUNTIME_7216);
        mockAdvertisedRuntimes(advertisedVersions);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expectedForRedhat8 = List.of(
                rt(RUNTIME_731, true, Architecture.ARM64),
                rt(RUNTIME_731, true, Architecture.X86_64),
                rt(RUNTIME_7218, false, Architecture.X86_64),
                rt(RUNTIME_7217, false, Architecture.X86_64)
        );

        List<AdvertisedRuntime> expectedForRedhat9 = List.of(
                rt(RUNTIME_732, false, Architecture.ARM64),
                rt(RUNTIME_732, false, Architecture.X86_64)
        );

        List<AdvertisedRuntime> actualForRedhat8 = cdpConfigService.getAdvertisedRuntimes(null, "redhat8", true);
        assertArrayEquals(expectedForRedhat8.toArray(), actualForRedhat8.toArray());

        List<AdvertisedRuntime> actualForRedhat9 = cdpConfigService.getAdvertisedRuntimes(null, "redhat9", true);
        assertArrayEquals(expectedForRedhat9.toArray(), actualForRedhat9.toArray());
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

    private AdvertisedRuntime rt(String version, boolean defaultVersion, Architecture architecture) {
        AdvertisedRuntime advertisedRuntime = new AdvertisedRuntime();
        advertisedRuntime.setRuntimeVersion(version);
        advertisedRuntime.setDefaultRuntimeVersion(defaultVersion);
        advertisedRuntime.setArchitecture(architecture);
        return advertisedRuntime;
    }
}
