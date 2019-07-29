package com.sequenceiq.cloudbreak.workspace.authorization;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;

@RunWith(Parameterized.class)
public class PermissionCheckingUtilsBulkTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String WORKSPACE_CRN = "crn:altus:iam:us-west-1:1234:workspace:" + WORKSPACE_ID;

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:1234:user:" + USER_ID;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private PermissionCheckingUtils underTest;

    @Mock
    private WorkspaceAwareResource workspaceAwareResource;

    @Mock
    private Workspace workspace;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private UmsWorkspaceAuthorizationService umsWorkspaceAuthorizationService;

    private ResourceAction action;

    private AuthorizationResource resource;

    public PermissionCheckingUtilsBulkTest(ResourceAction action, AuthorizationResource resource) {
        this.action = action;
        this.resource = resource;
    }

    @Parameters(name = "Current Action - AuthorizationResource pair: [{0} - {1}]")
    public static Object[][] data() {
        return combinationOf(ResourceAction.values(), AuthorizationResource.values());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(workspaceAwareResource.getWorkspace()).thenReturn(workspace);
        when(workspace.getResourceCrn()).thenReturn("crn:altus:iam:us-west-1:1234:workspace:1");
    }

    @Test
    public void testCheckPermissionsByTargetWhenWorkspaceIdsAreEmptyThenNothingSpecialHappens() {
        underTest.checkPermissionsByTarget(new Object(), USER_CRN, resource, action);

        verifyZeroInteractions(umsWorkspaceAuthorizationService);
    }

    @Test
    public void testCheckPermissionsByTargetWhenRoleCheckWentRightThenNoExceptionComes() {
        doReturn(true).when(umsWorkspaceAuthorizationService).hasRightOfUserForResource(anyString(),
                any(AuthorizationResource.class), any(ResourceAction.class));

        underTest.checkPermissionsByTarget(workspaceAwareResource, USER_CRN, resource, action);
    }

    @Test
    public void testCheckPermissionsByTargetWhenRightCheckFailsThenAccessDeniedExceptionComes() {
        doReturn(false).when(umsWorkspaceAuthorizationService).hasRightOfUserForResource(anyString(),
                any(AuthorizationResource.class), any(ResourceAction.class));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to these workspaces: %s", action.name(), 0));

        underTest.checkPermissionsByTarget(workspaceAwareResource, USER_CRN, resource, action);
    }

    @Test
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenWorkspaceIdIsNUllThenIllegalArgumentExceptionComes() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("workspaceId cannot be null!");

        underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, USER_CRN, null, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenProceedingJoinPointProceedOperationThrowsExceptionThenAccessDeniedExceptionComes()
            throws Throwable {
        //CHECKSTYLE:ON
        String someMessage = "hereComesTheSanta";
        doThrow(new RuntimeException(someMessage)).when(proceedingJoinPoint).proceed();

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(someMessage);

        underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, USER_CRN, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenRightCheckFailsThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsWorkspaceAuthorizationService).checkRightOfUserForResource(any(), any(), any());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, USER_CRN, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenProceedingJoinPointReturnsNullThenNullReturns() throws Throwable {
        //CHECKSTYLE:ON
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        Object result = underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, USER_CRN, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);

        assertNull(result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenProceedingJoinPointReturnsAnObjectThenThatObjectReturns() throws Throwable {
        //CHECKSTYLE:ON
        Object expected = new Object();
        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        Object result = underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, USER_CRN, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);

        assertNotNull(result);
        assertEquals(expected, result);
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