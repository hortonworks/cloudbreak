package com.sequenceiq.cloudbreak.auth;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.sql.SQLException;

import javax.validation.constraints.NotNull;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
public class ReflectionUtilTest {

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private ReflectionUtil underTest;

    @Test
    public void testParameterIfParamFilled() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("methodB", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"param"});

        assertTrue(underTest.getParameter(proceedingJoinPoint, methodSignature, NotNull.class).isPresent());
    }

    @Test
    public void testParameterIfParamNull() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("methodB", String.class);
        when(methodSignature.getMethod()).thenReturn(method);
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{null});

        assertFalse(underTest.getParameter(proceedingJoinPoint, methodSignature, NotNull.class).isPresent());
    }

    @Test
    public void testParameterIfParamNotExists() throws NoSuchMethodException {
        Method method = TestClass.class.getMethod("methodA");
        when(methodSignature.getMethod()).thenReturn(method);

        assertFalse(underTest.getParameter(proceedingJoinPoint, methodSignature, NotNull.class).isPresent());
    }

    @Test
    public void testProceedOk() {
        try {
            when(proceedingJoinPoint.proceed()).thenReturn("result");
            underTest.proceed(proceedingJoinPoint);
        } catch (Throwable throwable) {

        }
    }

    @Test
    public void testProceedNotRuntimeExceptionOccured() {
        try {
            when(proceedingJoinPoint.proceed()).thenThrow(new SQLException("wrong"));
            assertThrows(AccessDeniedException.class, () -> underTest.proceed(proceedingJoinPoint));
        } catch (Throwable throwable) {

        }
    }

    @Test
    public void testProceedRuntimeExceptionOccured() throws Exception {
        try {
            when(proceedingJoinPoint.proceed()).thenThrow(new NullPointerException("wrong"));
            assertThrows(NullPointerException.class, () -> underTest.proceed(proceedingJoinPoint));
        } catch (Throwable throwable) {

        }
    }

    private class TestClass {

        public void methodA() {

        }

        public void methodB(@NotNull String param) {

        }
    }

}
