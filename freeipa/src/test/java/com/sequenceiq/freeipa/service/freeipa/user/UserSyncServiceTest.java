package com.sequenceiq.freeipa.service.freeipa.user;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.configuration.BatchPartitionSizeProperties;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsGroup;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.freeipa.user.model.UsersStateDifference;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    private static final int MAX_SUBJECTS_PER_REQUEST = 10;

    @Mock
    StackService stackService;

    @Mock
    OperationService operationService;

    @Mock
    FreeIpaUsersStateProvider freeIpaUsersStateProvider;

    @Mock
    UserSyncStatusService userSyncStatusService;

    @Mock
    UserSyncRequestValidator userSyncRequestValidator;

    @Mock
    EntitlementService entitlementService;

    @Mock
    FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    FreeIpaClient freeIpaClient;

    @Mock
    BatchPartitionSizeProperties batchPartitionSizeProperties;

    @InjectMocks
    UserSyncService underTest;

    @BeforeEach
    void setUp() {
        underTest.maxSubjectsPerRequest = MAX_SUBJECTS_PER_REQUEST;
    }

    @Before
    public void setup() throws FreeIpaClientException {
        when(batchPartitionSizeProperties.getByOperation(anyString())).thenReturn(100);
        when(freeIpaClientFactory.getFreeIpaClientForStack(any())).thenReturn(freeIpaClient);
    }

    @Test
    void testFullSyncRetrievesFullIpaState() throws Exception {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        underTest.getIpaUserState(freeIpaClient, umsUsersState, true);
        verify(freeIpaUsersStateProvider).getUsersState(any());
    }

    @Test
    void testFilteredSyncRetrievesFilteredIpaState() throws Exception {
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        ImmutableSet<String> workloadUsers = mock(ImmutableSet.class);
        when(umsUsersState.getRequestedWorkloadUsernames()).thenReturn(workloadUsers);
        underTest.getIpaUserState(freeIpaClient, umsUsersState, false);
        verify(freeIpaUsersStateProvider).getFilteredFreeIpaState(any(), eq(workloadUsers));
    }

    @Test
    void testAddUsersToGroupsPartitionsRequests() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(5, underTest.maxSubjectsPerRequest * 2);

        Multimap<String, String> warnings = ArrayListMultimap.create();
        doNothing().when(freeIpaClient).callBatch(any(), any(), any(), any());

        underTest.addUsersToGroups(true, freeIpaClient, groupMapping, warnings::put);

        assertTrue(warnings.isEmpty());
    }

    @Test
    void testRemoveUsersFromGroupsPartitionsRequests() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(5, underTest.maxSubjectsPerRequest * 2);

        Multimap<String, String> warnings = ArrayListMultimap.create();
        doNothing().when(freeIpaClient).callBatch(any(), any(), any(), any());

        underTest.removeUsersFromGroups(true, freeIpaClient, groupMapping, warnings::put);

        assertTrue(warnings.isEmpty());
    }

    @Test
    void testRemoveUsersFromGroupsNullMembersInResponse() throws Exception {
        Multimap<String, String> groupMapping = setupGroupMapping(1, 1);

        Multimap<String, String> warnings = ArrayListMultimap.create();
        doNothing().when(freeIpaClient).callBatch(any(), any(), any(), any());

        underTest.removeUsersFromGroups(true, freeIpaClient, groupMapping, warnings::put);

        assertTrue(warnings.isEmpty());
    }

    @Test
    void testAsyncSynchronizeUsersUsesInternalCrn() {
        Stack stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stackService.getMultipleByEnvironmentCrnOrChildEnvironmantCrnAndAccountId(anySet(), anyString())).thenReturn(List.of(stack));
        Operation operation = mock(Operation.class);
        when(operation.getStatus()).thenReturn(OperationState.RUNNING);
        when(operation.getOperationId()).thenReturn("operationId");
        when(operationService.startOperation(anyString(), any(OperationType.class), anyCollection(), anyCollection()))
                .thenReturn(operation);
        UserSyncStatus userSyncStatus = mock(UserSyncStatus.class);
        when(userSyncStatusService.getOrCreateForStack(any(Stack.class))).thenReturn(userSyncStatus);
        when(userSyncStatusService.save(userSyncStatus)).thenReturn(userSyncStatus);

        UserSyncService spyService = spy(underTest);

        doAnswer(invocation -> {
            assertEquals(INTERNAL_ACTOR_CRN, ThreadBasedUserCrnProvider.getUserCrn());
            return null;
        }).when(spyService).asyncSynchronizeUsers(anyString(), anyString(), anyString(), anyList(), any(), any());

        spyService.synchronizeUsers("accountId", "actorCrn", Set.of(), Set.of(), Set.of(), false);

        verify(spyService).asyncSynchronizeUsers(anyString(), anyString(), anyString(), anyList(), any(), any());
    }

    @Test
    void testApplyStateDifferenceToIpa() throws FreeIpaClientException {
        FmsGroup groupToAdd1 = new FmsGroup().withName("groupToAdd1");
        FmsGroup groupToAdd2 = new FmsGroup().withName("groupToAdd2");
        FmsGroup groupToRemove1 = new FmsGroup().withName("groupToRemove1");
        FmsGroup groupToRemove2 = new FmsGroup().withName("groupToRemove2");
        FmsUser userToAdd1 = new FmsUser().withName("userToAdd1").withFirstName("clark").withLastName("kent");
        FmsUser userToAdd2 = new FmsUser().withName("userToAdd2").withFirstName("peter").withLastName("parker");
        String userToRemove1 = "userToRemove1";
        String userToRemove2 = "userToRemove2";
        Multimap<String, String> warnings = ArrayListMultimap.create();

        doNothing().when(freeIpaClient).callBatch(any(), any(), any(), any());

        UsersStateDifference usersStateDifference = new UsersStateDifference(
                ImmutableSet.of(groupToAdd1, groupToAdd2),
                ImmutableSet.of(groupToRemove1, groupToRemove2),
                ImmutableSet.of(userToAdd1, userToAdd2),
                ImmutableSet.of(),
                ImmutableSet.of(userToRemove1, userToRemove2),
                ImmutableMultimap.<String, String>builder()
                        .put(groupToAdd1.getName(), userToAdd1.getName())
                        .put(groupToAdd2.getName(), userToAdd2.getName())
                        .build(),
                ImmutableMultimap.<String, String>builder()
                        .put(groupToRemove1.getName(), userToRemove1)
                        .put(groupToRemove2.getName(), userToRemove2)
                        .build()
        );

        underTest.applyStateDifferenceToIpa(ENV_CRN, freeIpaClient, usersStateDifference, warnings::put, true);

        verify(freeIpaClient, times(6)).callBatch(any(), any(), any(), any());

        verifyNoMoreInteractions(freeIpaClient);
    }

    private Multimap<String, String> setupGroupMapping(int numGroups, int numPerGroup) {
        Multimap<String, String> groupMapping = HashMultimap.create();
        for (int i = 0; i < numGroups; ++i) {
            String group = "group" + i;
            for (int j = 0; j < numPerGroup; ++j) {
                groupMapping.put(group, "user" + j);
            }
        }
        return groupMapping;
    }
}