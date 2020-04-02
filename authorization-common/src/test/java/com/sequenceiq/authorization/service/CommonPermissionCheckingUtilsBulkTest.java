package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

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
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;

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

    private AuthorizationResourceAction action;

    private AuthorizationResourceType resource;

    public CommonPermissionCheckingUtilsBulkTest(AuthorizationResourceAction action, AuthorizationResourceType resource) {
        this.action = action;
        this.resource = resource;
    }

    @Parameters(name = "Current Action - AuthorizationResource pair: [{0} - {1}]")
    public static Object[][] data() {
        return combinationOf(AuthorizationResourceAction.values(), AuthorizationResourceType.values());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCheckPermissionWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsAccountAuthorizationService).checkRightOfUser(any(), any(), any());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionForUser(resource, action, USER_CRN);
    }

    @Test
    public void testCheckPermissionForUserWhenHasPermissionThenNoExceptionComes() {
        doNothing().when(umsAccountAuthorizationService).checkRightOfUser(any(), any(), any());

        underTest.checkPermissionForUser(resource, action, USER_CRN);
    }

    @Test
    public void testCheckPermissionOnResourceWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsResourceAuthorizationService).checkRightOfUserOnResource(any(), any(), any(), anyString());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionForUserOnResource(resource, action, USER_CRN, RESOURCE_CRN);
    }

    @Test
    public void testCheckPermissionOnResourceForUserWhenHasPermissionThenNoExceptionComes() {
        doNothing().when(umsResourceAuthorizationService).checkRightOfUserOnResource(any(), any(), any(), anyString());

        underTest.checkPermissionForUserOnResource(resource, action, USER_CRN, RESOURCE_CRN);
    }

    @Test
    public void testCheckPermissionOnResourcesWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsResourceAuthorizationService).checkRightOfUserOnResources(any(), any(), any(), any());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionForUserOnResources(resource, action, USER_CRN, Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN, OTHER_RESOURCE_CRN));
    }

    @Test
    public void testCheckPermissionOnResourcesForUserWhenHasPermissionThenNoExceptionComes() {
        doNothing().when(umsResourceAuthorizationService).checkRightOfUserOnResources(any(), any(), any(), any());

        underTest.checkPermissionForUserOnResources(resource, action, USER_CRN, Lists.newArrayList(RESOURCE_CRN, RESOURCE_CRN, OTHER_RESOURCE_CRN));
    }

    public static Object[][] combinationOf(Object[] first, Object[] second) {

        Object[][] testData = new Object[first.length * second.length][2];

        int index = 0;
        if (first.length > second.length) {
            for (Object elementOfSecond : second) {
                for (Object elementOfFirst : first) {
                    testData[index][0] = elementOfFirst;
                    testData[index][1] = elementOfSecond;
                    index++;
                }
            }
        } else {
            for (Object elementOfFirst : first) {
                for (Object elementOfSecond : second) {
                    testData[index][0] = elementOfFirst;
                    testData[index][1] = elementOfSecond;
                    index++;
                }
            }
        }

        return testData;
    }

}
