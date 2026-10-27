package com.sequenceiq.stackpatcher.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StackPatchTest {

    private static final Long RESOURCE_ID = 123L;

    @Test
    void constructorWritesLegacyStackId() {
        StackPatch underTest = new StackPatch(RESOURCE_ID, StackPatchType.MOCK);

        assertThat(ReflectionTestUtils.getField(underTest, "legacyStackId")).isEqualTo(RESOURCE_ID);
    }

    @Test
    void setResourceIdWritesLegacyStackId() {
        StackPatch underTest = new StackPatch();

        underTest.setResourceId(RESOURCE_ID);

        assertThat(ReflectionTestUtils.getField(underTest, "legacyStackId")).isEqualTo(RESOURCE_ID);
    }

    @Test
    void getResourceIdFallsBackToLegacyStackIdForRowWrittenByPreviousRelease() {
        StackPatch underTest = new StackPatch();
        ReflectionTestUtils.setField(underTest, "legacyStackId", RESOURCE_ID);

        assertThat(underTest.getResourceId()).isEqualTo(RESOURCE_ID);
    }

    @Test
    void getResourceIdPrefersResourceId() {
        StackPatch underTest = new StackPatch(RESOURCE_ID, StackPatchType.MOCK);
        ReflectionTestUtils.setField(underTest, "legacyStackId", 456L);

        assertThat(underTest.getResourceId()).isEqualTo(RESOURCE_ID);
    }
}
