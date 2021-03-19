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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRetrieverTest {

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @InjectMocks
    private UserRetriever underTest;

    private UserSyncTestData testData = new UserSyncTestData();

    @Test
    void getUsersFull() {
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();

        when(grpcUmsClient.listAllUsers(anyString(), anyString(), any(Optional.class)))
                .thenReturn(testData.getUsers());

        List<UserManagementProto.User> users = underTest.getUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, true, ImmutableSet.of(), warnings::put);

        assertEquals(testData.getUsers(), users);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void getUsersSingle() {
        setupListUsersMock();
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();
        UserManagementProto.User expectedUser = testData.getUsers().get(0);

        List<UserManagementProto.User> users = underTest.getUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, false,
                ImmutableSet.of(expectedUser.getCrn()), warnings::put);

        assertEquals(1, users.size());
        assertEquals(expectedUser, users.get(0));
        verify(grpcUmsClient).listUsers(eq(INTERNAL_ACTOR_CRN), eq(testData.getAccountId()),
                eq(List.of(expectedUser.getCrn())),
                eq(requestId));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void getUsersMultiple() {
        setupListUsersMock();
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();
        List<UserManagementProto.User> expectedUsers = testData.getUsers()
                .subList(0, 3);

        Set<String> requestedCrns = expectedUsers.stream()
                .map(UserManagementProto.User::getCrn)
                .collect(Collectors.toSet());

        List<UserManagementProto.User> users = underTest.getUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, false,
                requestedCrns, warnings::put);

        assertEquals(expectedUsers.size(), users.size());
        assertTrue(expectedUsers.containsAll(users));
        assertTrue(users.containsAll(expectedUsers));
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(grpcUmsClient).listUsers(eq(INTERNAL_ACTOR_CRN), eq(testData.getAccountId()),
                captor.capture(),
                eq(requestId));
        List<String> capturedCrns = captor.getValue();
        assertTrue(requestedCrns.containsAll(capturedCrns));
        assertTrue(capturedCrns.containsAll(requestedCrns));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void getUsersMultipleWithMissing() {
        setupListUsersMock();
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();
        List<UserManagementProto.User> expectedUsers = testData.getUsers()
                .subList(0, 3);

        Set<String> requestedCrns = expectedUsers.stream()
                .map(UserManagementProto.User::getCrn)
                .collect(Collectors.toSet());

        String extraCrn = Crn.builder(CrnResourceDescriptor.USER)
                .setAccountId(testData.getAccountId())
                .setResource(UUID.randomUUID().toString())
                .build()
                .toString();
        requestedCrns.add(extraCrn);

        List<UserManagementProto.User> users = underTest.getUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, false,
                requestedCrns, warnings::put);

        assertEquals(expectedUsers.size(), users.size());
        assertTrue(expectedUsers.containsAll(users));
        assertTrue(users.containsAll(expectedUsers));
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(grpcUmsClient, times(requestedCrns.size() + 1)).listUsers(
                eq(INTERNAL_ACTOR_CRN),
                eq(testData.getAccountId()),
                captor.capture(),
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
    void getUsersEmpty() {
        Optional<String> requestId = Optional.of(UUID.randomUUID().toString());
        Multimap<String, String> warnings = ArrayListMultimap.create();

        List<UserManagementProto.User> users = underTest.getUsers(
                INTERNAL_ACTOR_CRN, testData.getAccountId(), requestId, false, ImmutableSet.of(), warnings::put);

        assertEquals(ImmutableList.of(), users);
        assertTrue(warnings.isEmpty());
    }

    private void setupListUsersMock() {
        doAnswer(new Answer<List<UserManagementProto.User>>() {
            @Override
            public List<UserManagementProto.User> answer(InvocationOnMock invocation) throws Throwable {
                List<UserManagementProto.User> answer;

                List<String> requestedUserCrns = invocation.getArgument(2);
                if (requestedUserCrns.isEmpty()) {
                    answer = testData.getUsers();
                } else {
                    answer = testData.getUsers().stream()
                            .filter(user -> requestedUserCrns.contains(user.getCrn()))
                            .collect(Collectors.toList());
                    if (requestedUserCrns.size() > answer.size()) {
                        throw new StatusRuntimeException(Status.NOT_FOUND);
                    }
                }
                return answer;
            }
        }).when(grpcUmsClient).listUsers(anyString(), anyString(), anyList(), any(Optional.class));
    }
}