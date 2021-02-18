package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

public class DefaultRootVolumeSizeProviderTest {

    private final CloudPlatformConnectors mockConnectors = Mockito.mock(CloudPlatformConnectors.class);

    private final Environment environment = new StandardEnvironment();

    private DefaultRootVolumeSizeProvider underTest;

    @Before
    public void initConnectors() {
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
    public void testNoPropertySetForKnownPlatform() {
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getForPlatform("AWS");
        assertEquals(100L, rootVolumeSize);
    }

    @Test
    public void testPropertySetForKnownPlatform() {
        System.setProperty("cb.platform.default.rootVolumeSize.GCP", "100");
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getForPlatform("gcp");
        assertEquals(100L, rootVolumeSize);
    }

    @Test
    public void testWithUnknownPlatform() {
        underTest = new DefaultRootVolumeSizeProvider(mockConnectors, environment);
        int rootVolumeSize = underTest.getForPlatform("UNKNOWN_PLATFORM");
        assertEquals(100L, rootVolumeSize);
    }
}