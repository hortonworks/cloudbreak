package com.sequenceiq.sdx.api.model;

import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE_WITHOUT_HBASE;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.values;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class SdxClusterShapeTest {
    private static final Set<String> EXPECTED_SHAPES = Set.of("custom", "light_duty", "medium_duty_ha", "micro_duty", "enterprise", "containerized",
            "enterprise_without_hbase", "light_duty_without_hbase");

    private static final Set<SdxClusterShape> MULTI_AZ_SHAPES = Set.of(MEDIUM_DUTY_HA, ENTERPRISE, ENTERPRISE_WITHOUT_HBASE);

    private static final Set<SdxClusterShape> HA_SHAPES = Set.of(MEDIUM_DUTY_HA, ENTERPRISE, ENTERPRISE_WITHOUT_HBASE);

    @Test
    void testVerifyShapeNames() {
        for (SdxClusterShape shape : values()) {
            assertTrue(EXPECTED_SHAPES.contains(shape.name().toLowerCase(Locale.ROOT)));
        }
    }

    @Test
    void testVerifyMultiAZShapes() {
        for (SdxClusterShape shape : values()) {
            if (MULTI_AZ_SHAPES.contains(shape)) {
                assertTrue(shape.isMultiAzEnabledByDefault());
            } else {
                assertFalse(shape.isMultiAzEnabledByDefault());
            }
        }
    }

    @Test
    void testVerifyHAShapes() {
        for (SdxClusterShape shape : values()) {
            if (HA_SHAPES.contains(shape)) {
                assertTrue(shape.isHA());
            } else {
                assertFalse(shape.isHA());
            }
        }
    }
}
