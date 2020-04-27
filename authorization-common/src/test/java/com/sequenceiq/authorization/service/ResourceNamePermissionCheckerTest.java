package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@RunWith(MockitoJUnitRunner.class)
public class ResourceNamePermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceBasedCrnProvider resourceBasedCrnProvider;

    @InjectMocks
    private ResourceNamePermissionChecker underTest;

    @Before
    public void init() {
        when(commonPermissionCheckingUtils.getResourceBasedCrnProvider(any())).thenReturn(resourceBasedCrnProvider);
    }

    @Test
    public void testCheckPermissions() {
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), anyString(), anyString());
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn("resource");
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(any())).thenReturn(USER_CRN);

        CheckPermissionByResourceName rawMethodAnnotation = new CheckPermissionByResourceName() {

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.EDIT_CREDENTIAL;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceName.class;
            }
        };

        underTest.checkPermissions(rawMethodAnnotation, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceName.class), eq(String.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), anyString());
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), anyString());
    }
}
