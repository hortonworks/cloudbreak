package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
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
        lenient().when(proceedingJoinPoint.getTarget()).thenReturn(new ExampleClass());
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
                resourceAuthorizationService);
    }

    @Test
    public void testUserValidation() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("accountBasedMethod"));

        thrown.expect(NullPointerException.class);

        ThreadBasedUserCrnProvider.doAs(null, () -> underTest.hasPermission(proceedingJoinPoint));
    }

    @Test
    public void testAccountAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("accountBasedMethod"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(accountAuthorizationService).authorize(any(CheckPermissionByAccount.class), eq(USER_CRN));
        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature), any());
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier);
    }

    @Test
    public void testAccountAndResourceBasedAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("accountAndResourceBasedMethod", String.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(accountAuthorizationService).authorize(any(CheckPermissionByAccount.class), eq(USER_CRN));
        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature), any());
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier);
    }

    @Test
    public void testResourceBasedAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("resourceBasedMethod", String.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature), any());
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService);
    }

    @Test
    public void testList() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("listMethod"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        InOrder inOrder = Mockito.inOrder(resourceAuthorizationService, commonPermissionCheckingUtils);

        inOrder.verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature), any());
        inOrder.verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService);
    }

    @Test
    public void testListAndAccoundBasedAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("listAndAcccountBasedMethod"));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        InOrder inOrder = Mockito.inOrder(accountAuthorizationService, resourceAuthorizationService, commonPermissionCheckingUtils);
        inOrder.verify(accountAuthorizationService).authorize(any(CheckPermissionByAccount.class), eq(USER_CRN));
        inOrder.verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature), any());
        inOrder.verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(internalUserModifier);
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
                resourceAuthorizationService);
    }

    @Test
    public void testInternalOnlyClassIfNotInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(InternalOnlyClassExample.class.getMethod("get"));
        when(proceedingJoinPoint.getTarget()).thenReturn(new InternalOnlyClassExample());

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

        @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT,
                filter = ExampleFiltering.class)
        public List<String> listMethod() {
            return List.of();
        }

        @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT,
                filter = ExampleFiltering.class)
        public List<String> listAndAcccountBasedMethod() {
            return List.of();
        }

        @InternalOnly
        public void internalOnlyMethod() {

        }
    }

    static class ExampleFiltering extends AbstractAuthorizationFiltering<String> {

        @Override
        public List<ResourceWithId> getAllResources(Map<String, Object> args) {
            return List.of();
        }

        @Override
        public String filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
            return "NOPE";
        }

        @Override
        public String getAll(Map<String, Object> args) {
            return "NOPE";
        }
    }

    @InternalOnly
    public static class InternalOnlyClassExample {

        @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
        public void get() {

        }
    }
}
