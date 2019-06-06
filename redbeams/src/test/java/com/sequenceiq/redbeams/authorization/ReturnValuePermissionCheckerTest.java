package com.sequenceiq.redbeams.authorization;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.AccessDeniedException;

public class ReturnValuePermissionCheckerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private ReturnValuePermissionChecker underTest;

    @Mock
    private PermissionCheckingUtils permissionCheckingUtils;

    @Mock
    private CheckPermissionsByReturnValue annotation;

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
        when(annotation.action()).thenReturn(ResourceAction.READ);
        when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");

        Object result = underTest.checkPermissions(annotation, "userCrn", proceedingJoinPoint, methodSignature);

        assertEquals("ok", result);
        verify(permissionCheckingUtils).checkPermissionsByTarget("ok", "userCrn", ResourceAction.READ);
    }

    @Test
    // CHECKSTYLE:OFF
    public void testCheckPermissionsFail() throws Throwable {
    // CHECKSTYLE:ON
        thrown.expect(AccessDeniedException.class);
        when(annotation.action()).thenReturn(ResourceAction.READ);
        when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");
        doThrow(new AccessDeniedException("nope")).when(permissionCheckingUtils).checkPermissionsByTarget("ok", "userCrn", ResourceAction.READ);

        underTest.checkPermissions(annotation, "userCrn", proceedingJoinPoint, methodSignature);
    }

}
