package com.sequenceiq.freeipa.service.freeipa.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.model.RPCResponse;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.service.freeipa.user.model.FmsUser;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
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

    @Mock
    FreeIpaUsersStateProvider freeIpaUsersStateProvider;

    @InjectMocks
    UserService underTest;

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
        underTest.getIpaUserState(freeIpaClient, umsUsersState, Set.of(), Set.of());
        verify(freeIpaUsersStateProvider).getUsersState(any());
    }

    @Test
    void testFilteredSyncRetrievesFilteredIpaState() throws Exception {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        UmsUsersState umsUsersState = mock(UmsUsersState.class);
        Set<FmsUser> workloadUsers = mock(Set.class);
        when(umsUsersState.getRequestedWorkloadUsers()).thenReturn(workloadUsers);
        underTest.getIpaUserState(freeIpaClient, umsUsersState, Set.of(USER_CRN), Set.of(MACHINE_USER_CRN));
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
