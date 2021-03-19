package com.sequenceiq.freeipa.service.freeipa.user.ums;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MachineUserRetrieverTest {

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private MachineUserRetriever underTest;

    private UserSyncTestData testData = new UserSyncTestData();

    @Test
    void getMachineUsersFull() {
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();

        when(grpcUmsClient.listAllMachineUsers(anyString(), anyString(), anyBoolean(), anyBoolean(), any(Optional.class)))
                .thenReturn(testData.getMachineUsers());

        List<UserManagementProto.MachineUser> machineUsers = underTest.getMachineUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, true, ImmutableSet.of(), warnings::put);

        assertEquals(testData.getMachineUsers(), machineUsers);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void getMachineUsersSingle() {
        setupListMachineUsersMock();
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();
        UserManagementProto.MachineUser expectedMachineUser = testData.getMachineUsers().get(0);

        List<UserManagementProto.MachineUser> machineUsers = underTest.getMachineUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, false,
                ImmutableSet.of(expectedMachineUser.getCrn()), warnings::put);

        assertEquals(1, machineUsers.size());
        assertEquals(expectedMachineUser, machineUsers.get(0));
        verify(grpcUmsClient).listMachineUsers(eq(INTERNAL_ACTOR_CRN), eq(testData.getAccountId()),
                eq(List.of(expectedMachineUser.getCrn())),
                eq(MachineUserRetriever.DONT_INCLUDE_INTERNAL_MACHINE_USERS),
                eq(MachineUserRetriever.INCLUDE_WORKLOAD_MACHINE_USERS),
                eq(requestId));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void getMachineUsersMultiple() {
        setupListMachineUsersMock();
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();
        List<UserManagementProto.MachineUser> expectedMachineUsers = testData.getMachineUsers()
                .subList(0, 3);

        Set<String> requestedCrns = expectedMachineUsers.stream()
                .map(UserManagementProto.MachineUser::getCrn)
                .collect(Collectors.toSet());

        List<UserManagementProto.MachineUser> machineUsers = underTest.getMachineUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, false,
                requestedCrns, warnings::put);

        assertEquals(expectedMachineUsers.size(), machineUsers.size());
        assertTrue(expectedMachineUsers.containsAll(machineUsers));
        assertTrue(machineUsers.containsAll(expectedMachineUsers));
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(grpcUmsClient).listMachineUsers(eq(INTERNAL_ACTOR_CRN), eq(testData.getAccountId()),
                captor.capture(),
                eq(MachineUserRetriever.DONT_INCLUDE_INTERNAL_MACHINE_USERS),
                eq(MachineUserRetriever.INCLUDE_WORKLOAD_MACHINE_USERS),
                eq(requestId));
        List<String> capturedCrns = captor.getValue();
        assertTrue(requestedCrns.containsAll(capturedCrns));
        assertTrue(capturedCrns.containsAll(requestedCrns));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void getMachineUsersMultipleWithMissing() {
        setupListMachineUsersMock();
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();
        List<UserManagementProto.MachineUser> expectedMachineUsers = testData.getMachineUsers()
                .subList(0, 3);

        Set<String> requestedCrns = expectedMachineUsers.stream()
                .map(UserManagementProto.MachineUser::getCrn)
                .collect(Collectors.toSet());

        String extraCrn = Crn.builder(CrnResourceDescriptor.MACHINE_USER)
                .setAccountId(testData.getAccountId())
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
        requestedCrns.add(extraCrn);

        List<UserManagementProto.MachineUser> machineUsers = underTest.getMachineUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, false,
                requestedCrns, warnings::put);

        assertEquals(expectedMachineUsers.size(), machineUsers.size());
        assertTrue(expectedMachineUsers.containsAll(machineUsers));
        assertTrue(machineUsers.containsAll(expectedMachineUsers));
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(grpcUmsClient, times(requestedCrns.size() + 1)).listMachineUsers(
                eq(INTERNAL_ACTOR_CRN),
                eq(testData.getAccountId()),
                captor.capture(),
                eq(MachineUserRetriever.DONT_INCLUDE_INTERNAL_MACHINE_USERS),
                eq(MachineUserRetriever.INCLUDE_WORKLOAD_MACHINE_USERS),
                eq(requestId));
        List<List<String>> capturedArguments = captor.getAllValues();
        assertEquals(requestedCrns.size() + 1, capturedArguments.size());
        List<String> capturedCrns = capturedArguments.get(0);
        assertTrue(requestedCrns.containsAll(capturedCrns));
        assertTrue(capturedCrns.containsAll(requestedCrns));
        capturedArguments.subList(1, capturedArguments.size()).forEach(capturedCrn -> {
            assertEquals(1, capturedCrn.size());
            assertTrue(requestedCrns.contains(capturedCrn.get(0)));
        });
        assertEquals(1, warnings.size());
        assertTrue(warnings.containsKey(extraCrn));
    }

    @Test
    void getMachineUsersEmpty() {
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();

        List<UserManagementProto.MachineUser> machineUsers = underTest.getMachineUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, false, ImmutableSet.of(), warnings::put);

        assertEquals(ImmutableList.of(), machineUsers);
        assertTrue(warnings.isEmpty());
    }

    private void setupListMachineUsersMock() {
        doAnswer(new Answer<List<UserManagementProto.MachineUser>>() {
            @Override
            public List<UserManagementProto.MachineUser> answer(InvocationOnMock invocation) throws Throwable {
                List<UserManagementProto.MachineUser> answer;

                List<String> requestedMachineUserCrns = invocation.getArgument(2);
                if (requestedMachineUserCrns.isEmpty()) {
                    answer = testData.getMachineUsers();
                } else {
                    answer = testData.getMachineUsers().stream()
                            .filter(machineUser -> requestedMachineUserCrns.contains(machineUser.getCrn()))
                            .collect(Collectors.toList());
                    if (requestedMachineUserCrns.size() > answer.size()) {
                        throw new StatusRuntimeException(Status.NOT_FOUND);
                    }
                }
                return answer;
            }
        }).when(grpcUmsClient).listMachineUsers(anyString(), anyString(), anyList(),
                anyBoolean(), anyBoolean(), any(Optional.class));
    }
}