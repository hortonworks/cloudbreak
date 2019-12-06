package com.sequenceiq.freeipa.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SyncOperationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizationStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.controller.exception.SyncOperationAlreadyRunningException;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;
import com.sequenceiq.freeipa.service.operation.OperationStatusService;

@ExtendWith(MockitoExtension.class)
public class UserV1ControllerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String MACHINE_USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":machineUser:" + UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environment:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    @InjectMocks
    private UserV1Controller underTest;

    @Mock
    private UserService userService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private OperationStatusService operationStatusService;

    @Test
    void synchronizeUser() {
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(userService.synchronizeUsers(any(), any(), any(), any(), any())).thenReturn(status);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeUser(request));

        verify(userService, times(1)).synchronizeUsers(ACCOUNT_ID, USER_CRN, Set.of(), Set.of(USER_CRN), Set.of());
    }

    @Test
    void synchronizeUserMachineUser() {
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(userService.synchronizeUsers(any(), any(), any(), any(), any())).thenReturn(status);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        ThreadBasedUserCrnProvider.doAs(MACHINE_USER_CRN, () -> underTest.synchronizeUser(request));

        verify(userService, times(1)).synchronizeUsers(ACCOUNT_ID, MACHINE_USER_CRN, Set.of(), Set.of(), Set.of(MACHINE_USER_CRN));
    }

    @Test
    void synchronizeUserRejected() {
        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(userService.synchronizeUsers(ACCOUNT_ID, USER_CRN, Set.of(), Set.of(USER_CRN), Set.of())).thenReturn(status);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        assertThrows(SyncOperationAlreadyRunningException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeUser(request)));
    }

    @Test
    void synchronizeAllUsers() {
        Set<String> environments = Set.of(ENV_CRN);
        Set<String> users = Set.of(USER_CRN);
        Set<String> machineUsers = Set.of(MACHINE_USER_CRN);
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setEnvironments(environments);
        request.setUsers(users);
        request.setMachineUsers(machineUsers);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(userService.synchronizeUsers(any(), any(), any(), any(), any())).thenReturn(status);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeAllUsers(request));

        verify(userService, times(1)).synchronizeUsers(ACCOUNT_ID, USER_CRN, environments, users, machineUsers);
    }

    @Test
    void synchronizeAllUsersRejected() {
        Set<String> environments = Set.of(ENV_CRN);
        Set<String> users = Set.of(USER_CRN);
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setEnvironments(environments);
        request.setUsers(users);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(userService.synchronizeUsers(ACCOUNT_ID, USER_CRN, environments, users, Set.of())).thenReturn(status);

        assertThrows(SyncOperationAlreadyRunningException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.synchronizeAllUsers(request)));
    }

    @Test
    void getStatus() {
        String operationId = "testId";

        underTest.getSyncOperationStatus(operationId);

        verify(operationStatusService, times(1)).getSyncOperationStatus(operationId);
    }

    @Test
    void getStatusRejected() {
        String operationId = "testId";

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(operationStatusService.getSyncOperationStatus(operationId)).thenReturn(status);

        underTest.getSyncOperationStatus(operationId);

        verify(operationStatusService, times(1)).getSyncOperationStatus(operationId);
        verify(status, never()).getStatus();
    }

    @Test
    void setPassword() {
        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(passwordService.setPassword(any(), any(), any(), any(), any())).thenReturn(status);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setPassword(request));

        verify(passwordService, times(1)).setPassword(ACCOUNT_ID, USER_CRN, USER_CRN, password, new HashSet<>());
    }

    @Test
    void setPasswordRejected() {
        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(passwordService.setPassword(ACCOUNT_ID, USER_CRN, USER_CRN, password, new HashSet<>())).thenReturn(status);

        assertThrows(SyncOperationAlreadyRunningException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setPassword(request)));
    }
}
