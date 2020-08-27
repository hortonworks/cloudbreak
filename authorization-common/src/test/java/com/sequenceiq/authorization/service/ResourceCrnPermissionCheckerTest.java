package com.sequenceiq.authorization.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.collection.IsMapContaining;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

@RunWith(MockitoJUnitRunner.class)
public class ResourceCrnPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @InjectMocks
    private ResourceCrnPermissionChecker underTest;

    @Test
    public void testCheckPermissions() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(USER_CRN);
        ResourceBasedCrnProvider resourceBasedCrnProvider = mock(ResourceBasedCrnProvider.class);
        when(resourceBasedCrnProvider.getEnvironmentCrnByResourceCrn(anyString())).thenReturn(Optional.empty());
        when(commonPermissionCheckingUtils.getResourceBasedCrnProvider(AuthorizationResourceAction.EDIT_CREDENTIAL)).thenReturn(resourceBasedCrnProvider);

        CheckPermissionByResourceCrn rawMethodAnnotation = new CheckPermissionByResourceCrn() {

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.EDIT_CREDENTIAL;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceCrn.class;
            }
        };
        underTest.checkPermissions(rawMethodAnnotation, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceCrn.class), eq(String.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), anyString());
        ArgumentCaptor<Map<String, AuthorizationResourceAction>> captor = ArgumentCaptor.forClass(Map.class);
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(captor.capture(), eq(USER_CRN));
        Map<String, AuthorizationResourceAction> capturedActions = captor.getValue();
        assertEquals(1, capturedActions.keySet().size());
        assertThat(capturedActions, IsMapContaining.hasEntry(USER_CRN, AuthorizationResourceAction.EDIT_CREDENTIAL));
    }

    @Test
    public void testCheckPermissionsWithDatahub() {
        String datahubCrn = "crn:cdp:datalake:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:datahub:614a791a-a100-4f83-8c65-968fe9b06d47";
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(datahubCrn);
        ResourceBasedCrnProvider resourceBasedCrnProvider = mock(ResourceBasedCrnProvider.class);
        String envCrn = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";
        when(resourceBasedCrnProvider.getEnvironmentCrnByResourceCrn(anyString())).thenReturn(Optional.of(envCrn));
        when(commonPermissionCheckingUtils.getResourceBasedCrnProvider(AuthorizationResourceAction.DELETE_DATAHUB)).thenReturn(resourceBasedCrnProvider);

        CheckPermissionByResourceCrn rawMethodAnnotation = new CheckPermissionByResourceCrn() {

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.DELETE_DATAHUB;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceCrn.class;
            }
        };
        underTest.checkPermissions(rawMethodAnnotation, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceCrn.class), eq(String.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), anyString());
        ArgumentCaptor<Map<String, AuthorizationResourceAction>> captor = ArgumentCaptor.forClass(Map.class);
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(captor.capture(), eq(USER_CRN));
        Map<String, AuthorizationResourceAction> capturedActions = captor.getValue();
        assertEquals(2, capturedActions.keySet().size());
        assertThat(capturedActions, IsMapContaining.hasEntry(datahubCrn, AuthorizationResourceAction.DELETE_DATAHUB));
        assertThat(capturedActions, IsMapContaining.hasEntry(envCrn, AuthorizationResourceAction.DELETE_DATAHUB));
    }
}
