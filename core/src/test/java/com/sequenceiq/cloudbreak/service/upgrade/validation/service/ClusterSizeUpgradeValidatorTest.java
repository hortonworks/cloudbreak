package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.conf.LimitConfiguration;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class ClusterSizeUpgradeValidatorTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    private static final int NODE_COUNT_LIMIT = 20;

    @InjectMocks
    private ClusterSizeUpgradeValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private LimitConfiguration limitConfiguration;

    @Mock
    private StackDto stack;

    @BeforeEach
    void before() {
        ReflectionTestUtils.setField(underTest, "maxNumberOfInstancesForRollingUpgrade", NODE_COUNT_LIMIT);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsNotEnabledAndVmReplacementIsNotEnabled() {
        underTest.validate(createRequest(false, false));
        verifyNoInteractions(entitlementService);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsEnabledAndSkipRollingUpgradeValidationIsEnabled() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(true);

        doAs(ACTOR, () -> underTest.validate(createRequest(true, false)));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(any());
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenRollingUpgradeIsEnabledAndTheActualNodeCountIsAcceptable() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        mockAvailableInstances(NODE_COUNT_LIMIT);
        doAs(ACTOR, () -> underTest.validate(createRequest(true, false)));
    }

    @Test
    void testValidateShouldThrowExceptionWhenRollingUpgradeIsEnabledAndTheActualNodeCountIsTooHigh() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        mockAvailableInstances(21);
        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(true, false))));

        assertEquals("Rolling upgrade is not permitted because this cluster has 21 nodes but the maximum number of allowed nodes is 20.",
                exception.getMessage());
    }

    @Test
    void testValidateShouldThrowExceptionWhenVmReplacementIsEnabledAndTheActualNodeCountIsTooHigh() {
        when(limitConfiguration.getUpgradeNodeCountLimit(any())).thenReturn(NODE_COUNT_LIMIT);
        mockAvailableInstances(21);
        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(false, true))));

        assertEquals("There are 21 nodes in the cluster. Upgrade is supported up to 20 nodes. "
                        + "Please downscale the cluster below the limit and retry the upgrade.", exception.getMessage());
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenVmReplacementIsEnabledAndSkipValidationIsEnabled() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(true);
        mockAvailableInstances(5000);
        assertDoesNotThrow(() -> doAs(ACTOR, () -> underTest.validate(createRequest(false, true))));
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenVmReplacementIsEnabledAndTheActualNodeCountIsAcceptable() {
        when(limitConfiguration.getUpgradeNodeCountLimit(any())).thenReturn(NODE_COUNT_LIMIT);
        mockAvailableInstances(NODE_COUNT_LIMIT);
        doAs(ACTOR, () -> underTest.validate(createRequest(false, true)));
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenVmReplacementIsNotEnabledAndRollingUpgradeIsNotEnabled() {
        doAs(ACTOR, () -> underTest.validate(createRequest(false, false)));
    }

    private void mockAvailableInstances(long numberOfInstances) {
        when(stack.getFullNodeCount()).thenReturn(numberOfInstances);
    }

    private ServiceUpgradeValidationRequest createRequest(boolean rollingUpgradeEnabled, boolean replaceVms) {
        return new ServiceUpgradeValidationRequest(stack, false, rollingUpgradeEnabled, null, replaceVms);
    }

}