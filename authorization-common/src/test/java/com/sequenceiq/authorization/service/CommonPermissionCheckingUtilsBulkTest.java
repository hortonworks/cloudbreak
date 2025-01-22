package com.sequenceiq.authorization.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

import java.util.Map;

import jakarta.ws.rs.ForbiddenException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;

@ExtendWith(MockitoExtension.class)
public class CommonPermissionCheckingUtilsBulkTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    private static final String RESOURCE_CRN = "crn:cdp:credential:us-west-1:1234:credential:5678";

    private static final String OTHER_RESOURCE_CRN = "crn:cdp:credential:us-west-1:1234:credential:56789";

    @InjectMocks
    private CommonPermissionCheckingUtils underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private UmsAccountAuthorizationService umsAccountAuthorizationService;

    @Mock
    private UmsResourceAuthorizationService umsResourceAuthorizationService;

    @Mock
    private UmsRightProvider umsRightProvider;

    @Mock
    private Map<AuthorizationResourceType, DefaultResourceChecker> defaultResourceCheckerMap;

    @Mock
    private Map<AuthorizationResourceType, ResourcePropertyProvider> resourceBasedCrnProviderMap;

    @BeforeEach
    public void setUp() {
        lenient().when(umsRightProvider.getResourceType(any())).thenReturn(AuthorizationResourceType.IMAGE_CATALOG);
    }

    @ParameterizedTest(name = "AuthorizationResources: [{0}]")
    @EnumSource(AuthorizationResourceAction.class)
    public void testCheckPermissionWhenHasNoPermissionThenAccessDeniedExceptionComes(AuthorizationResourceAction action) {
        doThrow(ForbiddenException.class).when(umsAccountAuthorizationService).checkRightOfUser(any(), any());

        assertThrows(ForbiddenException.class, () -> {
            underTest.checkPermissionForUser(action, USER_CRN);
        });

    }

    @ParameterizedTest(name = "AuthorizationResources: [{0}]")
    @EnumSource(AuthorizationResourceAction.class)
    public void testCheckPermissionForUserWhenHasPermissionThenNoExceptionComes(AuthorizationResourceAction action) {
        doNothing().when(umsAccountAuthorizationService).checkRightOfUser(any(), any());

        underTest.checkPermissionForUser(action, USER_CRN);
    }

    @ParameterizedTest(name = "AuthorizationResources: [{0}]")
    @EnumSource(AuthorizationResourceAction.class)
    public void testCheckPermissionOnResourceWhenHasNoPermissionThenAccessDeniedExceptionComes(AuthorizationResourceAction action) {
        doThrow(ForbiddenException.class).when(umsResourceAuthorizationService).checkRightOfUserOnResource(any(), any(), anyString());

        assertThrows(ForbiddenException.class, () -> {
            underTest.checkPermissionForUserOnResource(action, USER_CRN, RESOURCE_CRN);
        });


    }

    @ParameterizedTest(name = "AuthorizationResources: [{0}]")
    @EnumSource(AuthorizationResourceAction.class)
    public void testCheckPermissionOnResourceForUserWhenHasPermissionThenNoExceptionComes(AuthorizationResourceAction action) {
        doNothing().when(umsResourceAuthorizationService).checkRightOfUserOnResource(any(), any(), anyString());

        underTest.checkPermissionForUserOnResource(action, USER_CRN, RESOURCE_CRN);
    }

    @ParameterizedTest(name = "AuthorizationResources: [{0}]")
    @EnumSource(AuthorizationResourceAction.class)
    public void testCheckPermissionOnResourcesWhenHasNoPermissionThenAccessDeniedExceptionComes(AuthorizationResourceAction action) {
        doThrow(ForbiddenException.class).when(umsResourceAuthorizationService).checkRightOfUserOnResources(any(), any(), any());

        assertThrows(ForbiddenException.class, () -> {
            underTest.checkPermissionForUserOnResources(action, USER_CRN, Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN, OTHER_RESOURCE_CRN));
        });
    }

    @ParameterizedTest(name = "AuthorizationResources: [{0}]")
    @EnumSource(AuthorizationResourceAction.class)
    public void testCheckPermissionOnResourcesForUserWhenHasPermissionThenNoExceptionComes(AuthorizationResourceAction action) {
        doNothing().when(umsResourceAuthorizationService).checkRightOfUserOnResources(any(), any(), any());

        underTest.checkPermissionForUserOnResources(action, USER_CRN, Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN, OTHER_RESOURCE_CRN));
    }

}
