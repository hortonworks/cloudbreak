package com.sequenceiq.cloudbreak.common.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.cloud.CloudConstant;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.common.gov.CommonGovService;

@ExtendWith(MockitoExtension.class)
class ProviderPreferencesServiceTest {

    private static final String PLATFORM_AWS = "AWS";

    private static final String PLATFORM_GCP = "GCP";

    private static final String PLATFORM_AZURE = "AZURE";

    @Spy
    private ArrayList<CloudConstant> cloudConstants;

    @Mock
    private CloudConstant cloudConstantAws;

    @Mock
    private CloudConstant cloudConstantGcp;

    @Mock
    private CloudConstant cloudConstantAzure;

    @Spy
    private CommonGovService commonGovService;

    @InjectMocks
    private ProviderPreferencesService underTest;

    @BeforeEach
    void initTest() {
        cloudConstants.add(cloudConstantAzure);
        cloudConstants.add(cloudConstantAws);
        cloudConstants.add(cloudConstantGcp);
    }

    @Test
    void enabledPlatformsShouldComeFromCloudConstants() {
        when(cloudConstantAzure.platform()).thenReturn(Platform.platform(PLATFORM_AZURE));
        when(cloudConstantAws.platform()).thenReturn(Platform.platform(PLATFORM_AWS));
        when(cloudConstantGcp.platform()).thenReturn(Platform.platform(PLATFORM_GCP));

        Set<String> actual = underTest.enabledPlatforms();
        Set<String> expected = Set.of(PLATFORM_AWS, PLATFORM_GCP, PLATFORM_AZURE);

        assertEquals(expected, actual);
    }

    @Test
    void enabledPlatformsShouldBeEmpty() {
        Field enabledGovPlatformsField = ReflectionUtils.findField(ProviderPreferencesService.class, "enabledGovPlatforms");
        ReflectionUtils.makeAccessible(enabledGovPlatformsField);
        ReflectionUtils.setField(enabledGovPlatformsField, underTest, PLATFORM_AWS);

        Set<String> actual = underTest.enabledPlatforms();
        Set<String> expected = Set.of();

        assertEquals(expected, actual);
    }

    @Test
    void enabledPlatformsShouldComeFromProperty() {
        Field enabledPlatforms = ReflectionUtils.findField(ProviderPreferencesService.class, "enabledPlatforms");
        ReflectionUtils.makeAccessible(enabledPlatforms);
        ReflectionUtils.setField(enabledPlatforms, underTest, PLATFORM_AWS + "," + PLATFORM_GCP);

        Set<String> actual = underTest.enabledPlatforms();
        Set<String> expected = Set.of(PLATFORM_AWS, PLATFORM_GCP);

        assertEquals(expected, actual);
    }

    @Test
    void enabledGovPlatforms() {
        Field enabledGovPlatformsField = ReflectionUtils.findField(ProviderPreferencesService.class, "enabledGovPlatforms");
        ReflectionUtils.makeAccessible(enabledGovPlatformsField);
        ReflectionUtils.setField(enabledGovPlatformsField, underTest, PLATFORM_AWS + "," + PLATFORM_AZURE);

        Set<String> actual = underTest.enabledGovPlatforms();
        assertEquals(Set.of(PLATFORM_AWS, PLATFORM_AZURE), actual);

        ReflectionUtils.setField(enabledGovPlatformsField, underTest, PLATFORM_AWS);
        actual = underTest.enabledGovPlatforms();
        assertEquals(Set.of(PLATFORM_AWS), actual);
    }

    @Test
    void isGovCloudDeployment() {
        Field enabledGovPlatformsField = ReflectionUtils.findField(ProviderPreferencesService.class, "enabledGovPlatforms");
        ReflectionUtils.makeAccessible(enabledGovPlatformsField);
        ReflectionUtils.setField(enabledGovPlatformsField, underTest, PLATFORM_AWS);

        boolean actual = underTest.isGovCloudDeployment();
        assertTrue(actual, "This is true, if only gov cloud is enabled, but other providers are not");
    }

    @Test
    void testWhenMultiplePlatformsAreEnabled() {
        Field enabledGovPlatformsField = ReflectionUtils.findField(ProviderPreferencesService.class, "enabledGovPlatforms");
        ReflectionUtils.makeAccessible(enabledGovPlatformsField);
        ReflectionUtils.setField(enabledGovPlatformsField, underTest, PLATFORM_AWS);

        Field enabledPlatforms = ReflectionUtils.findField(ProviderPreferencesService.class, "enabledPlatforms");
        ReflectionUtils.makeAccessible(enabledPlatforms);
        ReflectionUtils.setField(enabledPlatforms, underTest, PLATFORM_AWS + "," + PLATFORM_GCP);

        boolean actual = underTest.isGovCloudDeployment();

        assertFalse(actual, "This is false, when multiple providers are enabled including gov cloud, e.g in local env");
    }
}