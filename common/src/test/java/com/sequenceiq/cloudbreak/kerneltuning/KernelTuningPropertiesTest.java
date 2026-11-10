package com.sequenceiq.cloudbreak.kerneltuning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.config.CommonConfig;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { CommonConfig.class, KernelTuningProperties.class })
@EnableConfigurationProperties
class KernelTuningPropertiesTest {

    @Inject
    private KernelTuningProperties underTest;

    @Test
    void testDefaultsAreBoundFromCommonConfig() {
        assertTrue(underTest.isEnabled());
        assertEquals(1.0, underTest.getRatioPercent());
        assertEquals(67584L, underTest.getFloorKb());
        assertEquals(67584L, underTest.getRestoreKb());
    }

    @Test
    void testToPillarMap() {
        KernelTuningProperties properties = new KernelTuningProperties();
        properties.setEnabled(true);
        properties.setRatioPercent(1.5);
        properties.setFloorKb(65536L);
        properties.setRestoreKb(67584L);

        Map<String, Object> pillar = properties.toPillarMap();

        assertEquals(Map.of("kernel-tuning", Map.of("minFreeKbytes", Map.of(
                "enabled", true,
                "ratioPercent", 1.5,
                "floorKb", 65536L,
                "restoreKb", 67584L))), pillar);
    }
}
