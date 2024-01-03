package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@ExtendWith(MockitoExtension.class)
class ClusterSizeUpgradeValidatorTest {

    private static final String ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

    @InjectMocks
    private ClusterSizeUpgradeValidator underTest;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackDto stack;

    @BeforeEach
    void before() {
        ReflectionTestUtils.setField(underTest, "maxNumberOfInstancesForRollingUpgrade", 20);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsNotEnabled() {
        underTest.validate(createRequest(false));
        verifyNoInteractions(entitlementService);
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheRollingUpgradeIsEnabledAndSkipRollingUpgradeValidationIsEnabled() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(true);

        doAs(ACTOR, () -> underTest.validate(createRequest(true)));

        verify(entitlementService).isSkipRollingUpgradeValidationEnabled(any());
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheActualNodeCountIsAcceptable() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        mockAvailableInstances(20);
        doAs(ACTOR, () -> underTest.validate(createRequest(true)));
    }

    @Test
    void testValidateShouldNotThrowExceptionWhenTheActualNodeCountIsTooHigh() {
        when(entitlementService.isSkipRollingUpgradeValidationEnabled(any())).thenReturn(false);
        mockAvailableInstances(21);
        Exception exception = assertThrows(UpgradeValidationFailedException.class, () -> doAs(ACTOR, () -> underTest.validate(createRequest(true))));

        assertEquals("Rolling upgrade is not permitted because this cluster has 21 nodes but the maximum number of allowed nodes is 20.",
                exception.getMessage());
    }

    private void mockAvailableInstances(int numberOfInstances) {
        List<InstanceMetadataView> instanceMetadataList = Mockito.mock(ArrayList.class);
        when(instanceMetadataList.size()).thenReturn(numberOfInstances);
        when(stack.getAllAvailableInstances()).thenReturn(instanceMetadataList);
    }

    private ServiceUpgradeValidationRequest createRequest(boolean rollingUpgradeEnabled) {
        return new ServiceUpgradeValidationRequest(stack, false, rollingUpgradeEnabled, null, null);
    }

}