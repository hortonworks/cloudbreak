package com.sequenceiq.freeipa.controller;

import static org.mockito.Mockito.mock;
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
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.freeipa.user.SyncOperationStatusService;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;

@ExtendWith(MockitoExtension.class)
public class UserV1ControllerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:altus:iam:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

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

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        underTest.synchronizeUser(request);

        verify(userService, times(1)).synchronizeUser(ACCOUNT_ID, USER_CRN, USER_CRN);
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

        underTest.synchronizeAllUsers(request);

        verify(userService, times(1)).synchronizeAllUsers(ACCOUNT_ID, USER_CRN, environments, users);
    }

    @Test
    void getStatus() {
        String operationId = "testId";

        underTest.getSyncOperationStatus(operationId);

        verify(syncOperationStatusService, times(1)).getStatus(operationId);
    }

    @Test
    void setPassword() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(threadBaseUserCrnProvider.getAccountId()).thenReturn(ACCOUNT_ID);

        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        underTest.setPassword(request);

        verify(passwordService, times(1)).setPassword(ACCOUNT_ID, USER_CRN, password, new HashSet<>());
    }
}
