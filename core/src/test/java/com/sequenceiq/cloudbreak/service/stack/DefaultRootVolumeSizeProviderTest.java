package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

class DefaultRootVolumeSizeProviderTest {

    private final CloudPlatformConnectors mockConnectors = mock(CloudPlatformConnectors.class);

    private final Environment environment = new StandardEnvironment();

    private DefaultRootVolumeSizeProvider underTest;

    @BeforeEach
    void initConnectors() {
        Map<Platform, Collection<Variant>> platformToVariants = new HashMap<>();
        Platform gcp = Platform.platform("GCP");
        Platform aws = Platform.platform("AWS");
        platformToVariants.put(gcp, Collections.emptyList());
        platformToVariants.put(aws, Collections.emptyList());
        Map<Platform, Variant> defaultVariants = new HashMap<>();
        defaultVariants.put(gcp, Variant.EMPTY);
        defaultVariants.put(aws, Variant.EMPTY);
        when(mockConnectors.getPlatformVariants()).thenReturn(new PlatformVariants(platformToVariants, defaultVariants));
    }

    @Test
    void testNoPropertySetForKnownPlatformGW() {
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getDefaultRootVolumeForPlatform("AWS", true);
        assertEquals(300L, rootVolumeSize);
    }

    @Test
    void testNoPropertySetForKnownPlatformNotGW() {
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getDefaultRootVolumeForPlatform("AWS", false);
        assertEquals(200L, rootVolumeSize);
    }

    @Test
    void testPropertySetForKnownPlatformGW() {
        System.setProperty("cb.platform.default.rootVolumeSize.GCP", "100");
        System.setProperty("cb.platform.default.gatewayRootVolumeSize.GCP", "150");
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getDefaultRootVolumeForPlatform("gcp", true);
        assertEquals(150L, rootVolumeSize);
    }

    @Test
    void testPropertySetForKnownPlatformNotGW() {
        System.setProperty("cb.platform.default.rootVolumeSize.GCP", "100");
        System.setProperty("cb.platform.default.gatewayRootVolumeSize.GCP", "150");
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getDefaultRootVolumeForPlatform("gcp", false);
        assertEquals(100L, rootVolumeSize);
    }

    @Test
    void testWithUnknownPlatformGW() {
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getDefaultRootVolumeForPlatform("UNKNOWN_PLATFORM", true);
        assertEquals(300L, rootVolumeSize);
    }

    @Test
    void testWithUnknownPlatformNotGW() {
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getDefaultRootVolumeForPlatform("UNKNOWN_PLATFORM", false);
        assertEquals(200L, rootVolumeSize);
    }
}