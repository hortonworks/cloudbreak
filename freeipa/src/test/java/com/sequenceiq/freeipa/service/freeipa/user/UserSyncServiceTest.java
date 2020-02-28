package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.entity.Operation;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.UserSyncStatus;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;
import com.sequenceiq.freeipa.service.operation.OperationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class UserSyncServiceTest {
    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String NOT_CRN = "not:a:crn:";

    private static final String OTHER_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":database:" + UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String MACHINE_USER_CRN = "crn:cdp:iam:us-west-1:"
            + ACCOUNT_ID + ":machineUser:" + UUID.randomUUID().toString();

    private static final String INTERNAL_USER_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private static final int MAX_SUBJECTS_PER_REQUEST = 10;

    @Mock
    StackService stackService;

    @Mock
    OperationService operationService;

    @Mock
    FreeIpaUsersStateProvider freeIpaUsersStateProvider;

    @Mock
    UserSyncStatusService userSyncStatusService;

    @InjectMocks
    UserSyncService underTest;

    @BeforeEach
    void setUp() {
        underTest.maxSubjectsPerRequest = MAX_SUBJECTS_PER_REQUEST;
    }

    @Test
    void testValidateParameters() {
        underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(ENV_CRN), Set.of(USER_CRN), Set.of(MACHINE_USER_CRN));
    }

    @Test
    void testValidateParametersBadEnv() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(OTHER_CRN), Set.of(), Set.of());
        });
    }

    @Test
    void testValidateParametersBadUser() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(), Set.of(OTHER_CRN), Set.of());
        });
    }

    @Test
    void testValidateParametersBadMachineUser() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateParameters(ACCOUNT_ID, USER_CRN, Set.of(), Set.of(), Set.of(OTHER_CRN));
        });
    }

    @Test
    void testValidateCrnFilter() {
        underTest.validateCrnFilter(Set.of(ENV_CRN), Crn.ResourceType.ENVIRONMENT);
    }

    @Test
    void testValidateCrnFilterNotCrn() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateCrnFilter(Set.of(NOT_CRN), Crn.ResourceType.ENVIRONMENT);
        });
    }

    @Test
    void testValidateCrnFilterWrongResourceType() {
        Assertions.assertThrows(BadRequestException.class, () -> {
            underTest.validateCrnFilter(Set.of(ENV_CRN), Crn.ResourceType.USER);
        });
    }

    @Test
    void testFullSyncRetrievesFullIpaState() throws Exception {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        underTest.getIpaUserState(freeIpaClient, umsUsersState, true);
        verify(freeIpaUsersStateProvider).getUsersState(any());
    }

    @Test
    void testFilteredSyncRetrievesFilteredIpaState() throws Exception {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        ImmutableSet<FmsUser> workloadUsers = mock(ImmutableSet.class);
        when(umsUsersState.getRequestedWorkloadUsers()).thenReturn(workloadUsers);
        underTest.getIpaUserState(freeIpaClient, umsUsersState, false);
        verify(freeIpaUsersStateProvider).getFilteredFreeIPAState(any(), eq(workloadUsers));
    }

    @Test
    void testAddUsersToGroupsPartitionsReqeusts() throws Exception {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        RPCResponse<Object> response = mock(RPCResponse.class);
        when(freeIpaClient.groupAddMembers(any(), any())).thenReturn(response);

        Multimap<String, String> groupMapping = setupGroupMapping(5, underTest.maxSubjectsPerRequest * 2);
        underTest.addUsersToGroups(freeIpaClient, groupMapping);

        groupMapping.keySet().stream().forEach(group -> {
            try {
                verify(freeIpaClient, times(2)).groupAddMembers(eq(group), any());
            } catch (FreeIpaClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testRemoveUsersFromGroupsPartitionsRequests() throws Exception {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        RPCResponse<Object> response = mock(RPCResponse.class);
        when(freeIpaClient.groupRemoveMembers(any(), any())).thenReturn(response);

        Multimap<String, String> groupMapping = setupGroupMapping(5, underTest.maxSubjectsPerRequest * 2);
        underTest.removeUsersFromGroups(freeIpaClient, groupMapping);

        groupMapping.keySet().stream().forEach(group -> {
            try {
                verify(freeIpaClient, times(2)).groupRemoveMembers(eq(group), any());
            } catch (FreeIpaClientException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    void testAsyncSynchronizeUsersUsesInternalCrn() {
        Stack stack = mock(Stack.class);
        when(stack.getEnvironmentCrn()).thenReturn(ENV_CRN);
        when(stackService.getMultipleByEnvironmentCrnAndAccountId(anySet(), anyString())).thenReturn(List.of(stack));
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
            assertEquals(INTERNAL_USER_CRN, ThreadBasedUserCrnProvider.getUserCrn());
            return null;
        })
                .when(spyService).asyncSynchronizeUsers(anyString(), anyString(), anyString(), anyList(), anySet(), anySet(), anyBoolean());

        spyService.synchronizeUsers("accountId", "actorCrn",
                Set.of(), Set.of(), Set.of());

        verify(spyService).asyncSynchronizeUsers(anyString(), anyString(), anyString(), anyList(), anySet(), anySet(), anyBoolean());
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
