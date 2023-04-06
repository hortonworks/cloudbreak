package com.sequenceiq.sdx.api.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

public class SdxClusterShapeTest {
    private static final Set<String> EXPECTED_SHAPES = Set.of("custom", "light_duty", "medium_duty_ha", "micro_duty", "scalable");

    private static final Set<SdxClusterShape> MULTI_AZ_SHAPES = Set.of(SdxClusterShape.MEDIUM_DUTY_HA, SdxClusterShape.SCALABLE);

    private static final Set<SdxClusterShape> HA_SHAPES = Set.of(SdxClusterShape.MEDIUM_DUTY_HA, SdxClusterShape.SCALABLE);

    @Test
    void testVerifyShapeNames() {
        for (SdxClusterShape shape : SdxClusterShape.values()) {
            assertTrue(EXPECTED_SHAPES.contains(shape.name().toLowerCase()));
        }
    }

    @Test
    void testVerifyMultiAZShapes() {
        for (SdxClusterShape shape : SdxClusterShape.values()) {
            if (MULTI_AZ_SHAPES.contains(shape)) {
                assertTrue(shape.isMultiAzEnabledByDefault());
            } else {
                assertFalse(shape.isMultiAzEnabledByDefault());
            }
        }
    }

    @Test
    void testVerifyHAShapes() {
        for (SdxClusterShape shape : SdxClusterShape.values()) {
            if (HA_SHAPES.contains(shape)) {
                assertTrue(shape.isHA());
            } else {
                assertFalse(shape.isHA());
            }
        }
    }
}
