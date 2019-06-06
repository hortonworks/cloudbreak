package com.sequenceiq.redbeams.authorization;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DisabledPermissionCheckerTest {

    @InjectMocks
    private DisabledPermissionChecker underTest;

    @Mock
    private PermissionCheckingUtils permissionCheckingUtils;

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
    public void testCheckPermissions() throws Throwable {
    // CHECKSTYLE:ON
        when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");

        Object result = underTest.checkPermissions(null, "userCrn", proceedingJoinPoint, methodSignature);

        assertEquals("ok", result);
    }

}
