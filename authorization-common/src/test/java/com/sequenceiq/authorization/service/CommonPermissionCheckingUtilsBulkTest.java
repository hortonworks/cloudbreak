package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@RunWith(Parameterized.class)
public class CommonPermissionCheckingUtilsBulkTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    private static final String RESOURCE_CRN = "crn:cdp:credential:us-west-1:1234:credential:5678";

    private static final String OTHER_RESOURCE_CRN = "crn:cdp:credential:us-west-1:1234:credential:56789";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
    private EntitlementService entitlementService;

    @Mock
    private Map<AuthorizationResourceType, DefaultResourceChecker> defaultResourceCheckerMap;

    @Mock
    private Map<AuthorizationResourceType, ResourcePropertyProvider> resourceBasedCrnProviderMap;

    private AuthorizationResourceAction action;

    public CommonPermissionCheckingUtilsBulkTest(AuthorizationResourceAction action) {
        this.action = action;
    }

    @Parameters(name = "AuthorizationResources: [{0}]")
    public static Object[] data() {
        return AuthorizationResourceAction.values();
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        umsRightProvider.init();
        when(umsRightProvider.getResourceType(any())).thenReturn(AuthorizationResourceType.IMAGE_CATALOG);
    }

    @Test
    public void testCheckPermissionWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsAccountAuthorizationService).checkRightOfUser(any(), any());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionForUser(action, USER_CRN);
    }

    @Test
    public void testCheckPermissionForUserWhenHasPermissionThenNoExceptionComes() {
        doNothing().when(umsAccountAuthorizationService).checkRightOfUser(any(), any());

        underTest.checkPermissionForUser(action, USER_CRN);
    }

    @Test
    public void testCheckPermissionOnResourceWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsResourceAuthorizationService).checkRightOfUserOnResource(any(), any(), anyString());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionForUserOnResource(action, USER_CRN, RESOURCE_CRN);
    }

    @Test
    public void testCheckPermissionOnResourceForUserWhenHasPermissionThenNoExceptionComes() {
        doNothing().when(umsResourceAuthorizationService).checkRightOfUserOnResource(any(), any(), anyString());

        underTest.checkPermissionForUserOnResource(action, USER_CRN, RESOURCE_CRN);
    }

    @Test
    public void testCheckPermissionOnResourcesWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsResourceAuthorizationService).checkRightOfUserOnResources(any(), any(), any());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionForUserOnResources(action, USER_CRN, Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN, OTHER_RESOURCE_CRN));
    }

    @Test
    public void testCheckPermissionOnResourcesForUserWhenHasPermissionThenNoExceptionComes() {
        doNothing().when(umsResourceAuthorizationService).checkRightOfUserOnResources(any(), any(), any());

        underTest.checkPermissionForUserOnResources(action, USER_CRN, Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN, OTHER_RESOURCE_CRN));
    }

}
