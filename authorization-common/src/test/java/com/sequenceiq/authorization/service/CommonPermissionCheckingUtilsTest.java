package com.sequenceiq.authorization.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

public class CommonPermissionCheckingUtilsTest {

    private static final String INDEX_NAME = "someIndexNameValue";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private CommonPermissionCheckingUtils underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedThrowsUncheckedExceptionThenAccessDeniedExceptionComes() throws Throwable {
        //CHECKSTYLE:ON
        String exceptionMessage = "somethingHappened!!!";
        doThrow(new RuntimeException(exceptionMessage)).when(proceedingJoinPoint).proceed();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(exceptionMessage);

        underTest.proceed(proceedingJoinPoint, methodSignature);
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedThrowsCheckedExceptionThenAccessDeniedExceptionComes() throws Throwable {
        //CHECKSTYLE:ON
        String exceptionMessage = "somethingHappened!!!";
        doThrow(new FileNotFoundException(exceptionMessage)).when(proceedingJoinPoint).proceed();

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(exceptionMessage);

        underTest.proceed(proceedingJoinPoint, methodSignature);
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedReturnsNullThenItShouldHaveLoggedWithMethodSignatureCallAndNullReturnsAtTheEnd() throws Throwable {
        //CHECKSTYLE:ON
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        Object result = underTest.proceed(proceedingJoinPoint, methodSignature);

        Assert.assertNull(result);
        verify(methodSignature, times(1)).toLongString();
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedReturnsWithAnObjectThenThatObjectShouldReturnAtTheEnd() throws Throwable {
        //CHECKSTYLE:ON
        Object expected = new Object();
        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        Object result = underTest.proceed(proceedingJoinPoint, methodSignature);

        Assert.assertNotNull(result);
        Assert.assertEquals(expected, result);
        verify(methodSignature, times(0)).toLongString();
        verify(proceedingJoinPoint, times(1)).proceed();
    }

}