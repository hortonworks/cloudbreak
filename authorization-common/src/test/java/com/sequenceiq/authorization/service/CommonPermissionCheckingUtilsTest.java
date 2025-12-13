package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.ForbiddenException;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.defaults.CrnsByCategory;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;
import com.sequenceiq.authorization.utils.AuthorizationMessageUtilsService;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;

@ExtendWith(MockitoExtension.class)
public class CommonPermissionCheckingUtilsTest {

    private static final String USER_CRN = "USER_CRN";

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:1234:environment:1";

    private static final String DEFAULT_RESOURCE_CRN = "crn:cdp:datalake:us-west-1:1234:environment:2";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonPermissionCheckingUtilsTest.class);

    @Mock
    private UmsAccountAuthorizationService umsAccountAuthorizationService;

    @Mock
    private UmsResourceAuthorizationService umsResourceAuthorizationService;

    @Mock
    private UmsRightProvider umsRightProvider;

    @Spy
    private Map<AuthorizationResourceType, DefaultResourceChecker> defaultResourceCheckerMap = new EnumMap<>(AuthorizationResourceType.class);

    @Spy
    private Map<AuthorizationResourceType, ResourcePropertyProvider> resourceBasedCrnProviderMap = new EnumMap<>(AuthorizationResourceType.class);

    @InjectMocks
    private CommonPermissionCheckingUtils underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private DefaultResourceChecker defaultResourceChecker;

    @Mock
    private ResourcePropertyProvider resourceBasedCrnProvider;

    private AuthorizationMessageUtilsService authorizationMessageUtilsService;

    @Mock
    private ResourceNameFactoryService resourceNameFactoryService;

    @BeforeEach
    public void setUp() throws IllegalAccessException {
        defaultResourceCheckerMap.put(AuthorizationResourceType.IMAGE_CATALOG, defaultResourceChecker);
        resourceBasedCrnProviderMap.put(AuthorizationResourceType.IMAGE_CATALOG, resourceBasedCrnProvider);
        lenient().when(defaultResourceChecker.getResourceType()).thenReturn(AuthorizationResourceType.IMAGE_CATALOG);
        lenient().when(defaultResourceChecker.isDefault(RESOURCE_CRN)).thenReturn(false);
        lenient().when(defaultResourceChecker.isDefault(DEFAULT_RESOURCE_CRN)).thenReturn(true);
        lenient().when(defaultResourceChecker.isAllowedAction(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG)).thenReturn(true);
        lenient().when(defaultResourceChecker.getDefaultResourceCrns(any())).thenReturn(CrnsByCategory.newBuilder()
                .defaultResourceCrns(List.of(DEFAULT_RESOURCE_CRN))
                .notDefaultResourceCrns(List.of(RESOURCE_CRN))
                .build());
        lenient().when(umsRightProvider.getResourceType(any())).thenReturn(AuthorizationResourceType.IMAGE_CATALOG);
        lenient().when(umsRightProvider.getRight(eq(AuthorizationResourceAction.DELETE_IMAGE_CATALOG))).thenReturn("environments/deleteImageCatalog");
        lenient().when(methodSignature.getMethod()).thenReturn(getClass().getMethods()[0]);
        lenient().when(resourceNameFactoryService.getNames(any())).thenReturn(Map.of(RESOURCE_CRN, Optional.empty(), DEFAULT_RESOURCE_CRN, Optional.empty()));
        this.authorizationMessageUtilsService = spy(new AuthorizationMessageUtilsService(resourceNameFactoryService));
        FieldUtils.writeField(underTest, "authorizationMessageUtilsService", authorizationMessageUtilsService, true);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedThrowsUncheckedExceptionThenForbiddenExceptionComes() throws Throwable {
        //CHECKSTYLE:ON
        String exceptionMessage = "somethingHappened!!!";
        doThrow(new RuntimeException(exceptionMessage)).when(proceedingJoinPoint).proceed();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> underTest.proceed(proceedingJoinPoint, methodSignature, 0L));
        assertEquals(exception.getMessage(), exceptionMessage);
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedThrowsCheckedExceptionThenForbiddenExceptionComes() throws Throwable {
        //CHECKSTYLE:ON
        String exceptionMessage = "somethingHappened!!!";
        doThrow(new FileNotFoundException(exceptionMessage)).when(proceedingJoinPoint).proceed();

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            underTest.proceed(proceedingJoinPoint, methodSignature, 0L);
        });

        assertEquals(exceptionMessage, exception.getMessage());

        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    //CHECKSTYLE:OFF
    public void testProceedWhenProceedingJoinPointProceedReturnsNullThenItShouldHaveLoggedWithMethodSignatureCallAndNullReturnsAtTheEnd() throws Throwable {
        //CHECKSTYLE:ON
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        Object result = underTest.proceed(proceedingJoinPoint, methodSignature, 0L);

        assertNull(result);
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

        assertNotNull(result);
        assertEquals(expected, result);
        verify(methodSignature, times(0)).toLongString();
        verify(proceedingJoinPoint, times(1)).proceed();
    }

    @Test
    public void testGetParameterWithCorrectlyAnnotatedMethod() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithParamAnnotation", String.class, String.class));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"name", "other"});

        String parameter = underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceName.class, String.class);

        assertEquals("name", parameter);
    }

    @Test
    public void testGetParameterWithIncorrectlyAnnotatedMethod() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithParamAnnotation", String.class, String.class));

        IllegalStateException exception =
                assertThrows(IllegalStateException.class, () -> {
                    underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
                });
        assertEquals(exception.getMessage(), "Your controller method exampleMethodWithParamAnnotation should " +
                "have one and only one parameter with the annotation ResourceCrn");
    }

    @Test
    public void testGetParameterWithIncorrectlyParametrizedMethod() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithParamAnnotation", String.class, String.class));
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{0L, "other"});

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceName.class, String.class);
        });
        assertEquals("The type of the annotated parameter does not match with the expected type String", exception.getMessage());
    }

    @Test
    public void testGetParameterWithNukeMethod() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithoutParamAnnotation", String.class, String.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
        });
        assertEquals("Your controller method exampleMethodWithoutParamAnnotation should " +
                "have one and only one parameter with the annotation ResourceCrn", exception.getMessage());
    }

    @Test
    public void testGetParameterWithTooManyAnnotations() throws NoSuchMethodException {
        when(methodSignature.getMethod())
                .thenReturn(ExampleAuthorizationResourceClass.class.getMethod("exampleMethodWithTooManyParamAnnotation", String.class, String.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            underTest.getParameter(proceedingJoinPoint, methodSignature, ResourceCrn.class, String.class);
        });
        assertEquals("Your controller method exampleMethodWithTooManyParamAnnotation should " +
                "have one and only one parameter with the annotation ResourceCrn", exception.getMessage());
    }

    @Test
    public void testCheckPermissionForUserOnDefaultResource() {
        underTest.checkPermissionForUserOnResource(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, USER_CRN, DEFAULT_RESOURCE_CRN);

        verify(defaultResourceChecker).isDefault(DEFAULT_RESOURCE_CRN);
        verify(defaultResourceChecker).isAllowedAction(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG);
        verifyNoInteractions(umsResourceAuthorizationService);
    }

    @Test
    public void testCheckPermissionFailForUserOnDefaultResource() {
        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            underTest.checkPermissionForUserOnResource(AuthorizationResourceAction.DELETE_IMAGE_CATALOG, USER_CRN, DEFAULT_RESOURCE_CRN);
        });

        assertEquals("You have insufficient rights to perform the following action(s): " +
                String.format("'%s' on a(n) '%s' type resource with resource identifier: [Crn: '%s']",
                        AuthorizationResourceAction.DELETE_IMAGE_CATALOG.getRight(), "environment", DEFAULT_RESOURCE_CRN), exception.getMessage());

        verify(defaultResourceChecker).isDefault(DEFAULT_RESOURCE_CRN);
        verify(defaultResourceChecker).isAllowedAction(AuthorizationResourceAction.DELETE_IMAGE_CATALOG);
        verifyNoInteractions(umsResourceAuthorizationService);
    }

    @Test
    public void testCheckPermissionForUserOnNotDefaultResource() {
        underTest.checkPermissionForUserOnResource(AuthorizationResourceAction.DELETE_IMAGE_CATALOG, USER_CRN, RESOURCE_CRN);

        verify(defaultResourceChecker).isDefault(RESOURCE_CRN);
        verify(umsResourceAuthorizationService).checkRightOfUserOnResource(USER_CRN,
                AuthorizationResourceAction.DELETE_IMAGE_CATALOG, RESOURCE_CRN);
        verify(defaultResourceChecker, times(0)).isAllowedAction(any());
    }

    @Test
    public void testCheckPermissionForUserOnMixedResources() {
        List<String> resourceCrns = List.of(DEFAULT_RESOURCE_CRN, RESOURCE_CRN);

        underTest.checkPermissionForUserOnResources(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, USER_CRN, resourceCrns);

        verify(defaultResourceChecker).getDefaultResourceCrns(resourceCrns);
        verify(defaultResourceChecker).isAllowedAction(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG);
        verify(umsResourceAuthorizationService).checkRightOfUserOnResources(USER_CRN,
                AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, List.of(RESOURCE_CRN));
    }

    @Test
    public void testCheckPermissionFailForUserOnMixedResources() {
        List<String> resourceCrns = List.of(DEFAULT_RESOURCE_CRN, RESOURCE_CRN);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> {
            underTest.checkPermissionForUserOnResources(AuthorizationResourceAction.DELETE_IMAGE_CATALOG, USER_CRN, resourceCrns);
        });

        assertEquals("You have insufficient rights to perform the following action(s): " +
                String.format("'%s' on a(n) '%s' type resource with resource identifier: [Crn: '%s'],",
                        AuthorizationResourceAction.DELETE_IMAGE_CATALOG.getRight(), "environment", DEFAULT_RESOURCE_CRN) +
                String.format("'%s' on a(n) '%s' type resource with resource identifier: [Crn: '%s']",
                        AuthorizationResourceAction.DELETE_IMAGE_CATALOG.getRight(), "environment", RESOURCE_CRN), exception.getMessage());

        verify(defaultResourceChecker).getDefaultResourceCrns(resourceCrns);
        verify(defaultResourceChecker).isAllowedAction(AuthorizationResourceAction.DELETE_IMAGE_CATALOG);
        verifyNoInteractions(umsResourceAuthorizationService);
    }

    @Test
    public void testGetPermissionForUserOnMixedResources() {
        List<String> resourceCrns = List.of(DEFAULT_RESOURCE_CRN, RESOURCE_CRN);
        when(umsResourceAuthorizationService.getRightOfUserOnResources(anyString(), any(), any())).thenReturn(Map.of(RESOURCE_CRN, true));

        Map<String, Boolean> result = underTest.getPermissionsForUserOnResources(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, USER_CRN, resourceCrns);

        assertEquals(Map.of(DEFAULT_RESOURCE_CRN, true, RESOURCE_CRN, true), result);
        verify(defaultResourceChecker).getDefaultResourceCrns(resourceCrns);
        verify(defaultResourceChecker).isAllowedAction(AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG);
        verify(umsResourceAuthorizationService).getRightOfUserOnResources(USER_CRN,
                AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG, List.of(RESOURCE_CRN));
    }

    private static class ExampleAuthorizationResourceClass {

        @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
        public void exampleMethodWithParamAnnotation(@ResourceName String name, String other) {
            LOGGER.info(name + other);
        }

        @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
        public void exampleMethodWithoutParamAnnotation(String name, String other) {
            LOGGER.info(name + other);
        }

        @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
        public void exampleMethodWithTooManyParamAnnotation(@ResourceCrn String name, @ResourceCrn String other) {
            LOGGER.info(name + other);
        }
    }

    @DisableCheckPermissions
    private static class ExampleDisabledAuthorizationResourceClass {

    }

}