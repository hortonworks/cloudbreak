package com.sequenceiq.authorization.utils;

import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

class EventAuthorizationUtilsTest {

    @Mock
    private CommonPermissionCheckingUtils mockCommonPermissionCheckingUtils;

    private EventAuthorizationUtils underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new EventAuthorizationUtils(mockCommonPermissionCheckingUtils);
    }

    @Test
    void testWhenNullPassedThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(IllegalArgumentException.class,
                () -> underTest.checkPermissionBasedOnResourceTypeAndCrn(null));

        assertNotNull(expectedException);
        assertEquals("The collection of " + EventAuthorizationDto.class.getSimpleName() + "s should not be null!", expectedException.getMessage());
    }

    @Test
    void testWhenEmptySetPassedThenNoPermissionCheckCallShouldHappen() {
        underTest.checkPermissionBasedOnResourceTypeAndCrn(emptySet());

        verify(mockCommonPermissionCheckingUtils, never()).checkPermissionForUserOnResource(any(), any(), any());
    }

    @Test
    void testWhenProperSetOfDtosHasPassedThenProperCallShouldHappen() {
        EventAuthorizationDto testDto = new EventAuthorizationDto("someResourceCrn", "datalake");
        String userCrn = "someUserCrn";
        ThreadBasedUserCrnProvider.setUserCrn(userCrn);

        underTest.checkPermissionBasedOnResourceTypeAndCrn(Set.of(testDto));
        verify(mockCommonPermissionCheckingUtils, times(1)).checkPermissionForUserOnResource(any(), any(), any());
        verify(mockCommonPermissionCheckingUtils, times(1))
                .checkPermissionForUserOnResource(AuthorizationResourceAction.DESCRIBE_DATALAKE, userCrn, testDto.getResourceCrn());
    }

}