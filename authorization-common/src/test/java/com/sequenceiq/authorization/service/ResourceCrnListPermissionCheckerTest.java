package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@RunWith(MockitoJUnitRunner.class)
public class ResourceCrnListPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @InjectMocks
    private ResourceCrnListPermissionChecker underTest;

    @Test
    public void testCheckPermissions() {
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResources(any(), anyString(), any());
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(Lists.newArrayList(USER_CRN, USER_CRN));

        CheckPermissionByResourceCrnList rawMethodAnnotation = new CheckPermissionByResourceCrnList() {

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.EDIT_CREDENTIAL;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceCrnList.class;
            }
        };
        underTest.checkPermissions(rawMethodAnnotation, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceCrnList.class), eq(Collection.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), anyString());
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResources(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), any());
    }
}
