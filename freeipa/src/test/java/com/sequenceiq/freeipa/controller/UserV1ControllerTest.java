package com.sequenceiq.freeipa.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.CreateUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeAllUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUserRequest;
import com.sequenceiq.freeipa.service.freeipa.user.PasswordService;
import com.sequenceiq.freeipa.service.freeipa.user.UserService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
public class UserV1ControllerTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String USER_CRN = ":crn:altus:iam:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:user:59cbfafb-999b-4df9-81e2-aa00a88382e9";

    @InjectMocks
    private UserV1Controller underTest;

    @Mock
    private UserService userService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private CrnService crnService;

    @Mock
    private ThreadBasedUserCrnProvider threadBaseUserCrnProvider;

    @Test
    void synchronizeUser() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);

        SynchronizeUserRequest request = mock(SynchronizeUserRequest.class);

        underTest.synchronizeUser(request);

        verify(userService, times(1)).synchronizeUser(ACCOUNT_ID, USER_CRN, USER_CRN);
    }

    @Test
    void synchronizeAllUsers() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);

        SynchronizeAllUsersRequest request = mock(SynchronizeAllUsersRequest.class);

        underTest.synchronizeAllUsers(request);

        verify(userService, times(1)).synchronizeAllUsers(ACCOUNT_ID, USER_CRN, request);
    }

    @Test
    void getStatus() {
        String syncId = "testId";

        underTest.getSynchronizationStatus(syncId);

        verify(userService, times(1)).getSynchronizeUsersStatus(syncId);
    }

    @Test
    void setPassword() {
        when(threadBaseUserCrnProvider.getUserCrn()).thenReturn(USER_CRN);
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);

        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        underTest.setPassword(request);

        verify(passwordService, times(1)).setPassword(ACCOUNT_ID, USER_CRN, password);
    }

    @Test
    void createUsers() throws Exception {
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);

        CreateUsersRequest request = mock(CreateUsersRequest.class);

        underTest.createUsers(request);

        verify(userService, times(1)).createUsers(ACCOUNT_ID, request);
    }
}
