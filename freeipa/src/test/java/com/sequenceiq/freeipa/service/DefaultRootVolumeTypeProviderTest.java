package com.sequenceiq.freeipa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

class DefaultRootVolumeTypeProviderTest {

    private DefaultRootVolumeTypeProvider underTest;

    @BeforeEach
    void setUp() {
        CloudPlatformConnectors cloudPlatformConnectors = mock(CloudPlatformConnectors.class);
        PlatformVariants platformVariants = mock(PlatformVariants.class);
        Map<Platform, Variant> variantMap = new HashMap<>();
        variantMap.put(Platform.platform("GCP"), Variant.variant("GCP"));
        when(platformVariants.getDefaultVariants()).thenReturn(variantMap);
        when(cloudPlatformConnectors.getPlatformVariants()).thenReturn(platformVariants);
        Environment environment = mock(Environment.class);
        when(environment.containsProperty(anyString())).thenReturn(true);
        when(environment.getProperty(anyString(), anyString())).thenReturn("SSD");
        underTest = new DefaultRootVolumeTypeProvider(cloudPlatformConnectors, environment);
    }

    @Test
    void getForPlatform() {
        assertThat(underTest.getForPlatform("GCP")).isEqualTo("SSD");
        assertThat(underTest.getForPlatform("YARN")).isEqualTo("HDD");
    }
}