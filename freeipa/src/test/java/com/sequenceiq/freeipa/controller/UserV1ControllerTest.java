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

import com.sequenceiq.freeipa.api.v1.freeipa.user.model.CreateUsersRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SetPasswordRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.user.model.SynchronizeUsersRequest;
import com.sequenceiq.freeipa.service.user.PasswordService;
import com.sequenceiq.freeipa.service.user.UserService;
import com.sequenceiq.freeipa.util.CrnService;

@ExtendWith(MockitoExtension.class)
public class UserV1ControllerTest {

    private static final String ACCOUNT_ID = "accountId";

    @InjectMocks
    private UserV1Controller underTest;

    @Mock
    private UserService userService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private CrnService crnService;

    @Test
    void synchronizeUsers() {
        SynchronizeUsersRequest request = mock(SynchronizeUsersRequest.class);

        underTest.synchronizeUsers(request);

        verify(userService, times(1)).synchronizeUsers(request);
    }

    @Test
    void getStatus() {
        String syncId = "testId";

        underTest.getStatus(syncId);

        verify(userService, times(1)).getSynchronizeUsersStatus(syncId);
    }

    @Test
    void setPassword() {
        String username = "username";
        String password = "password";
        SetPasswordRequest request = mock(SetPasswordRequest.class);
        when(request.getPassword()).thenReturn(password);

        underTest.setPassword(username, request);

        verify(passwordService, times(1)).setPassword(username, password);
    }

    @Test
    void createUsers() throws Exception {
        when(crnService.getCurrentAccountId()).thenReturn(ACCOUNT_ID);
        CreateUsersRequest request = mock(CreateUsersRequest.class);

        underTest.createUsers(request);

        verify(userService, times(1)).createUsers(request, ACCOUNT_ID);
    }
}
