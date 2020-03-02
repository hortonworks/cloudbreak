package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@RunWith(MockitoJUnitRunner.class)
public class PermissionCheckServiceTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private PermissionChecker permissionChecker;

    @Spy
    private List<PermissionChecker> permissionCheckers = new ArrayList<PermissionChecker>();

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private PermissionCheckerService underTest;

    @Before
    public void setup() {
        permissionCheckers.add(permissionChecker);
        underTest.populatePermissionCheckMap();
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    public void testHasPermissionIfAnnotationIsNotPresent() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.empty());
        when(methodSignature.getMethod()).thenReturn(String.class.getMethod("length"));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no access to this resource.");

        underTest.hasPermission(proceedingJoinPoint);
    }

    @Test
    public void testHasPermissionIfAuthorizationDisabled() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.of(ExampleClass.class));
        when(commonPermissionCheckingUtils.getClassAnnotation(any())).thenReturn(Optional.of(new AuthorizationResource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return AuthorizationResource.class;
            }

            @Override
            public AuthorizationResourceType type() {
                return AuthorizationResourceType.CREDENTIAL;
            }
        }));
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("disabledAuthorization"));
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(null);

        underTest.hasPermission(proceedingJoinPoint);

        verify(permissionChecker, times(0)).checkPermissions(any(), any(), anyString(), any(), any(), anyLong());
    }

    @Test
    public void testHasPermissionIfThereAreTooManyAnnotationOnMethod() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.of(ExampleClass.class));
        when(commonPermissionCheckingUtils.getClassAnnotation(any())).thenReturn(Optional.of(new AuthorizationResource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return AuthorizationResource.class;
            }

            @Override
            public AuthorizationResourceType type() {
                return AuthorizationResourceType.CREDENTIAL;
            }
        }));
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("tooManyAnnotation"));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Only one of these annotations can be added to method");

        underTest.hasPermission(proceedingJoinPoint);

        verify(permissionChecker, times(0)).checkPermissions(any(), any(), anyString(), any(), any(), anyLong());
        verify(commonPermissionCheckingUtils, times(0)).proceed(any(), any(), anyLong());
    }

    @Test
    public void testHasPermissionIfThereIsNoAnnotationOnMethod() throws NoSuchMethodException {
        when(proceedingJoinPoint.getTarget()).thenReturn(new ExampleClass());
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.of(ExampleClass.class));
        doNothing().when(commonPermissionCheckingUtils).checkPermissionForUser(any(), any(), any());
        when(commonPermissionCheckingUtils.getClassAnnotation(any())).thenReturn(Optional.of(new AuthorizationResource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return AuthorizationResource.class;
            }

            @Override
            public AuthorizationResourceType type() {
                return AuthorizationResourceType.CREDENTIAL;
            }
        }));
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("withoutAnnotation"));

        underTest.hasPermission(proceedingJoinPoint);

        verify(permissionChecker, times(0)).checkPermissions(any(), any(), anyString(), any(), any(), anyLong());
        verify(commonPermissionCheckingUtils).checkPermissionForUser(any(), eq(AuthorizationResourceAction.WRITE), any());
        verify(commonPermissionCheckingUtils).proceed(any(), any(), anyLong());
    }

    @Test
    public void testHasPermission() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.getAuthorizationClass(proceedingJoinPoint)).thenReturn(Optional.of(ExampleClass.class));
        when(commonPermissionCheckingUtils.getClassAnnotation(any())).thenReturn(Optional.of(new AuthorizationResource() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return AuthorizationResource.class;
            }

            @Override
            public AuthorizationResourceType type() {
                return AuthorizationResourceType.CREDENTIAL;
            }
        }));
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("correctMethod"));
        when(permissionChecker.supportedAnnotation()).thenReturn(CheckPermissionByAccount.class);
        when(permissionChecker.checkPermissions(any(), any(), any(), any(), any(), anyLong())).thenReturn(null);

        underTest.populatePermissionCheckMap();
        underTest.hasPermission(proceedingJoinPoint);

        verify(permissionChecker).checkPermissions(any(), eq(AuthorizationResourceType.CREDENTIAL), any(), any(), any(), anyLong());
        verify(commonPermissionCheckingUtils, times(0)).proceed(any(), any(), anyLong());
    }

    private static class ExampleClass {

        @DisableCheckPermissions
        public void disabledAuthorization() {

        }

        @DisableCheckPermissions
        @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
        public void tooManyAnnotation() {

        }

        public void withoutAnnotation() {

        }

        @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
        public void correctMethod() {

        }
    }
}
