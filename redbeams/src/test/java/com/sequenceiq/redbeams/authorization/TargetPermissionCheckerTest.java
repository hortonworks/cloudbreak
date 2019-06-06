package com.sequenceiq.redbeams.authorization;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.AccessDeniedException;

public class TargetPermissionCheckerTest {

    private static final Object[] ARGS = { "foo", "bar", "baz" };

    private static final Object[] OPT_ARGS = { Optional.of("foo"), Optional.of("bar"), Optional.of("baz") };

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private TargetPermissionChecker underTest;

    @Mock
    private PermissionCheckingUtils permissionCheckingUtils;

    @Mock
    private CheckPermissionsByTarget annotation;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    // CHECKSTYLE:OFF
    public void testCheckPermissionsPass() throws Throwable {
    // CHECKSTYLE:ON
        when(proceedingJoinPoint.getArgs()).thenReturn(ARGS);
        when(annotation.action()).thenReturn(ResourceAction.READ);
        when(annotation.targetIndex()).thenReturn(1);
        when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");

        Object result = underTest.checkPermissions(annotation, "userCrn", proceedingJoinPoint, methodSignature);

        assertEquals("ok", result);
        verify(permissionCheckingUtils).checkPermissionsByTarget("bar", "userCrn", ResourceAction.READ);
        verify(permissionCheckingUtils).proceed(proceedingJoinPoint, methodSignature);
    }

    @Test
    // CHECKSTYLE:OFF
    public void testCheckPermissionsFail() throws Throwable {
    // CHECKSTYLE:ON
        thrown.expect(AccessDeniedException.class);
        when(proceedingJoinPoint.getArgs()).thenReturn(ARGS);
        when(annotation.action()).thenReturn(ResourceAction.READ);
        when(annotation.targetIndex()).thenReturn(1);
        when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");
        doThrow(new AccessDeniedException("nope")).when(permissionCheckingUtils).checkPermissionsByTarget("bar", "userCrn", ResourceAction.READ);

        try {
            underTest.checkPermissions(annotation, "userCrn", proceedingJoinPoint, methodSignature);
        } finally {
            verify(permissionCheckingUtils, never()).proceed(proceedingJoinPoint, methodSignature);
        }
    }

    @Test
    // CHECKSTYLE:OFF
    public void testCheckPermissionsIndexValidation() throws Throwable {
    // CHECKSTYLE:ON
        thrown.expect(IllegalArgumentException.class);
        when(proceedingJoinPoint.getArgs()).thenReturn(ARGS);
        when(annotation.action()).thenReturn(ResourceAction.READ);
        when(annotation.targetIndex()).thenReturn(ARGS.length + 1);
        doThrow(new IllegalArgumentException()).when(permissionCheckingUtils).validateIndex(ARGS.length + 1, ARGS.length, "targetIndex");

        try {
            underTest.checkPermissions(annotation, "userCrn", proceedingJoinPoint, methodSignature);
        } finally {
            verify(permissionCheckingUtils, never()).proceed(proceedingJoinPoint, methodSignature);
        }
    }

    @Test
    // CHECKSTYLE:OFF
    public void testCheckPermissionsPassOptional() throws Throwable {
    // CHECKSTYLE:ON
        when(proceedingJoinPoint.getArgs()).thenReturn(OPT_ARGS);
        when(annotation.action()).thenReturn(ResourceAction.READ);
        when(annotation.targetIndex()).thenReturn(1);
        when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");

        Object result = underTest.checkPermissions(annotation, "userCrn", proceedingJoinPoint, methodSignature);

        assertEquals("ok", result);
        verify(permissionCheckingUtils).checkPermissionsByTarget("bar", "userCrn", ResourceAction.READ);
        verify(permissionCheckingUtils).proceed(proceedingJoinPoint, methodSignature);
    }

    @Test
    // CHECKSTYLE:OFF
    public void testCheckPermissionsPassOptionalEmpty() throws Throwable {
    // CHECKSTYLE:ON
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[] { Optional.empty() });
        when(annotation.action()).thenReturn(ResourceAction.READ);
        when(annotation.targetIndex()).thenReturn(0);
        when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");

        Object result = underTest.checkPermissions(annotation, "userCrn", proceedingJoinPoint, methodSignature);

        assertEquals("ok", result);
        verify(permissionCheckingUtils).proceed(proceedingJoinPoint, methodSignature);
        verify(permissionCheckingUtils, never()).checkPermissionsByTarget(any(), eq("userCrn"), eq(ResourceAction.READ));
    }

}
