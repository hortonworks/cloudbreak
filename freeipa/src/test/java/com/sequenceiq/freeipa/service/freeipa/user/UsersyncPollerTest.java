package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;

@ExtendWith(MockitoExtension.class)
class UsersyncPollerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    @Mock
    ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Mock
    AvailableStackProvider availableStackProvider;

    @Mock
    UserSyncStatusService userSyncStatusService;

    @Mock
    UserService userService;

    @Mock
    UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @InjectMocks
    UsersyncPoller underTest;

    @Test
    void testIsStale() throws Exception {
        UmsEventGenerationIds currentEventGenerationIds = mock(UmsEventGenerationIds.class);
        Stack stack = mock(Stack.class);
        setupUserSyncStatus(stack, mock(UmsEventGenerationIds.class));

        assertTrue(underTest.isStale(stack, currentEventGenerationIds));
    }

    @Test
    void testIsNotStale() throws Exception {
        UmsEventGenerationIds currentEventGenerationIds = mock(UmsEventGenerationIds.class);
        Stack stack = mock(Stack.class);
        setupUserSyncStatus(stack, currentEventGenerationIds);

        assertFalse(underTest.isStale(stack, currentEventGenerationIds));
    }

    @Test
    void testSyncStackWhenStale() throws Exception {
        UmsEventGenerationIds currentEventGenerationIds = mock(UmsEventGenerationIds.class);
        when(umsEventGenerationIdsProvider.getEventGenerationIds(any(), any())).thenReturn(currentEventGenerationIds);
        Stack stack = mock(Stack.class);
        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        setupUserSyncStatus(stack, mock(UmsEventGenerationIds.class));
        when(availableStackProvider.getAvailableStacks()).thenReturn(List.of(stack));

        underTest.syncFreeIpaStacks();

        verify(userService).synchronizeUsers(stack.getAccountId(), UsersyncPoller.INTERNAL_ACTOR_CRN,
                Set.of(stack.getEnvironmentCrn()), Set.of(), Set.of());
    }

    @Test
    void testDontSyncStackWhenNotStale() throws Exception {
        UmsEventGenerationIds currentEventGenerationIds = mock(UmsEventGenerationIds.class);
        when(umsEventGenerationIdsProvider.getEventGenerationIds(any(), any())).thenReturn(currentEventGenerationIds);
        Stack stack = mock(Stack.class);
        when(stack.getAccountId()).thenReturn(ACCOUNT_ID);
        when(stack.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN);
        setupUserSyncStatus(stack, currentEventGenerationIds);
        when(availableStackProvider.getAvailableStacks()).thenReturn(List.of(stack));

        underTest.syncFreeIpaStacks();

        verify(userService, times(0))
                .synchronizeUsers(any(), any(), any(), any(), any());
    }

    private void setupUserSyncStatus(Stack stack, UmsEventGenerationIds umsEventGenerationIds) throws Exception {
        UserSyncStatus userSyncStatus = mock(UserSyncStatus.class, RETURNS_DEEP_STUBS);
        when(userSyncStatus.getUmsEventGenerationIds().get(UmsEventGenerationIds.class)).thenReturn(umsEventGenerationIds);
        when(userSyncStatusService.getOrCreateForStack(stack)).thenReturn(userSyncStatus);
    }
}