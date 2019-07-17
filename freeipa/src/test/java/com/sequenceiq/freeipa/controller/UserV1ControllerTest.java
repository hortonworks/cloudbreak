package com.sequenceiq.freeipa.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
import com.sequenceiq.freeipa.service.freeipa.user.SyncOperationStatusService;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;

@ExtendWith(MockitoExtension.class)
public class UserV1ControllerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:altus:environment:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    @InjectMocks
    private UserV1Controller underTest;

    @Mock
    private UserService userService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private SyncOperationStatusService syncOperationStatusService;

    @Mock
    private ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

    @Test
    void synchronizeUser() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(threadBaseUserCrnProvider.getAccountId()).thenReturn(ACCOUNT_ID);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(userService.synchronizeUser(ACCOUNT_ID, USER_CRN, USER_CRN)).thenReturn(status);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        underTest.synchronizeUser(request);

        verify(userService, times(1)).synchronizeUser(ACCOUNT_ID, USER_CRN, USER_CRN);
    }

    @Test
    void synchronizeUserRejected() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(threadBaseUserCrnProvider.getAccountId()).thenReturn(ACCOUNT_ID);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(userService.synchronizeUser(ACCOUNT_ID, USER_CRN, USER_CRN)).thenReturn(status);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        assertThrows(SyncOperationAlreadyRunningException.class, () -> underTest.synchronizeUser(request));
    }

    @Test
    void synchronizeAllUsers() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(threadBaseUserCrnProvider.getAccountId()).thenReturn(ACCOUNT_ID);

        Set<String> environments = Set.of(ENV_CRN);
        Set<String> users = Set.of(USER_CRN);
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setEnvironments(environments);
        request.setUsers(users);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(userService.synchronizeAllUsers(ACCOUNT_ID, USER_CRN, environments, users)).thenReturn(status);

        underTest.synchronizeAllUsers(request);

        verify(userService, times(1)).synchronizeAllUsers(ACCOUNT_ID, USER_CRN, environments, users);
    }

    @Test
    void synchronizeAllUsersRejected() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(threadBaseUserCrnProvider.getAccountId()).thenReturn(ACCOUNT_ID);

        Set<String> environments = Set.of(ENV_CRN);
        Set<String> users = Set.of(USER_CRN);
        SynchronizeAllUsersRequest request = new SynchronizeAllUsersRequest();
        request.setEnvironments(environments);
        request.setUsers(users);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(userService.synchronizeAllUsers(ACCOUNT_ID, USER_CRN, environments, users)).thenReturn(status);

        assertThrows(SyncOperationAlreadyRunningException.class, () -> underTest.synchronizeAllUsers(request));
    }

    @Test
    void getStatus() {
        String operationId = "testId";

        underTest.getSyncOperationStatus(operationId);

        verify(syncOperationStatusService, times(1)).getStatus(operationId);
    }

    @Test
    void getStatusRejected() {
        String operationId = "testId";

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(syncOperationStatusService.getStatus(operationId)).thenReturn(status);

        underTest.getSyncOperationStatus(operationId);

        verify(syncOperationStatusService, times(1)).getStatus(operationId);
        verify(status, never()).getStatus();
    }

    @Test
    void setPassword() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(threadBaseUserCrnProvider.getAccountId()).thenReturn(ACCOUNT_ID);

        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(passwordService.setPassword(ACCOUNT_ID, USER_CRN, password, new HashSet<>())).thenReturn(status);

        underTest.setPassword(request);

        verify(passwordService, times(1)).setPassword(ACCOUNT_ID, USER_CRN, password, new HashSet<>());
    }

    @Test
    void setPasswordRejected() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(threadBaseUserCrnProvider.getAccountId()).thenReturn(ACCOUNT_ID);

        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        SyncOperationStatus status = mock(SyncOperationStatus.class);
        when(status.getStatus()).thenReturn(SynchronizationStatus.REJECTED);
        when(passwordService.setPassword(ACCOUNT_ID, USER_CRN, password, new HashSet<>())).thenReturn(status);

        assertThrows(SyncOperationAlreadyRunningException.class, () -> underTest.setPassword(request));
    }
}
