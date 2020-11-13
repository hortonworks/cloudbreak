package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.ResourceCrnAwareApiModel;
import com.sequenceiq.authorization.service.list.ListPermissionChecker;
import com.sequenceiq.cloudbreak.auth.ReflectionUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalUserModifier;

@RunWith(MockitoJUnitRunner.class)
public class PermissionCheckServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String INTERNAL_ACTOR_CRN = "crn:cdp:iam:us-west-1:altus:user:__internal__actor__";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private ListPermissionChecker listPermissionChecker;

    @Mock
    private InternalUserModifier internalUserModifier;

    @Mock
    private CrnUserDetailsService crnUserDetailsService;

    @Mock
    private ReflectionUtil reflectionUtil;

    @Mock
    private AccountAuthorizationService accountAuthorizationService;

    @Mock
    private ResourceAuthorizationService resourceAuthorizationService;

    @InjectMocks
    private PermissionCheckService underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Before
    public void setUp() {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
    }

    @Test
    public void testDisabledAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("disabledAuthorization"));
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(null);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                listPermissionChecker,
                resourceAuthorizationService);
    }

    @Test
    public void testDisableAnnotationCancelsOtherAnnotations() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("tooManyAnnotation"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                listPermissionChecker,
                resourceAuthorizationService);
    }

    @Test
    public void testAccountAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("accountBasedMethod"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(accountAuthorizationService).authorize(any(CheckPermissionByAccount.class), eq(USER_CRN));
        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature), any());
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                listPermissionChecker);
    }

    @Test
    public void testAccountAndResourceBasedAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("accountAndResourceBasedMethod", String.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(accountAuthorizationService).authorize(any(CheckPermissionByAccount.class), eq(USER_CRN));
        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature), any());
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                listPermissionChecker);
    }

    @Test
    public void testResourceBasedAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("resourceBasedMethod", String.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature), any());
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                listPermissionChecker);
    }

    @Test
    public void testList() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("listMethod"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(listPermissionChecker).checkPermissions(any(FilterListBasedOnPermissions.class), eq(USER_CRN),
                eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verify(commonPermissionCheckingUtils, times(0)).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                resourceAuthorizationService);
    }

    @Test
    public void testListAndAccoundBasedAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("listAndAcccountBasedMethod"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(accountAuthorizationService).authorize(any(CheckPermissionByAccount.class), eq(USER_CRN));
        verify(listPermissionChecker).checkPermissions(any(FilterListBasedOnPermissions.class), eq(USER_CRN),
                eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verify(commonPermissionCheckingUtils, times(0)).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                resourceAuthorizationService);
    }

    @Test
    public void testInternalOnlyMethodIfNotInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethod"));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no access to this resource.");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));
    }

    @Test
    public void testInternalOnlyMethodIfInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethod"));

        ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                listPermissionChecker,
                resourceAuthorizationService);
    }

    @Test
    public void testInternalOnlyClassIfNotInternalActor() throws NoSuchMethodException {
        when(commonPermissionCheckingUtils.isInternalOnly(proceedingJoinPoint)).thenReturn(true);
        when(methodSignature.getMethod()).thenReturn(InternalOnlyClassExample.class.getMethod("get"));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no access to this resource.");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));
    }

    @Test
    public void testInternalOnlyClassIfInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(InternalOnlyClassExample.class.getMethod("get"));

        ThreadBasedUserCrnProvider.doAs(INTERNAL_ACTOR_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                listPermissionChecker,
                resourceAuthorizationService);
    }

    private static class ExampleClass {

        @DisableCheckPermissions
        public void disabledAuthorization() {

        }

        @DisableCheckPermissions
        @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        public void tooManyAnnotation() {

        }

        @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        public void accountBasedMethod() {

        }

        @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
        public void resourceBasedMethod(@ResourceCrn String crn) {

        }

        @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CREDENTIAL)
        public void accountAndResourceBasedMethod(@ResourceCrn String crn) {

        }

        @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        public List<ResourceCrnAwareApiModel> listMethod() {
            return List.of();
        }

        @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        public List<ResourceCrnAwareApiModel> listAndAcccountBasedMethod() {
            return List.of();
        }

        @InternalOnly
        public void internalOnlyMethod() {

        }
    }

    @InternalOnly
    public static class InternalOnlyClassExample {

        @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        public void get() {

        }
    }
}
