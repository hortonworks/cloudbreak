package com.sequenceiq.cloudbreak.util;

import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_TAG;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_V1;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.HYPER_V_GENERATION_V2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AzureHyperVGenerationUtilTest {

    @Test
    void testToGenerationWhenGen1Tag() {
        assertEquals(HYPER_V_GENERATION_V1, AzureHyperVGenerationUtil.toGeneration(Map.of(HYPER_V_GENERATION_TAG, "1")));
    }

    @Test
    void testToGenerationWhenGen2Tag() {
        assertEquals(HYPER_V_GENERATION_V2, AzureHyperVGenerationUtil.toGeneration(Map.of(HYPER_V_GENERATION_TAG, "2")));
    }

    @Test
    void testToGenerationWhenTagAbsent() {
        assertNull(AzureHyperVGenerationUtil.toGeneration(Map.of("release-version", "7.3.1")));
    }

    @Test
    void testToGenerationWhenNoTagsAtAll() {
        assertNull(AzureHyperVGenerationUtil.toGeneration(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "3", "V2", "gen2", "two"})
    void testToGenerationWhenTagValueUnrecognised(String tagValue) {
        assertNull(AzureHyperVGenerationUtil.toGeneration(Map.of(HYPER_V_GENERATION_TAG, tagValue)));
    }
}
