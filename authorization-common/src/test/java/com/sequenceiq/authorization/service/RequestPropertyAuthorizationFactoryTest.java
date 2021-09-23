package com.sequenceiq.authorization.service;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DELETE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_CREDENTIAL;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationVariableType;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.authorization.service.model.HasRight;
import com.sequenceiq.authorization.service.model.HasRightOnAll;
import com.sequenceiq.authorization.utils.CrnAccountValidator;

@RunWith(MockitoJUnitRunner.class)
public class RequestPropertyAuthorizationFactoryTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

    private static final String RESOURCE_CRN = "crn:cdp:credential:us-west-1:1234:credential:5678";

    private static final String RESOURCE_NAME = "resource";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ResourceCrnAthorizationFactory resourceCrnAthorizationFactory;

    @Mock
    private ResourceNameAuthorizationFactory resourceNameAuthorizationFactory;

    @Mock
    private ResourceCrnListAuthorizationFactory resourceCrnListAuthorizationFactory;

    @Mock
    private ResourceNameListAuthorizationFactory resourceNameListAuthorizationFactory;

    @Mock
    private CrnAccountValidator crnAccountValidator;

    @InjectMocks
    private RequestPropertyAuthorizationFactory underTest;

    @AfterEach
    public void after() {
        verify(commonPermissionCheckingUtils).getParameter(any(), any(), eq(RequestObject.class), eq(Object.class));
    }

    @Test
    public void testOnCrn() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(RESOURCE_CRN));
        Optional<AuthorizationRule> expected = Optional.of(new HasRight(EDIT_CREDENTIAL, RESOURCE_CRN));
        when(resourceCrnAthorizationFactory.calcAuthorization(anyString(), any()))
                .thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(CRN, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null);

        verify(resourceCrnAthorizationFactory).calcAuthorization(RESOURCE_CRN, EDIT_CREDENTIAL);
        assertEquals(expected, authorization);
    }

    @Test
    public void testOnCrnList() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(List.of(RESOURCE_CRN)));
        Optional<AuthorizationRule> expected = Optional.of(new HasRightOnAll(EDIT_CREDENTIAL, List.of(RESOURCE_CRN)));
        when(resourceCrnListAuthorizationFactory.calcAuthorization(anyCollection(), any()))
                .thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null);

        verify(resourceCrnListAuthorizationFactory).calcAuthorization(List.of(RESOURCE_CRN), EDIT_CREDENTIAL);
        assertEquals(expected, authorization);
    }

    @Test
    public void testOnName() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(RESOURCE_NAME));
        Optional<AuthorizationRule> expected = Optional.of(new HasRight(DELETE_DATAHUB, RESOURCE_CRN));
        when(resourceNameAuthorizationFactory.calcAuthorization(anyString(), any()))
                .thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(NAME, DELETE_DATAHUB, false, "field"), USER_CRN, null, null);

        verify(resourceNameAuthorizationFactory).calcAuthorization(RESOURCE_NAME, DELETE_DATAHUB);
        assertEquals(expected, authorization);
    }

    @Test
    public void testOnNameList() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(List.of(RESOURCE_NAME)));
        Optional<AuthorizationRule> expected = Optional.of(new HasRightOnAll(EDIT_CREDENTIAL, List.of(RESOURCE_CRN)));
        when(resourceNameListAuthorizationFactory.calcAuthorization(anyCollection(), any()))
                .thenReturn(expected);

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(NAME_LIST, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null);

        verify(resourceNameListAuthorizationFactory).calcAuthorization(List.of(RESOURCE_NAME), EDIT_CREDENTIAL);
        assertEquals(expected, authorization);
    }

    @Test
    public void testOnNullWhenNullAllowed() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject());

        Optional<AuthorizationRule> authorization = underTest.getAuthorization(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, true, "field"), USER_CRN, null, null);

        assertEquals(Optional.empty(), authorization);
    }

    @Test
    public void testOnNullWhenRequired() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Property [field] of request object is null and it should be authorized, thus should be filled in.");

        underTest.getAuthorization(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null);
    }

    @Test
    public void testOnNestedNullWhenNullAllowed() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(new SampleRequestObject()));

        Optional<AuthorizationRule> authorization =
                underTest.getAuthorization(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, true, "field.field"), USER_CRN, null, null);

        assertEquals(Optional.empty(), authorization);
    }

    @Test
    public void testOnNestedNullWhenRequired() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Property [field.field] of request object is null and it should be authorized, thus should be filled in.");

        underTest.getAuthorization(getAnnotation(CRN_LIST, EDIT_CREDENTIAL, false, "field.field"), USER_CRN, null, null);
    }

    @Test
    public void testOnNonStringRequestField() {
        when(commonPermissionCheckingUtils.getParameter(any(), any(), any(), any())).thenReturn(new SampleRequestObject(Integer.valueOf(0)));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Referred property within request object is not string, thus access is denied!");

        underTest.getAuthorization(getAnnotation(CRN, EDIT_CREDENTIAL, false, "field"), USER_CRN, null, null);
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
