package com.sequenceiq.cloudbreak.controller;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.OperationRetryService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StackV2ControllerTest {

    private static final String STACK_NAME = "stackName";

    @InjectMocks
    private StackV2Controller underTest;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private StackService stackService;

    @Mock
    private OperationRetryService operationRetryService;

    @Test
    public void retry() {
        IdentityUser identityUser = new IdentityUser("userId", "username", "account", Collections.emptyList(),
                "givenName", "familyName", Date.from(Instant.now()));
        when(authenticatedUserService.getCbUser()).thenReturn(identityUser);

        Stack stack = new Stack();
        when(stackService.getPublicStack(STACK_NAME, identityUser)).thenReturn(stack);

        underTest.retry(STACK_NAME);

        verify(operationRetryService, times(1)).retry(stack);
    }
}