package com.sequenceiq.redbeams.authorization;

import static org.junit.Assert.assertEquals;
// import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

import java.util.ArrayList;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
// import org.junit.Rule;
import org.junit.Test;
// import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;

public class PermissionCheckerServiceTest {

    // @Rule
    // public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private PermissionCheckerService underTest;

    @Mock
    private PermissionCheckingUtils permissionCheckingUtils;

    @Mock
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Mock
    private TargetPermissionChecker permissionChecker;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication springAuthentication;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MethodSignature methodSignature;

    @Before
    public void setUp() {
        initMocks(this);

        underTest.permissionCheckers = new ArrayList<>();
        underTest.permissionCheckers.add(permissionChecker);
        underTest.testSecurityContext = securityContext;

        underTest.populatePermissionCheckerMap();

        when(proceedingJoinPoint.getSignature()).thenReturn(methodSignature);
    }

    // Need more test methods here. I'm having trouble mocking MethodSignature.

    // @Test
    // public void testNoMethodAnnotation() throws Throwable {
    //     when(securityContext.getAuthentication()).thenReturn(springAuthentication);
    //     when(methodSignature.getMethod().getAnnotation(any())).thenReturn(null);
    //     when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");

    //     Object result = underTest.hasPermission(proceedingJoinPoint);

    //     assertEquals("ok", result);
    //     verify(permissionCheckingUtils).proceed(proceedingJoinPoint, methodSignature);
    // }

    @Test
    // CHECKSTYLE:OFF
    public void testNoSpringAuthentication() throws Throwable {
    // CHECKSTYLE:ON
        when(securityContext.getAuthentication()).thenReturn(null);
        when(permissionCheckingUtils.proceed(proceedingJoinPoint, methodSignature)).thenReturn("ok");

        Object result = underTest.hasPermission(proceedingJoinPoint);

        assertEquals("ok", result);
        verify(permissionCheckingUtils).proceed(proceedingJoinPoint, methodSignature);
    }

}
