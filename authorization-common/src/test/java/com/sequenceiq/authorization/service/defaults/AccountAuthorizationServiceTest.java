package com.sequenceiq.authorization.service.defaults;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.lang.annotation.Annotation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.AccountAuthorizationService;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;

@ExtendWith(MockitoExtension.class)
public class AccountAuthorizationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @InjectMocks
    private AccountAuthorizationService underTest;

    @Test
    public void testCheckPermissions() {
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUser(any(), anyString());

        CheckPermissionByAccount methodAnnotation = new CheckPermissionByAccount() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByAccount.class;
            }

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.CREATE_ENVIRONMENT;
            }
        };
        underTest.authorize(methodAnnotation, USER_CRN);

        verify(commonPermissionCheckingUtils).checkPermissionForUser(eq(AuthorizationResourceAction.CREATE_ENVIRONMENT), eq(USER_CRN));
    }
}
