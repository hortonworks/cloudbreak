package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Maps;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.resource.AuthorizableFieldInfoModel;
import com.sequenceiq.authorization.resource.AuthorizationApiRequest;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;

@RunWith(MockitoJUnitRunner.class)
public class ResourceObjectPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String RESOURCE_CRN = "crn:cdp:credential:us-west-1:1234:credential:5678";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceBasedCrnProvider resourceBasedCrnProvider;

    @Spy
    private List<ResourceBasedCrnProvider> resourceBasedCrnProviders = new ArrayList<ResourceBasedCrnProvider>();

    @InjectMocks
    private ResourceObjectPermissionChecker underTest;

    @Test
    public void setCheckPermissionsWithResourceObjectWithFieldAnnotationOnCrnStringField() {
        resourceBasedCrnProviders.add(resourceBasedCrnProvider);
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithCrn());
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), any(), anyString(), anyString());
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());

        underTest.populateResourceBasedCrnProviderMapMap();
        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.ENVIRONMENT, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).checkPermissionForUser(eq(AuthorizationResourceType.ENVIRONMENT),
                eq(AuthorizationResourceAction.READ), eq(USER_CRN));
        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(AuthorizationApiRequest.class));
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceType.CREDENTIAL), eq(AuthorizationResourceAction.WRITE),
                eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider, times(0)).getResourceCrnByResourceName(anyString());
    }

    @Test
    public void setCheckPermissionsWithResourceObjectWithFieldAnnotationOnNameStringField() {
        resourceBasedCrnProviders.add(resourceBasedCrnProvider);
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithName());
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);
        when(resourceBasedCrnProvider.getResourceType()).thenReturn(AuthorizationResourceType.CREDENTIAL);
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), any(), anyString(), anyString());
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());

        underTest.populateResourceBasedCrnProviderMapMap();
        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.ENVIRONMENT, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).checkPermissionForUser(eq(AuthorizationResourceType.ENVIRONMENT),
                eq(AuthorizationResourceAction.READ), eq(USER_CRN));
        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(AuthorizationApiRequest.class));
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceType.CREDENTIAL), eq(AuthorizationResourceAction.WRITE),
                eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq("resource"));
    }

    @Test
    public void setCheckPermissionsWithResourceObjectWhenOtherExceptionOccurs() {
        resourceBasedCrnProviders.add(resourceBasedCrnProvider);
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithName());
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);
        when(resourceBasedCrnProvider.getResourceType()).thenReturn(AuthorizationResourceType.CREDENTIAL);
        doThrow(new NullPointerException("valami")).when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), any(), anyString(), anyString());
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Error happened during permission check of resource object, thus access is denied!");

        underTest.populateResourceBasedCrnProviderMapMap();
        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.ENVIRONMENT, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).checkPermissionForUser(eq(AuthorizationResourceType.ENVIRONMENT),
                eq(AuthorizationResourceAction.READ), eq(USER_CRN));
        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(AuthorizationApiRequest.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), any(), anyString());
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceType.CREDENTIAL), eq(AuthorizationResourceAction.WRITE),
                eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq("resource"));
    }

    @Test
    public void setCheckPermissionsWithResourceObjectWhenAccessDeniedExceptionOccurs() {
        resourceBasedCrnProviders.add(resourceBasedCrnProvider);
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithName());
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);
        when(resourceBasedCrnProvider.getResourceType()).thenReturn(AuthorizationResourceType.CREDENTIAL);
        doThrow(new AccessDeniedException("get out!")).when(commonPermissionCheckingUtils)
                .checkPermissionForUserOnResource(any(), any(), anyString(), anyString());
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUser(any(), any(), anyString());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("get out!");

        underTest.populateResourceBasedCrnProviderMapMap();
        underTest.checkPermissions(getAnnotation(), AuthorizationResourceType.ENVIRONMENT, USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).checkPermissionForUser(eq(AuthorizationResourceType.ENVIRONMENT),
                eq(AuthorizationResourceAction.READ), eq(USER_CRN));
        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(AuthorizationApiRequest.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), any(), anyString());
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceType.CREDENTIAL), eq(AuthorizationResourceAction.WRITE),
                eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq("resource"));
    }

    @Test
    public void testGetSupportedAnnotation() {
        assertEquals(CheckPermissionByResourceObject.class, underTest.supportedAnnotation());
    }

    private CheckPermissionByResourceObject getAnnotation() {
        return new CheckPermissionByResourceObject() {

            @Override
            public AuthorizationResourceAction action() {
                return AuthorizationResourceAction.READ;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceObject.class;
            }
        };
    }

    private static class ResourceObjectWithName implements AuthorizationApiRequest {
        private String field = "resource";

        @Override
        public Map<String, AuthorizableFieldInfoModel> getAuthorizableFields() {
            Map<String, AuthorizableFieldInfoModel> authorizableFields = Maps.newHashMap();
            authorizableFields.put(field, new AuthorizableFieldInfoModel(AuthorizationResourceType.CREDENTIAL,
                    AuthorizationResourceAction.WRITE, AuthorizationVariableType.NAME));
            return authorizableFields;
        }
    }

    private static class ResourceObjectWithCrn implements AuthorizationApiRequest {
        private String field = RESOURCE_CRN;

        @Override
        public Map<String, AuthorizableFieldInfoModel> getAuthorizableFields() {
            Map<String, AuthorizableFieldInfoModel> authorizableFields = Maps.newHashMap();
            authorizableFields.put(field, new AuthorizableFieldInfoModel(AuthorizationResourceType.CREDENTIAL,
                    AuthorizationResourceAction.WRITE, AuthorizationVariableType.CRN));
            return authorizableFields;
        }
    }
}
