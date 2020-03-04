package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;

@RunWith(MockitoJUnitRunner.class)
public class ResourceCrnPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @InjectMocks
    private ResourceCrnPermissionChecker underTest;

    @Test
    public void setCheckPermissions() {
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(null);
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), any(), anyString(), anyString());
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(USER_CRN);

        CheckPermissionByResourceCrn rawMethodAnnotation = new CheckPermissionByResourceCrn() {

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.WRITE;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceCrn.class;
            }
        };
        underTest.checkPermissions(rawMethodAnnotation, AuthorizationResourceType.CREDENTIAL, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceCrn.class), eq(String.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), any(), anyString());
        verify(commonPermissionCheckingUtils)
                .checkPermissionForUserOnResource(eq(AuthorizationResourceType.CREDENTIAL), eq(AuthorizationResourceAction.WRITE), eq(USER_CRN), anyString());
    }

    @Test
    public void testGetSupportedAnnotation() {
        assertEquals(CheckPermissionByResourceCrn.class, underTest.supportedAnnotation());
    }
}
