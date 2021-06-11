package com.sequenceiq.cloudbreak.filter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.CachedWorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceConfigurationFilterTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private CloudbreakRestRequestThreadLocalService cloudbreakRestRequestThreadLocalService;

    @Mock
    private CachedWorkspaceService workspaceService;

    @Mock
    private UserService userService;

    @Mock
    private AuthenticatedUserService authenticatedUserService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private WorkspaceConfiguratorFilter underTest;

    @Before
    public void before() throws IOException, ServletException {
        doNothing().when(filterChain).doFilter(any(), any());
        doNothing().when(cloudbreakRestRequestThreadLocalService).removeRequestedWorkspaceId();
        doNothing().when(cloudbreakRestRequestThreadLocalService).setRequestedWorkspaceId(any());
    }

    @Test
    public void filterWithoutCloudbreakUser() throws ServletException, IOException {
        when(authenticatedUserService.getCbUser()).thenReturn(null);

        underTest.doFilterInternal(request, response, filterChain);

        verify(authenticatedUserService, times(1)).getCbUser();
        verifyNoInteractions(userService, workspaceService);
        verify(cloudbreakRestRequestThreadLocalService, times(1)).removeRequestedWorkspaceId();
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    public void filterWhenWorkspaceExists() throws ServletException, IOException {
        CloudbreakUser cbUser = createCbUserWithCrn();
        when(authenticatedUserService.getCbUser()).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.of(createWorkspace()));

        underTest.doFilterInternal(request, response, filterChain);

        verify(workspaceService, times(1)).getByName(eq(Crn.fromString(cbUser.getUserCrn()).getAccountId()), any());
        verify(workspaceService, times(0)).getByName(eq(cbUser.getTenant()), any());
    }

    @Test
    public void filterWhenWorkspaceDoesntExist() throws ServletException, IOException {
        CloudbreakUser cbUser = createCbUserWithCrn();
        when(authenticatedUserService.getCbUser()).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.empty());

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("Tenant default workspace does not exist!");

        underTest.doFilterInternal(request, response, filterChain);

        verify(workspaceService, times(1)).getByName(eq(Crn.fromString(cbUser.getUserCrn()).getAccountId()), any());
        verify(workspaceService, times(0)).getByName(eq(cbUser.getTenant()), any());
    }

    private CloudbreakUser createCbUserWithCrn() {
        String userCrn = CrnTestUtil.getUserCrnBuilder()
                .setAccountId("1")
                .setResource("1")
                .build().toString();
        return new CloudbreakUser("userId", userCrn, "username", "email", "tenant");
    }

    private Workspace createWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        return workspace;
    }
}
