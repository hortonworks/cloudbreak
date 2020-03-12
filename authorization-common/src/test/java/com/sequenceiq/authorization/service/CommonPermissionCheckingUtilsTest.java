package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.lang.annotation.Annotation;
import java.util.Optional;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;

@RunWith(MockitoJUnitRunner.class)
public class CommonPermissionCheckingUtilsTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonPermissionCheckingUtilsTest.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private CommonPermissionCheckingUtils underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedThrowsUncheckedExceptionThenAccessDeniedExceptionComes() throws Throwable {
        //CHECKSTYLE:ON
        String exceptionMessage = "somethingHappened!!!";
        doThrow(new RuntimeException(exceptionMessage)).when(proceedingJoinPoint).proceed();

        thrown.expect(RuntimeException.class);
        thrown.expectMessage(exceptionMessage);

        underTest.proceed(proceedingJoinPoint, methodSignature, 0L);
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

        underTest.proceed(proceedingJoinPoint, methodSignature, 0L);
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedReturnsNullThenItShouldHaveLoggedWithMethodSignatureCallAndNullReturnsAtTheEnd() throws Throwable {
        //CHECKSTYLE:ON
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        Object result = underTest.proceed(proceedingJoinPoint, methodSignature, 0L);

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

        Object result = underTest.proceed(proceedingJoinPoint, methodSignature, 0L);

        Assert.assertNotNull(result);
        Assert.assertEquals(expected, result);
        verify(methodSignature, times(0)).toLongString();
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    public void testGetAnnotation() {
        assertFalse(underTest.getClassAnnotation(String.class).isPresent());
        Optional<Annotation> classAnnotation = underTest.getClassAnnotation(ExampleAuthorizationResourceClass.class);
        assertTrue(classAnnotation.isPresent());
        assertTrue(classAnnotation.get().annotationType().equals(AuthorizationResource.class));
        assertEquals(AuthorizationResourceType.CREDENTIAL, ((AuthorizationResource) classAnnotation.get()).type());
    }

    @Test
    public void testGetAuthorizationClass() {
        when(proceedingJoinPoint.getTarget()).thenReturn("");
        assertFalse(underTest.getAuthorizationClass(proceedingJoinPoint).isPresent());

        when(proceedingJoinPoint.getTarget()).thenReturn(new ExampleAuthorizationResourceClass());
        Optional<Class<?>> authorizationClass = underTest.getAuthorizationClass(proceedingJoinPoint);
        assertTrue(authorizationClass.isPresent());
        assertEquals(ExampleAuthorizationResourceClass.class, authorizationClass.get());
    }

    @Test
    public void testCheckIfAuthorizationDisabled() {
        when(proceedingJoinPoint.getTarget()).thenReturn(new ExampleDisabledAuthorizationResourceClass());
        assertTrue(underTest.isAuthorizationDisabled(proceedingJoinPoint));

        when(proceedingJoinPoint.getTarget()).thenReturn(new ExampleAuthorizationResourceClass());
        assertFalse(underTest.isAuthorizationDisabled(proceedingJoinPoint));
    }

    @Test
    public void testGetParameterWithCorrectlyAnnotatedMethod() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithAnnotation", String.class, String.class));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"name", "other"});

        String parameter = underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceName.class, String.class);

        assertEquals("name", parameter);
    }

    @Test
    public void testGetParameterWithIncorrectlyAnnotatedMethod() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithAnnotation", String.class, String.class));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Your controller method exampleMethodWithAnnotation should have one and only one parameter with the annotation ResourceCrn");

        underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
    }

    @Test
    public void testGetParameterWithIncorrectlyParametrizedMethod() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithAnnotation", String.class, String.class));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{0L, "other"});

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("The type of the annotated parameter does not match with the expected type String");

        underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceName.class, String.class);
    }

    @Test
    public void testGetParameterWithNukeMethod() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithoutAnnotation", String.class, String.class));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Your controller method exampleMethodWithoutAnnotation should have one and only one parameter with the annotation ResourceCrn");

        underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
    }

    @Test
    public void testGetParameterWithTooManyAnnotations() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithTooManyAnnotation", String.class, String.class));

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Your controller method exampleMethodWithTooManyAnnotation should have one and only one parameter with the annotation ResourceCrn");

        underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
    }

    @AuthorizationResource(type = AuthorizationResourceType.CREDENTIAL)
    private static class ExampleAuthorizationResourceClass {

        public void exampleMethodWithAnnotation(@ResourceName String name, String other) {
            LOGGER.info(name + other);
        }

        public void exampleMethodWithoutAnnotation(String name, String other) {
            LOGGER.info(name + other);
        }

        public void exampleMethodWithTooManyAnnotation(@ResourceCrn String name, @ResourceCrn String other) {
            LOGGER.info(name + other);
        }
    }

    @DisableCheckPermissions
    private static class ExampleDisabledAuthorizationResourceClass {

    }

}