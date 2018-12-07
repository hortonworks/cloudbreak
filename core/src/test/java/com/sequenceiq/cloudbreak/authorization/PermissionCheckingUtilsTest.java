package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

public class PermissionCheckingUtilsTest {

    private static final String INDEX_NAME = "someIndexNameValue";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private PermissionCheckingUtils underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testValidateIndexWhenIndexIsGreaterThanLengthThenIllegalArgumentExceptionComes() {
        int index = 2;
        int length = 1;
        String indexName = INDEX_NAME;

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(format("The %s [%s] cannot be bigger than or equal to the methods argument count [%s]", indexName, index, length));

        underTest.validateIndex(index, length, indexName);
    }

    @Test
    public void testValidateIndexWhenIndexIsEqualsWithLengthThenIllegalArgumentExceptionComes() {
        int index = 2;
        int length = 2;
        String indexName = INDEX_NAME;

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(format("The %s [%s] cannot be bigger than or equal to the methods argument count [%s]", indexName, index, length));

        underTest.validateIndex(index, length, indexName);
    }

    @Test
    public void testValidateIndexWhenIndexIsLessThanLengthThenIllegalArgumentExceptionComes() {
        int index = 2;
        int length = 3;

        underTest.validateIndex(index, length, INDEX_NAME);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedThrowsExceptionThenAccessDeniedExceptionComes() throws Throwable {
        //CHECKSTYLE:ON
        String exceptionMessage = "somethingHappened!!!";
        doThrow(new RuntimeException(exceptionMessage)).when(proceedingJoinPoint).proceed();

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