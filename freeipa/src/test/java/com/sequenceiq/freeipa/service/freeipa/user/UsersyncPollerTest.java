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

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsEventGenerationIds;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UsersyncPollerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    @Mock
    StackService stackService;

    @Mock
    UserSyncStatusService userSyncStatusService;

    @Mock
    UserService userService;

    @Mock
    UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Mock
    EntitlementService entitlementService;

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
        Stack stack = setupStack();
        setupEntitlement(true);
        setupEventGenerationIds(stack, true);

        underTest.syncFreeIpaStacks();

        verify(userService).synchronizeUsers(ACCOUNT_ID, UsersyncPoller.INTERNAL_ACTOR_CRN,
                Set.of(ENVIRONMENT_CRN), Set.of(), Set.of());
    }

    @Test
    void testDontSyncStackWhenNotStale() throws Exception {
        Stack stack = setupStack();
        setupEntitlement(true);
        setupEventGenerationIds(stack, false);

        underTest.syncFreeIpaStacks();

        verify(userService, times(0))
                .synchronizeUsers(any(), any(), any(), any(), any());
    }

    @Test
    void testDontSyncStackWhenNotEntitled() throws Exception {
        setupStack();
        setupEntitlement(false);

        underTest.syncFreeIpaStacks();

        verify(userService, times(0))
                .synchronizeUsers(any(), any(), any(), any(), any());
    }

    private Stack setupStack() {
        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        when(stackService.findAllRunning()).thenReturn(List.of(stack));
        return stack;
    }

    private void setupEntitlement(boolean entitled) {
        when(entitlementService.automaticUsersyncPollerEnabled(any(), any())).thenReturn(entitled);
    }

    private void setupEventGenerationIds(Stack stack, boolean stale) throws Exception {
        UmsEventGenerationIds currentEventGenerationIds = mock(UmsEventGenerationIds.class);
        when(umsEventGenerationIdsProvider.getEventGenerationIds(any(), any())).thenReturn(currentEventGenerationIds);
        if (stale) {
            setupUserSyncStatus(stack, mock(UmsEventGenerationIds.class));
        } else {
            setupUserSyncStatus(stack, currentEventGenerationIds);
        }
    }

    private void setupUserSyncStatus(Stack stack, UmsEventGenerationIds umsEventGenerationIds) throws Exception {
        UserSyncStatus userSyncStatus = mock(UserSyncStatus.class, RETURNS_DEEP_STUBS);
        when(userSyncStatus.getUmsEventGenerationIds().get(UmsEventGenerationIds.class)).thenReturn(umsEventGenerationIds);
        when(userSyncStatusService.getOrCreateForStack(stack)).thenReturn(userSyncStatus);
    }
}