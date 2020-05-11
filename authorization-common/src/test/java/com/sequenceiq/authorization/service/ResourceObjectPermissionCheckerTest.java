package com.sequenceiq.authorization.service;

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

import javax.ws.rs.ForbiddenException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObject;
import com.sequenceiq.authorization.annotation.ResourceObjectField;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
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

    @InjectMocks
    private ResourceObjectPermissionChecker underTest;

    @Before
    public void init() {
        when(commonPermissionCheckingUtils.getResourceBasedCrnProvider(any())).thenReturn(resourceBasedCrnProvider);
    }

    @Test
    public void testCheckPermissionsWithResourceObjectWithoutFieldAnnotation() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithoutAnnotation());

        underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(Object.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUserOnResource(any(), anyString(), anyString());
        verify(resourceBasedCrnProvider, times(0)).getResourceCrnByResourceName(anyString());
    }

    @Test
    public void testCheckPermissionsWithResourceObjectWithFieldAnnotationOnNonStringField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithNonStringAnnotation());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Annotated field within resource object is not string, thus access is denied!");

        underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils, times(0)).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(Object.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), anyString());
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUserOnResource(any(), anyString(), anyString());
        verify(resourceBasedCrnProvider, times(0)).getResourceCrnByResourceName(anyString());
    }

    @Test
    public void testCheckPermissionsWithResourceObjectWithFieldAnnotationOnCrnStringField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithCrnAnnotation());
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), anyString(), anyString());

        underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(Object.class));
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider, times(0)).getResourceCrnByResourceName(anyString());
    }

    @Test
    public void testCheckPermissionsWithResourceObjectWithFieldAnnotationOnNameStringField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithNameAnnotation());
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), anyString(), anyString());

        underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(Object.class));
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq("resource"));
    }

    @Test
    public void testCheckPermissionsWithResourceObjectWhenOtherExceptionOccurs() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithNameAnnotation());
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);
        doThrow(new ForbiddenException("some error")).when(commonPermissionCheckingUtils)
                .checkPermissionForUserOnResource(any(), anyString(), anyString());

        thrown.expect(ForbiddenException.class);
        thrown.expectMessage("some error");

        underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(Object.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), anyString());
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq("resource"));
    }

    @Test
    public void testCheckPermissionsWithResourceObjectWhenAccessDeniedExceptionOccurs() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new ResourceObjectWithNameAnnotation());
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);
        doThrow(new AccessDeniedException("get out!")).when(commonPermissionCheckingUtils)
                .checkPermissionForUserOnResource(any(), anyString(), anyString());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("get out!");

        underTest.checkPermissions(getAnnotation(), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(ResourceObject.class), eq(Object.class));
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), anyString());
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq("resource"));
    }

    private CheckPermissionByResourceObject getAnnotation() {
        return new CheckPermissionByResourceObject() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByResourceObject.class;
            }
        };
    }

    private static class ResourceObjectWithoutAnnotation {
        private String fieldWithoutAnnotation = "";

        public String getFieldWithoutAnnotation() {
            return fieldWithoutAnnotation;
        }
    }

    private static class ResourceObjectWithNameAnnotation {
        @ResourceObjectField(action = AuthorizationResourceAction.EDIT_CREDENTIAL, variableType = AuthorizationVariableType.NAME)
        private String field = "resource";

        public String getField() {
            return field;
        }
    }

    private static class ResourceObjectWithCrnAnnotation {
        @ResourceObjectField(action = AuthorizationResourceAction.EDIT_CREDENTIAL, variableType = AuthorizationVariableType.CRN)
        private String field = RESOURCE_CRN;

        public String getField() {
            return field;
        }
    }

    private static class ResourceObjectWithNonStringAnnotation {
        @ResourceObjectField(action = AuthorizationResourceAction.EDIT_CREDENTIAL, variableType = AuthorizationVariableType.CRN)
        private Object field;

        public Object getField() {
            return field;
        }
    }
}
