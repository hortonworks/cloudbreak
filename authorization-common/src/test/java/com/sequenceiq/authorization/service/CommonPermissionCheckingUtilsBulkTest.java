package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
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

import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;

@RunWith(Parameterized.class)
public class CommonPermissionCheckingUtilsBulkTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private CommonPermissionCheckingUtils underTest;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private UmsAuthorizationService umsAuthorizationService;

    private ResourceAction action;

    private ResourceType resource;

    public CommonPermissionCheckingUtilsBulkTest(ResourceAction action, ResourceType resource) {
        this.action = action;
        this.resource = resource;
    }

    @Parameters(name = "Current Action - AuthorizationResource pair: [{0} - {1}]")
    public static Object[][] data() {
        return combinationOf(ResourceAction.values(), ResourceType.values());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCheckPermissionByWorkspaceIdForUserWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsAuthorizationService).checkRightOfUserForResource(any(), any(), any());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionForUser(resource, action, USER_CRN);

    }

    @Test
    public void testCheckPermissionByWorkspaceIdForUserWhenHasPermissionThenNoExceptionComes() {
        doNothing().when(umsAuthorizationService).checkRightOfUserForResource(any(), any(), any(), any());

        underTest.checkPermissionForUser(resource, action, USER_CRN);

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
