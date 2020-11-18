package com.sequenceiq.authorization.service;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DELETE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME_LIST;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.ForbiddenException;

import org.hamcrest.collection.IsMapContaining;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;

@RunWith(MockitoJUnitRunner.class)
public class RequestPropertyPermissionCheckerTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String RESOURCE_CRN = "crn:cdp:credential:us-west-1:1234:credential:5678";

    private static final String RESOURCE_NAME = "resource";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceBasedCrnProvider resourceBasedCrnProvider;

    @InjectMocks
    private RequestPropertyPermissionChecker underTest;

    @Before
    public void init() {
        when(commonPermissionCheckingUtils.getResourceBasedCrnProvider(any())).thenReturn(resourceBasedCrnProvider);
    }

    @AfterEach
    public void after() {
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(RequestObject.class), eq(Object.class));
    }

    @Test
    public void testCheckPermissionsOnNonStringRequestField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(Integer.valueOf(0)));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Referred property within request object is not string, thus access is denied!");

        underTest.checkPermissions(getAnnotation(CRN, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null, 0L);

        verifyResourceCheckInteraction(any(), anyString(), anyString());
        verifyZeroInteractionRegardingName();
    }

    @Test
    public void testCheckPermissionsOnCrnRequestField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(RESOURCE_CRN));
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), anyString());

        underTest.checkPermissions(getAnnotation(CRN, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null, 0L);

        ArgumentCaptor<Map<String, AuthorizationResourceAction>> captor = ArgumentCaptor.forClass(Map.class);
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(captor.capture(), eq(USER_CRN));
        Map<String, AuthorizationResourceAction> capturedActions = captor.getValue();
        assertEquals(1, capturedActions.keySet().size());
        assertThat(capturedActions, IsMapContaining.hasEntry(RESOURCE_CRN, AuthorizationResourceAction.EDIT_CREDENTIAL));
        verifyZeroInteractionRegardingName();
    }

    @Test
    public void testCheckPermissionsOnCrnRequestFieldIfEntitlementDisabled() {
        when(commonPermissionCheckingUtils.legacyAuthorizationNeeded()).thenReturn(Boolean.TRUE);
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(RESOURCE_CRN));
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), anyString(), anyString());

        underTest.checkPermissions(getAnnotation(CRN, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), eq(USER_CRN), eq(RESOURCE_CRN));
        verifyZeroInteractionRegardingName();
    }

    @Test
    public void testCheckPermissionsOnCrnListRequestField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(Lists.newArrayList(RESOURCE_CRN)));
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResources(any(), anyString(), any());

        underTest.checkPermissions(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResources(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), any());
        verifyZeroInteractionRegardingName();
    }

    @Test
    public void testCheckPermissionsOnNullRequestField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject());

        underTest.checkPermissions(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, true, "field"), USER_CRN, null, null, 0L);

        verifyZeroInteraction();
    }

    @Test
    public void testCheckPermissionsOnNullRequestFieldIfRequired() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Property [field] of request object is null and it should be authorized, thus should be filled in.");

        underTest.checkPermissions(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null, 0L);

        verifyZeroInteraction();
    }

    @Test
    public void testCheckPermissionsOnNestedNullRequestField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(new SampleRequestObject()));

        underTest.checkPermissions(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, true, "field.field"), USER_CRN, null, null, 0L);

        verifyZeroInteraction();
    }

    @Test
    public void testCheckPermissionsOnNestedNullRequestFieldIfRequired() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Property [field.field] of request object is null and it should be authorized, thus should be filled in.");

        underTest.checkPermissions(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, false, "field.field"), USER_CRN, null, null, 0L);

        verifyZeroInteraction();
    }

    @Test
    public void testHierarchicalCheckPermissionsOnNameRequestField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(RESOURCE_NAME));
        String datahubCrn = "crn:cdp:datalake:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:datahub:614a791a-a100-4f83-8c65-968fe9b06d47";
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(datahubCrn);
        String envCrn = "crn:cdp:environments:us-west-1:460c0d8f-ae8e-4dce-9cd7-2351762eb9ac:environment:6b2b1600-8ac6-4c26-aa34-dab36f4bd243";
        when(resourceBasedCrnProvider.getEnvironmentCrnByResourceCrn(eq(datahubCrn))).thenReturn(Optional.of(envCrn));
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), anyString());

        underTest.checkPermissions(getAnnotation(NAME, DELETE_DATAHUB, false, "field"), USER_CRN, null, null, 0L);

        ArgumentCaptor<Map<String, AuthorizationResourceAction>> captor = ArgumentCaptor.forClass(Map.class);
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(captor.capture(), eq(USER_CRN));
        Map<String, AuthorizationResourceAction> capturedActions = captor.getValue();
        assertEquals(2, capturedActions.keySet().size());
        assertThat(capturedActions, IsMapContaining.hasEntry(datahubCrn, AuthorizationResourceAction.DELETE_DATAHUB));
        assertThat(capturedActions, IsMapContaining.hasEntry(envCrn, AuthorizationResourceAction.DELETE_DATAHUB));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq(RESOURCE_NAME));
    }

    @Test
    public void testHierarchicalCheckPermissionsOnNameRequestFieldIfEntitlementDisabled() {
        when(commonPermissionCheckingUtils.legacyAuthorizationNeeded()).thenReturn(Boolean.TRUE);
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(RESOURCE_NAME));
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(eq(RESOURCE_NAME))).thenReturn(RESOURCE_CRN);
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), anyString(), anyString());

        underTest.checkPermissions(getAnnotation(NAME, DELETE_DATAHUB, false, "field"), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(any(), eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq(RESOURCE_NAME));
    }

    @Test
    public void testCheckPermissionsOnNameListRequestField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(Lists.newArrayList(RESOURCE_NAME)));
        when(resourceBasedCrnProvider.getResourceCrnListByResourceNameList(any())).thenReturn(Lists.newArrayList(RESOURCE_CRN));
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUserOnResources(any(), anyString(), any());

        underTest.checkPermissions(getAnnotation(NAME_LIST, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null, 0L);

        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResources(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), any());
        verify(resourceBasedCrnProvider).getResourceCrnListByResourceNameList(any());
        verify(resourceBasedCrnProvider, times(0)).getResourceCrnByResourceName(anyString());
    }

    @Test
    public void testCheckPermissionsOnNameRequestFieldWhenOtherExceptionOccurs() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(RESOURCE_NAME));
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);
        doThrow(new ForbiddenException("some error")).when(commonPermissionCheckingUtils)
                .checkPermissionForUserOnResource(any(), anyString());

        thrown.expect(ForbiddenException.class);
        thrown.expectMessage("some error");

        underTest.checkPermissions(getAnnotation(NAME, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null, 0L);

        verifyResourceCheckInteraction(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq(RESOURCE_NAME));
    }

    @Test
    public void testCheckPermissionsOnNameRequestFieldWhenAccessDeniedExceptionOccurs() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(RESOURCE_NAME));
        when(resourceBasedCrnProvider.getResourceCrnByResourceName(anyString())).thenReturn(RESOURCE_CRN);
        doThrow(new AccessDeniedException("get out!")).when(commonPermissionCheckingUtils)
                .checkPermissionForUserOnResource(any(), anyString());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("get out!");

        underTest.checkPermissions(getAnnotation(NAME, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null, 0L);

        verifyResourceCheckInteraction(eq(AuthorizationResourceAction.EDIT_CREDENTIAL), eq(USER_CRN), eq(RESOURCE_CRN));
        verify(resourceBasedCrnProvider).getResourceCrnByResourceName(eq(RESOURCE_NAME));
    }

    private void verifyResourceCheckInteraction(AuthorizationResourceAction eq, String eq2, String eq3) {
        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUser(any(), anyString());
        verify(commonPermissionCheckingUtils).checkPermissionForUserOnResource(eq(eq), eq(eq2), eq(eq3));
    }

    private void verifyZeroInteraction() {
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUserOnResources(any(), anyString(), any());
        verify(commonPermissionCheckingUtils, times(0)).checkPermissionForUserOnResource(any(), anyString(), any());
        verifyZeroInteractionRegardingName();
    }

    private void verifyZeroInteractionRegardingName() {
        verify(resourceBasedCrnProvider, times(0)).getResourceCrnByResourceName(anyString());
        verify(resourceBasedCrnProvider, times(0)).getResourceCrnListByResourceNameList(any());
    }

    private CheckPermissionByRequestProperty getAnnotation(AuthorizationVariableType type, AuthorizationResourceAction action,
            Boolean skipOnNull, String path) {
        return new CheckPermissionByRequestProperty() {

            @Override
            public AuthorizationVariableType type() {
                return type;
            }

            @Override
            public AuthorizationResourceAction action() {
                return action;
            }

            @Override
            public boolean skipOnNull() {
                return skipOnNull;
            }

            @Override
            public String path() {
                return path;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return CheckPermissionByRequestProperty.class;
            }
        };
    }

    public static class SampleRequestObject {
        private Object field;

        public SampleRequestObject() {
        }

        public SampleRequestObject(Object field) {
            this.field = field;
        }

        public Object getField() {
            return field;
        }
    }
}
