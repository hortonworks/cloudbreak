package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;

@ExtendWith(MockitoExtension.class)
class RepairBatchSizeServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ScalingHardLimitsService scalingHardLimitsService;

    @InjectMocks
    private RepairBatchSizeService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "extendedBatchNodeCount", 1000);
    }

    @Test
    void testGetBatchNodeCountWhenEntitlementIsNotEnabledThenMaxUpscaleStepIsReturned() {
        when(entitlementService.isExtendedRepairBatchSizeEnabled(ACCOUNT_ID)).thenReturn(false);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);

        assertEquals(100, underTest.getBatchNodeCount(ACCOUNT_ID));
    }

    @Test
    void testGetBatchNodeCountWhenEntitlementIsEnabledThenExtendedNodeCountIsReturned() {
        when(entitlementService.isExtendedRepairBatchSizeEnabled(ACCOUNT_ID)).thenReturn(true);

        assertEquals(1000, underTest.getBatchNodeCount(ACCOUNT_ID));

        verifyNoInteractions(scalingHardLimitsService);
    }
}
