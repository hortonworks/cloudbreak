package com.sequenceiq.freeipa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@ExtendWith(MockitoExtension.class)
class DefaultRootVolumeTypeProviderTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private Environment environment;

    @InjectMocks
    private DefaultRootVolumeTypeProvider underTest;

    @BeforeEach
    void setUp() {
        PlatformVariants platformVariants = mock(PlatformVariants.class);
        Map<Platform, Variant> variantMap = new HashMap<>();
        variantMap.put(Platform.platform("GCP"), Variant.variant("GCP"));
        when(platformVariants.getDefaultVariants()).thenReturn(variantMap);
        when(cloudPlatformConnectors.getPlatformVariants()).thenReturn(platformVariants);
        when(environment.containsProperty(anyString())).thenReturn(true);
        when(environment.getProperty(anyString(), anyString())).thenReturn("SSD");
        underTest.init();
    }

    @Test
    void getForPlatform() {
        assertThat(underTest.getForPlatform("GCP")).isEqualTo("SSD");
        assertThat(underTest.getForPlatform("YARN")).isEqualTo("HDD");
    }
}