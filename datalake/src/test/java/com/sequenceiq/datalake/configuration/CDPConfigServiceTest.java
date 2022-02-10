package com.sequenceiq.datalake.configuration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
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

    @BeforeEach
    public void setupAll() {
        lenient().when(entitlementService.isEntitledFor(anyString(), eq(Entitlement.CDP_DATA_LAKE_MEDIUM_DUTY_WITH_PROFILER))).thenReturn(Boolean.TRUE);
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_710);
    }

    private static Object[][] testResourceTemplateDataProvider() {
        return new Object[][]{
                //testCaseName     templatePath     expectedVersion     expectedProvider    expectedEntitlement     expectedSdxClusterShape
                {"testResourceTemplate - with Entitlement", "resources/duties/7.2.14/aws/cdp_data_lake_medium_duty_with_profiler/medium_duty_ha.json",
                        RUNTIME_7214, "aws", "/cdp_data_lake_medium_duty_with_profiler", "medium_duty_ha"},
                {"testResourceTemplate - without Entitlement", "resources/duties/7.2.14/aws/medium_duty_ha.json",
                        RUNTIME_7214, "aws", null, "medium_duty_ha"}
        };
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testResourceTemplateDataProvider")
    public void testResourceTemplate(String testName, String templatePath,
            String expectedVersion, String expectedProvider, String expectedEntitlement, String expectedSdxClusterShape) {
        Matcher matcher = cdpConfigService.RESOURCE_TEMPLATE_PATTERN.matcher(templatePath);
        assertTrue(matcher.matches());
        assertEquals(matcher.group(1), expectedVersion);
        assertEquals(matcher.group(2), expectedProvider);
        assertEquals(matcher.group(3), expectedEntitlement);
        assertEquals(matcher.group(4), expectedSdxClusterShape);
    }

    @Test
    void cdpStackRequests() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(supportedRuntimes.isEmpty()).thenReturn(true);
            cdpConfigService.initCdpStackRequests();
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_7212)));
            assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
            assertTrue(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.MEDIUM_DUTY_HA, RUNTIME_7214))
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
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
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
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AWS, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.AZURE, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.YARN, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
        assertNotNull(cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.MOCK, SdxClusterShape.LIGHT_DUTY, RUNTIME_710)));
    }

    @Test
    void testEmptyAdvertisedRuntimes() {
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(false);
        when(supportedRuntimes.contains(RUNTIME_7212)).thenReturn(true);
        when(supportedRuntimes.contains(RUNTIME_710)).thenReturn(true);

        when(advertisedRuntimes.isEmpty()).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7212, false), rt(RUNTIME_710, true));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testEmptyDefaultRuntimeAndEmptyAdvertisedRuntimes() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", null);

        when(imageCatalogService.getDefaultImageCatalogRuntimeVersions(any())).thenReturn(List.of(RUNTIME_7212, RUNTIME_729));
        when(supportedRuntimes.isEmpty()).thenReturn(false);
        when(supportedRuntimes.contains(Mockito.anyString())).thenReturn(false);
        when(supportedRuntimes.contains(RUNTIME_7212)).thenReturn(true);
        when(supportedRuntimes.contains(RUNTIME_710)).thenReturn(true);

        when(advertisedRuntimes.isEmpty()).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7212, true));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testFilteredDlVersions() {
        List<String> supportedVersions = List.of(RUNTIME_7212, RUNTIME_726);
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        assertArrayEquals(supportedVersions.toArray(), cdpConfigService.getDatalakeVersions("").toArray());
    }

    @Test
    void testFilteredDlVersionsEmptyDefault() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", null);

        when(imageCatalogService.getDefaultImageCatalogRuntimeVersions(any())).thenReturn(List.of(RUNTIME_7212, RUNTIME_726));

        List<String> supportedVersions = List.of(RUNTIME_7212, RUNTIME_726, RUNTIME_722);
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        assertArrayEquals(List.of(RUNTIME_7212, RUNTIME_726).toArray(), cdpConfigService.getDatalakeVersions("").toArray());
    }

    @Test
    void testFilteredDlVersionsForGcp() {
        List<String> supportedVersions = List.of(RUNTIME_726, RUNTIME_722);
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        List<String> gcpSupportedVersions = List.of(RUNTIME_726);
        assertArrayEquals("Templates are only available from 7.2.6",
                gcpSupportedVersions.toArray(), cdpConfigService.getDatalakeVersions("GCP").toArray());
    }

    @Test
    void testFilteredDlVersionsForAws() {
        List<String> supportedVersions = List.of(RUNTIME_726, RUNTIME_7212);
        mockSupportedRuntimes(supportedVersions);
        cdpConfigService.initCdpStackRequests();
        List<String> gcpSupportedVersions = List.of(RUNTIME_7212, RUNTIME_726);
        assertArrayEquals(gcpSupportedVersions.toArray(), cdpConfigService.getDatalakeVersions("AWS").toArray());
    }

    @Test
    void testAdvertisedRuntimes() {
        List<String> supportedVersions = List.of(RUNTIME_710, RUNTIME_7212);
        mockSupportedRuntimes(supportedVersions);

        when(advertisedRuntimes.isEmpty()).thenReturn(false);
        when(advertisedRuntimes.contains(RUNTIME_7212)).thenReturn(false);
        when(advertisedRuntimes.contains(RUNTIME_710)).thenReturn(true);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_710, true));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null);

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

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_7212, true), rt(RUNTIME_729, false));
        List<AdvertisedRuntime> actual = cdpConfigService.getAdvertisedRuntimes(null);

        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testAdvertisedRuntimesForGcp() {
        ReflectionTestUtils.setField(cdpConfigService, "defaultRuntime", RUNTIME_726);

        List<String> supportedVersions = List.of(RUNTIME_726, RUNTIME_722);
        mockSupportedRuntimes(supportedVersions);

        List<String> advertisedVersions = List.of(RUNTIME_726);
        mockAdvertisedRuntimes(advertisedVersions);

        cdpConfigService.initCdpStackRequests();

        List<AdvertisedRuntime> expected = List.of(rt(RUNTIME_726, true));
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