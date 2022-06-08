package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ReflectionUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalUserModifier;

@ExtendWith(MockitoExtension.class)
public class PermissionCheckServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String MODIFIED_INTERNAL_ACTOR = "crn:cdp:iam:us-west-1:cloudera:user:__internal__actor__";

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

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @InjectMocks
    private PermissionCheckService underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @BeforeEach
    public void setUp() {
        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(proceedingJoinPoint.getTarget()).thenReturn(new ExampleClass());
    }

    @Test
    public void testDisabledAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("disabledAuthorization"));
        when(commonPermissionCheckingUtils.proceed(any(), any(), anyLong())).thenReturn(null);

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
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

        assertThrows(NullPointerException.class, () ->
                ThreadBasedUserCrnProvider.doAs(null, () -> underTest.hasPermission(proceedingJoinPoint)));
    }

    @Test
    public void testAccountAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("accountBasedMethod"));

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(accountAuthorizationService).authorize(any(CheckPermissionByAccount.class), eq(USER_CRN));
        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature));
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier);
    }

    @Test
    public void testAccountAndResourceBasedAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("accountAndResourceBasedMethod", String.class));

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(accountAuthorizationService).authorize(any(CheckPermissionByAccount.class), eq(USER_CRN));
        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature));
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier);
    }

    @Test
    public void testResourceBasedAuthorization() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("resourceBasedMethod", String.class));

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(resourceAuthorizationService).authorize(eq(USER_CRN), eq(proceedingJoinPoint), eq(methodSignature));
        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService);
    }

    @Test
    public void testList() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("listMethod"));
        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService);
    }

    @Test
    public void testInternalOnlyMethodIfNotInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethod"));

        assertThrows(AccessDeniedException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint)),
                "This API is not publicly available and therefore not usable by end users. " +
                        "Please refer to our documentation about public APIs used by our UI and CLI.");
    }

    @Test
    public void testInternalOnlyMethodIfModifiedInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethod"));

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(MODIFIED_INTERNAL_ACTOR, () -> underTest.hasPermission(proceedingJoinPoint));

        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                resourceAuthorizationService);
    }

    @Test
    public void testInternalOnlyMethodIfOriginalInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethod"));

        assertThrows(AccessDeniedException.class, () ->
                ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:altus:user:__not_internal__actor__",
                        () -> underTest.hasPermission(proceedingJoinPoint)),
                "This API is not prepared to use it in service-to-service communication.");

        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                resourceAuthorizationService);
    }

    @Test
    public void testInternalOnlyMethodIfOriginalInternalActorButAccountIdNotNeeded() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethodAccountIdNotNeeded"));

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:altus:user:__internal__actor__", () -> underTest.hasPermission(proceedingJoinPoint));

        verifyNoInteractions(
                internalUserModifier,
                accountAuthorizationService,
                resourceAuthorizationService);
    }

    @Test
    public void testInternalOnlyMethodIfInitiatorUserCrn() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(ExampleClass.class.getMethod("internalOnlyMethod"));
        when(reflectionUtil.getParameter(eq(proceedingJoinPoint), eq(methodSignature), eq(InitiatorUserCrn.class)))
                .thenReturn(Optional.of(USER_CRN));

        ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:altus:user:__internal__actor__", () -> underTest.hasPermission(proceedingJoinPoint));

        verify(commonPermissionCheckingUtils).proceed(eq(proceedingJoinPoint), eq(methodSignature), anyLong());
        verify(internalUserModifier).persistModifiedInternalUser(any());
        verifyNoInteractions(
                accountAuthorizationService,
                resourceAuthorizationService);
    }

    @Test
    public void testInternalOnlyClassIfNotInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(InternalOnlyClassExample.class.getMethod("get"));
        when(proceedingJoinPoint.getTarget()).thenReturn(new InternalOnlyClassExample());

        assertThrows(AccessDeniedException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasPermission(proceedingJoinPoint)),
                "This API is not publicly available and therefore not usable by end users. " +
                        "Please refer to our documentation about public APIs used by our UI and CLI.");
    }

    @Test
    public void testInternalOnlyClassIfModifiedInternalActor() throws NoSuchMethodException {
        when(methodSignature.getMethod()).thenReturn(InternalOnlyClassExample.class.getMethod("get"));

        when(regionAwareInternalCrnGenerator.getInternalCrnForServiceAsString())
                .thenReturn("crn:altus:iam:us-west-1:altus:user:__internal__actor__");
        when(regionAwareInternalCrnGeneratorFactory.iam()).thenReturn(regionAwareInternalCrnGenerator);
        ThreadBasedUserCrnProvider.doAs(MODIFIED_INTERNAL_ACTOR, () -> underTest.hasPermission(proceedingJoinPoint));

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

        @FilterListBasedOnPermissions
        public List<String> listMethod() {
            return List.of();
        }

        @InternalOnly
        public void internalOnlyMethod() {

        }

        @InternalOnly
        @AccountIdNotNeeded
        public void internalOnlyMethodAccountIdNotNeeded() {

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
