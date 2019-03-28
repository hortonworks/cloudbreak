package com.sequenceiq.cloudbreak.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceConfigurationFilterTest {

    @Mock
    private CloudbreakRestRequestThreadLocalService cloudbreakRestRequestThreadLocalService;

    @Mock
    private WorkspaceService workspaceService;

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
        verifyZeroInteractions(userService, workspaceService);
        verify(cloudbreakRestRequestThreadLocalService, times(1)).removeRequestedWorkspaceId();
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    public void filterWhenCbUserWithCrnAndWorkspaceExist() throws ServletException, IOException {
        CloudbreakUser cbUser = createCbUserWithCrn();
        when(authenticatedUserService.getCbUser()).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.of(createWorkspace()));

        underTest.doFilterInternal(request, response, filterChain);

        verify(workspaceService, times(0)).create(any());
        verify(workspaceService, times(1)).getByName(eq(Crn.fromString(cbUser.getUserCrn()).getAccountId()), any());
        verify(workspaceService, times(0)).getByName(eq(cbUser.getTenant()), any());
    }

    @Test
    public void filterWhenCbUserWithoutCrnAndWorkspaceExist() throws ServletException, IOException {
        CloudbreakUser cbUser = createCbUserWithoutCrn();
        when(authenticatedUserService.getCbUser()).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.of(createWorkspace()));

        underTest.doFilterInternal(request, response, filterChain);

        verify(workspaceService, times(0)).create(any());
        verify(workspaceService, times(1)).getByName(eq(cbUser.getTenant()), any());
        assertNull(cbUser.getUserCrn());
    }

    @Test
    public void filterWhenWorkspaceDoesntExistAndCrnIsPresent() throws ServletException, IOException {
        CloudbreakUser cbUser = createCbUserWithCrn();
        when(authenticatedUserService.getCbUser()).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.empty());
        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        when(workspaceService.create(workspaceCaptor.capture())).thenReturn(createWorkspace());

        underTest.doFilterInternal(request, response, filterChain);

        assertEquals(Crn.fromString(cbUser.getUserCrn()).getAccountId(), workspaceCaptor.getValue().getName());
        verify(workspaceService, times(1)).create(any());
        verify(workspaceService, times(1)).getByName(eq(Crn.fromString(cbUser.getUserCrn()).getAccountId()), any());
        verify(workspaceService, times(0)).getByName(eq(cbUser.getTenant()), any());
    }

    @Test
    public void filterWhenWorkspaceDoesntExistAndCrnIsNotPresent() throws ServletException, IOException {
        CloudbreakUser cbUser = createCbUserWithoutCrn();
        when(authenticatedUserService.getCbUser()).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.empty());
        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        when(workspaceService.create(workspaceCaptor.capture())).thenReturn(createWorkspace());

        underTest.doFilterInternal(request, response, filterChain);

        assertEquals(cbUser.getTenant(), workspaceCaptor.getValue().getName());
        verify(workspaceService, times(1)).create(any());
        verify(workspaceService, times(1)).getByName(eq(cbUser.getTenant()), any());
    }

    private CloudbreakUser createCbUserWithCrn() {
        String userCrn = Crn.builder()
                .setAccountId("1")
                .setResource("1")
                .setResourceType(Crn.ResourceType.USER)
                .setService(Crn.Service.IAM)
                .build().toString();
        return new CloudbreakUser("userId", userCrn, "username", "email", "tenant");
    }

    private CloudbreakUser createCbUserWithoutCrn() {
        return new CloudbreakUser("userId", null, "username", "email", "tenant");
    }

    private Workspace createWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        return workspace;
    }
}
