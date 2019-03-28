package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
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

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(Parameterized.class)
public class PermissionCheckingUtilsBulkTest {

    private static final Long WORKSPACE_ID = 1L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private PermissionCheckingUtils underTest;

    @Mock
    private User user;

    @Mock
    private WorkspaceAwareResource workspaceAwareResource;

    @Mock
    private Workspace workspace;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private UmsAuthorizationService umsAuthorizationService;

    @Mock
    private WorkspaceService workspaceService;

    private ResourceAction action;

    private WorkspaceResource resource;

    public PermissionCheckingUtilsBulkTest(ResourceAction action, WorkspaceResource resource) {
        this.action = action;
        this.resource = resource;
    }

    @Parameters(name = "Current Action - WorkspaceResource pair: [{0} - {1}]")
    public static Object[][] data() {
        return TestUtil.combinationOf(ResourceAction.values(), WorkspaceResource.values());
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(workspaceAwareResource.getWorkspace()).thenReturn(workspace);
        when(workspaceService.getByIdForCurrentUser(anyLong())).thenReturn(workspace);
    }

    @Test
    public void testCheckPermissionByWorkspaceIdForUserWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsAuthorizationService).checkRightOfUserForResource(any(), any(), any(), any());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, resource, action, user);

    }

    @Test
    public void testCheckPermissionByWorkspaceIdForUserWhenHasPermissionThenNoExceptionComes() {
        doNothing().when(umsAuthorizationService).checkRightOfUserForResource(any(), any(), any(), any());

        underTest.checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, resource, action, user);

    }

    @Test
    public void testCheckPermissionsByTargetWhenWorkspaceIdsAreEmptyThenNothingSpecialHappens() {
        underTest.checkPermissionsByTarget(new Object(), user, resource, action);

        verifyZeroInteractions(umsAuthorizationService);
    }

    @Test
    public void testCheckPermissionsByTargetWhenRoleCheckWentRightThenNoExceptionComes() {
        doReturn(true).when(umsAuthorizationService).hasRightOfUserForResource(any(), any(), any(), any());

        when(workspace.getId()).thenReturn(WORKSPACE_ID);

        underTest.checkPermissionsByTarget(workspaceAwareResource, user, resource, action);
    }

    @Test
    public void testCheckPermissionsByTargetWhenRightCheckFailsThenAccessDeniedExceptionComes() {
        doReturn(false).when(umsAuthorizationService).hasRightOfUserForResource(any(), any(), any(), any());

        when(workspace.getId()).thenReturn(WORKSPACE_ID);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to these workspaces: %s", action.name(), WORKSPACE_ID));

        underTest.checkPermissionsByTarget(workspaceAwareResource, user, resource, action);
    }

    @Test
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenWorkspaceIdIsNUllThenIllegalArgumentExceptionComes() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("workspaceId cannot be null!");

        underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, user, null, action, proceedingJoinPoint, methodSignature);
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

        underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenRightCheckFailsThenAccessDeniedExceptionComes() {
        doThrow(AccessDeniedException.class).when(umsAuthorizationService).checkRightOfUserForResource(any(), any(), any(), any());

        thrown.expect(AccessDeniedException.class);

        underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenProceedingJoinPointReturnsNullThenNullReturns() throws Throwable {
        //CHECKSTYLE:ON
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        Object result = underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);

        assertNull(result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCheckPermissionsByWorkspaceIdForUserAndProceedWhenProceedingJoinPointReturnsAnObjectThenThatObjectReturns() throws Throwable {
        //CHECKSTYLE:ON
        Object expected = new Object();
        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        Object result = underTest.checkPermissionsByWorkspaceIdForUserAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);

        assertNotNull(result);
        assertEquals(expected, result);
    }

}