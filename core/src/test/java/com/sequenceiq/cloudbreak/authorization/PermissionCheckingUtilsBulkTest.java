package com.sequenceiq.cloudbreak.authorization;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

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

import com.sequenceiq.cloudbreak.authorization.WorkspacePermissions.Action;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.UserWorkspacePermissions;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;
import com.sequenceiq.cloudbreak.service.user.UserWorkspacePermissionsService;

@RunWith(Parameterized.class)
public class PermissionCheckingUtilsBulkTest {

    private static final Long WORKSPACE_ID = 1L;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private UserWorkspacePermissionsService userWorkspacePermissionsService;

    @Mock
    private WorkspacePermissionUtil workspacePermissionUtil;

    @InjectMocks
    private PermissionCheckingUtils underTest;

    @Mock
    private UserWorkspacePermissions userWorkspacePermissions;

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

    private Action action;

    private WorkspaceResource resource;

    public PermissionCheckingUtilsBulkTest(Action action, WorkspaceResource resource) {
        this.action = action;
        this.resource = resource;
    }

    @Parameters(name = "Current Action - WorkspaceResource pair: [{0} - {1}]")
    public static Object[][] data() {
        Action[] actions = Action.values();
        WorkspaceResource[] resources = WorkspaceResource.values();

        Object[][] testData = new Object[actions.length * resources.length][2];

        int index = 0;
        if (actions.length < resources.length) {
            for (Action action : actions) {
                for (WorkspaceResource resource : resources) {
                    testData[index][0] = action;
                    testData[index][1] = resource;
                    index++;
                }
            }
        } else {
            for (WorkspaceResource resource : resources) {
                for (Action action : actions) {
                    testData[index][0] = resource;
                    testData[index][1] = action;
                    index++;
                }
            }
        }
        return testData;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(workspaceAwareResource.getWorkspace()).thenReturn(workspace);
    }

    @Test
    public void testCheckPermissionByWorkspaceIdForUserWhenUserWorkspacePermissionsIsNullThenAccessDeniedExceptionComes() {
        when(userWorkspacePermissionsService.findForUserByWorkspaceId(WORKSPACE_ID, user)).thenReturn(null);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to %s.", action.name(), resource));

        underTest.checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, resource, action, user);

        verify(userWorkspacePermissionsService, times(1)).findForUserByWorkspaceId(anyLong(), any(User.class));
        verify(userWorkspacePermissionsService, times(1)).findForUserByWorkspaceId(WORKSPACE_ID, user);
        verify(workspacePermissionUtil, times(0)).hasPermission(anySet(), any(WorkspaceResource.class), any(Action.class));
    }

    @Test
    public void testCheckPermissionByWorkspaceIdForUserWhenHasNoPermissionThenAccessDeniedExceptionComes() {
        Set<String> permissionSet = Collections.emptySet();
        when(userWorkspacePermissionsService.findForUserByWorkspaceId(WORKSPACE_ID, user)).thenReturn(userWorkspacePermissions);
        when(userWorkspacePermissions.getPermissionSet()).thenReturn(permissionSet);
        when(workspacePermissionUtil.hasPermission(permissionSet, resource, action)).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to %s.", action.name(), resource));

        underTest.checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, resource, action, user);

        verify(userWorkspacePermissionsService, times(1)).findForUserByWorkspaceId(anyLong(), any(User.class));
        verify(userWorkspacePermissionsService, times(1)).findForUserByWorkspaceId(WORKSPACE_ID, user);
        verify(workspacePermissionUtil, times(1)).hasPermission(anySet(), any(WorkspaceResource.class), any(Action.class));
        verify(workspacePermissionUtil, times(1)).hasPermission(anySet(), eq(resource), eq(action));
    }

    @Test
    public void testCheckPermissionByWorkspaceIdForUserWhenHasPermissionThenNoExceptionComes() {
        Set<String> permissionSet = Collections.emptySet();
        when(userWorkspacePermissionsService.findForUserByWorkspaceId(WORKSPACE_ID, user)).thenReturn(userWorkspacePermissions);
        when(userWorkspacePermissions.getPermissionSet()).thenReturn(permissionSet);
        when(workspacePermissionUtil.hasPermission(permissionSet, resource, action)).thenReturn(true);

        underTest.checkPermissionByWorkspaceIdForUser(WORKSPACE_ID, resource, action, user);

        verify(userWorkspacePermissionsService, times(1)).findForUserByWorkspaceId(anyLong(), any(User.class));
        verify(userWorkspacePermissionsService, times(1)).findForUserByWorkspaceId(WORKSPACE_ID, user);
        verify(workspacePermissionUtil, times(1)).hasPermission(anySet(), any(WorkspaceResource.class), any(Action.class));
        verify(workspacePermissionUtil, times(1)).hasPermission(anySet(), eq(resource), eq(action));
    }

    @Test
    public void testCheckPermissionsByTargetWhenWorkspaceIdsAreEmptyThenNothingSpecialHappens() {
        Object o = new Object();

        underTest.checkPermissionsByTarget(o, user, resource, action);

        verify(userWorkspacePermissionsService, times(0)).findForUserByWorkspaceIds(eq(user), anySet());
    }

    @Test
    public void testCheckPermissionsByTargetWhenWorkspaceIdsAreNotEmptyButItsSizeNotEqualsToTheUserWorkspacePermissionsThenAccessDeniedExceptionComes() {
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(userWorkspacePermissionsService.findForUserByWorkspaceIds(eq(user), anySet())).thenReturn(Collections.emptySet());

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to %s.", action.name(), resource.getReadableName()));

        underTest.checkPermissionsByTarget(workspaceAwareResource, user, resource, action);

        verify(userWorkspacePermissionsService, times(1)).findForUserByWorkspaceIds(eq(user), anySet());
    }

    @Test
    public void testCheckPermissionsByTargetWhenUserWorkspacePermissionsSetIsEmptyAndWorkspacePermissionUtilTellsItIsFineThenNoExceptionComes() {
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(userWorkspacePermissions.getPermissionSet()).thenReturn(Collections.emptySet());
        when(userWorkspacePermissionsService.findForUserByWorkspaceIds(eq(user), anySet())).thenReturn(Set.of(userWorkspacePermissions));
        // returning true, to avoid AccessDeniedException from the validation of this value later on
        when(workspacePermissionUtil.hasPermission(anySet(), eq(resource), eq(action))).thenReturn(true);

        underTest.checkPermissionsByTarget(workspaceAwareResource, user, resource, action);
    }

    @Test
    public void testCheckPermissionsByTargetWhenUserWorkspacePermissionsSetIsEmptyAndWorkspacePermissionUtilThinksItIsNotFineThenAccessDeniedExceptionComes() {
        when(workspace.getId()).thenReturn(WORKSPACE_ID);
        when(userWorkspacePermissions.getPermissionSet()).thenReturn(Collections.emptySet());
        when(userWorkspacePermissionsService.findForUserByWorkspaceIds(eq(user), anySet())).thenReturn(Set.of(userWorkspacePermissions));
        when(workspacePermissionUtil.hasPermission(anySet(), eq(resource), eq(action))).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to %s.", action.name(), resource.getReadableName()));

        underTest.checkPermissionsByTarget(workspaceAwareResource, user, resource, action);
    }

    @Test
    public void testCheckPermissionsByPermissionSetAndProceedWhenWorkspaceIdIsNUllThenIllegalArgumentExceptionComes() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("workspaceId cannot be null!");

        underTest.checkPermissionsByPermissionSetAndProceed(resource, user, null, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    public void testCheckPermissionsByPermissionSetAndProceedWhenUserWorkspacePermissionsIsNUllThenAccessDeniedExceptionComes() {
        when(userWorkspacePermissionsService.findForUserByWorkspaceId(user, WORKSPACE_ID)).thenReturn(null);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to %s.", action.name(), resource.getReadableName()));

        underTest.checkPermissionsByPermissionSetAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCheckPermissionsByPermissionSetAndProceedWhenProceedingJoinPointProceedOperationThrowsExceptionThenAccessDeniedExceptionComes()
            throws Throwable {
        //CHECKSTYLE:ON
        when(userWorkspacePermissionsService.findForUserByWorkspaceId(user, WORKSPACE_ID)).thenReturn(userWorkspacePermissions);
        when(userWorkspacePermissions.getPermissionSet()).thenReturn(Set.of("somePermission"));
        when(workspacePermissionUtil.hasPermission(anySet(), eq(resource), eq(action))).thenReturn(true);
        String someMessage = "hereComesTheSanta";
        doThrow(new RuntimeException(someMessage)).when(proceedingJoinPoint).proceed();

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(someMessage);

        underTest.checkPermissionsByPermissionSetAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    public void testCheckPermissionsByPermissionSetAndProceedWhenWorkspacePermissionUtilTellsItHasNoPermissionThenAccessDeniedExceptionComes() {
        when(userWorkspacePermissionsService.findForUserByWorkspaceId(user, WORKSPACE_ID)).thenReturn(userWorkspacePermissions);
        when(userWorkspacePermissions.getPermissionSet()).thenReturn(Set.of("somePermission"));
        when(workspacePermissionUtil.hasPermission(anySet(), eq(resource), eq(action))).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to %s.", action.name(), resource.getReadableName()));

        underTest.checkPermissionsByPermissionSetAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCheckPermissionsByPermissionSetAndProceedWhenProceedingJoinPointProceedReturnsNullThenNullReturns() throws Throwable {
        //CHECKSTYLE:ON
        when(userWorkspacePermissionsService.findForUserByWorkspaceId(user, WORKSPACE_ID)).thenReturn(userWorkspacePermissions);
        when(userWorkspacePermissions.getPermissionSet()).thenReturn(Set.of("somePermission"));
        when(workspacePermissionUtil.hasPermission(anySet(), eq(resource), eq(action))).thenReturn(true);
        when(proceedingJoinPoint.proceed()).thenReturn(null);

        Object result = underTest.checkPermissionsByPermissionSetAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);

        assertNull(result);
    }

    @Test
    //CHECKSTYLE:OFF
    public void testCheckPermissionsByPermissionSetAndProceedWhenProceedingJoinPointProceedReturnsAnObjectThenThatObjectReturnsAtTheEnd() throws Throwable {
        //CHECKSTYLE:ON
        when(userWorkspacePermissionsService.findForUserByWorkspaceId(user, WORKSPACE_ID)).thenReturn(userWorkspacePermissions);
        when(userWorkspacePermissions.getPermissionSet()).thenReturn(Set.of("somePermission"));
        when(workspacePermissionUtil.hasPermission(anySet(), eq(resource), eq(action))).thenReturn(true);
        Object expected = new Object();
        when(proceedingJoinPoint.proceed()).thenReturn(expected);

        Object result = underTest.checkPermissionsByPermissionSetAndProceed(resource, user, WORKSPACE_ID, action, proceedingJoinPoint, methodSignature);

        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    public void testCheckPermissionByPermissionSetWhenWorkspacePermissionUtilTellsHasNoPermissionThenAccessDeniedExceptionComesRegardlessOfTheGivenTypes() {
        when(workspacePermissionUtil.hasPermission(anySet(), eq(resource), eq(action))).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(format("You have no [%s] permission to %s.", action.name(), resource.getReadableName()));

        underTest.checkPermissionByPermissionSet(action, resource, Collections.emptySet());

        verify(workspacePermissionUtil, times(1)).hasPermission(anySet(), eq(resource), eq(action));
    }

    @Test
    public void testCheckPermissionByPermissionSetWhenWorkspacePermissionUtilTellsHasPermissionThenNoExceptionComes() {
        when(workspacePermissionUtil.hasPermission(anySet(), eq(resource), eq(action))).thenReturn(true);

        underTest.checkPermissionByPermissionSet(action, resource, Collections.emptySet());

        verify(workspacePermissionUtil, times(1)).hasPermission(anySet(), eq(resource), eq(action));
    }

}