package com.sequenceiq.freeipa.service.freeipa.user.poller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.WorkloadCredentialsUpdateType;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.EventGenerationIdsChecker;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncStatusService;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncTestUtils;
import com.sequenceiq.freeipa.service.freeipa.user.ums.UmsEventGenerationIdsProvider;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UserSyncPollerTest {
    private static final Duration COOLDOWN = Duration.ofMinutes(10);

    @Mock
    StackService stackService;

    @Mock
    UserSyncService userSyncService;

    @Mock
    UserSyncStatusService userSyncStatusService;

    @Mock
    UmsEventGenerationIdsProvider umsEventGenerationIdsProvider;

    @Mock
    UserSyncPollerEntitlementChecker userSyncPollerEntitlementChecker;

    @Mock
    EventGenerationIdsChecker eventGenerationIdsChecker;

    @Mock
    CooldownChecker cooldownChecker;

    @Mock
    RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    UserSyncPoller underTest;

    @BeforeEach
    void setUp() {
        underTest.cooldown = COOLDOWN;
    }

    @Test
    void testSyncStackWhenNotInSync() {
        UserSyncStatus userSyncStatus = setupMocks();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(eventGenerationIdsChecker.isInSync(eq(userSyncStatus), any())).thenReturn(false);
        when(cooldownChecker.isCooldownExpired(eq(userSyncStatus), any())).thenReturn(true);

        underTest.syncAllFreeIpaStacks();

        verify(userSyncService).synchronizeUsers(UserSyncTestUtils.ACCOUNT_ID, "crn:altus:iam:us-west-1:altus:user:__internal__actor__",
                Set.of(UserSyncTestUtils.ENVIRONMENT_CRN), Set.of(), Set.of(), WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED);
    }

    @Test
    void testDontSyncStackWhenInSync() {
        UserSyncStatus userSyncStatus = setupMocks();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(eventGenerationIdsChecker.isInSync(eq(userSyncStatus), any())).thenReturn(true);

        underTest.syncAllFreeIpaStacks();

        verify(userSyncService, times(0))
                .synchronizeUsers(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testSyncStackWhenCool() {
        UserSyncStatus userSyncStatus = setupMocks();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(eventGenerationIdsChecker.isInSync(eq(userSyncStatus), any())).thenReturn(false);
        when(cooldownChecker.isCooldownExpired(eq(userSyncStatus), any())).thenReturn(true);

        underTest.syncAllFreeIpaStacks();

        verify(userSyncService).synchronizeUsers(UserSyncTestUtils.ACCOUNT_ID, "crn:altus:iam:us-west-1:altus:user:__internal__actor__",
                Set.of(UserSyncTestUtils.ENVIRONMENT_CRN), Set.of(), Set.of(), WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED);
    }

    @Test
    void testDontSyncStackWhenNotCool() {
        UserSyncStatus userSyncStatus = setupMocks();
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(eventGenerationIdsChecker.isInSync(eq(userSyncStatus), any())).thenReturn(false);
        when(cooldownChecker.isCooldownExpired(eq(userSyncStatus), any())).thenReturn(false);

        underTest.syncAllFreeIpaStacks();

        verify(userSyncService, times(0))
                .synchronizeUsers(any(), any(), any(), any(), any(), any());
    }

    @Test
    void testDontSyncStackWhenNotEntitled() {
        setupMockStackService(UserSyncTestUtils.createStack());
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        when(userSyncPollerEntitlementChecker.isAccountEntitled(anyString())).thenReturn(false);

        underTest.syncAllFreeIpaStacks();

        verify(userSyncService, times(0))
                .synchronizeUsers(any(), any(), any(), any(), any(), any());
    }

    private Stack setupMockStackService(Stack stack) {
        when(stackService.findAllWithDetailedStackStatuses(DetailedStackStatus.AVAILABLE_STATUSES)).thenReturn(List.of(stack));
        return stack;
    }

    private UserSyncStatus setupMocks() {
        Stack stack = UserSyncTestUtils.createStack();
        setupMockStackService(stack);
        when(userSyncPollerEntitlementChecker.isAccountEntitled(anyString())).thenReturn(true);
        UserSyncStatus userSyncStatus = UserSyncTestUtils.createUserSyncStatus(stack);
        when(userSyncStatusService.getOrCreateForStack(userSyncStatus.getStack())).thenReturn(userSyncStatus);
        when(umsEventGenerationIdsProvider.getEventGenerationIds(any(), any())).thenReturn(UserSyncTestUtils.createUniqueUmsEventGenerationIds());
        return userSyncStatus;
    }
}