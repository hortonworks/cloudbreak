package com.sequenceiq.environment.environment.service.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;

class FreeIpaInstanceCountByGroupProviderTest {

    private static final int DEFAULT_INSTANCE_COUNT_BY_GROUP = 2;

    private FreeIpaInstanceCountByGroupProvider underTest;

    @BeforeEach
    void setUp() {
        underTest = new FreeIpaInstanceCountByGroupProvider();
        ReflectionTestUtils.setField(underTest, "defaultInstanceCountByGroup", DEFAULT_INSTANCE_COUNT_BY_GROUP);
    }

    @Test
    void getInstanceCountShouldReturnWithTheDefaultWhenRequestDoesNotContainRequiredInstanceCount() {
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();

        int actual = underTest.getInstanceCount(attachedFreeIpaRequest);

        assertEquals(DEFAULT_INSTANCE_COUNT_BY_GROUP, actual);
    }

    @Test
    void getInstanceCountShouldReturnWithTheUserRequestedWhenRequestContainsRequiredInstanceCount() {
        int expectedInstanceCountByGroup = 3;
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        attachedFreeIpaRequest.setInstanceCountByGroup(expectedInstanceCountByGroup);

        int actual = underTest.getInstanceCount(attachedFreeIpaRequest);

        assertEquals(expectedInstanceCountByGroup, actual);
    }

    @Test
    void getDefaultInstanceCountShouldReturn() {
        int actual = underTest.getDefaultInstanceCount();

        assertEquals(DEFAULT_INSTANCE_COUNT_BY_GROUP, actual);
    }
}