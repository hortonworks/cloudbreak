package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Annotation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@RunWith(MockitoJUnitRunner.class)
public class DefaultPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @InjectMocks
    private DefaultPermissionChecker underTest;

    @Test
    public void testCheckPermissions() {
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUser(any(), anyString());

        CheckPermissionByAccount rawMethodAnnotation = new CheckPermissionByAccount() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByAccount.class;
            }

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.ENVIRONMENT_WRITE;
            }
        };
        underTest.checkPermissions(rawMethodAnnotation, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils)
                .checkPermissionForUser(eq(AuthorizationResourceAction.ENVIRONMENT_WRITE), eq(USER_CRN));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUserOnResource(any(), anyString(), anyString());
    }
}
