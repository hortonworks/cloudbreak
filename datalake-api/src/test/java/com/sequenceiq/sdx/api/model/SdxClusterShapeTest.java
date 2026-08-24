package com.sequenceiq.sdx.api.model;

import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.ENTERPRISE_PRO;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY_PRO;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.values;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class SdxClusterShapeTest {
    private static final Set<String> EXPECTED_SHAPES = Set.of("custom", "light_duty", "medium_duty_ha", "micro_duty", "enterprise", "containerized",
            "enterprise_pro", "light_duty_pro");

    private static final Set<SdxClusterShape> MULTI_AZ_SHAPES = Set.of(MEDIUM_DUTY_HA, ENTERPRISE, ENTERPRISE_PRO);

    private static final Set<SdxClusterShape> HA_SHAPES = Set.of(MEDIUM_DUTY_HA, ENTERPRISE, ENTERPRISE_PRO);

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

    @Test
    void testLegacyShapeNamesDeserializeToProShapes() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        assertEquals(LIGHT_DUTY_PRO, objectMapper.readValue("\"LIGHT_DUTY_WITHOUT_HBASE\"", SdxClusterShape.class));
        assertEquals(ENTERPRISE_PRO, objectMapper.readValue("\"ENTERPRISE_WITHOUT_HBASE\"", SdxClusterShape.class));
    }

    @Test
    void testProShapesSerializeToProNames() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        assertEquals("\"LIGHT_DUTY_PRO\"", objectMapper.writeValueAsString(LIGHT_DUTY_PRO));
        assertEquals("\"ENTERPRISE_PRO\"", objectMapper.writeValueAsString(ENTERPRISE_PRO));
    }
}
