package com.sequenceiq.cloudbreak.filter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.CachedWorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class WorkspaceConfiguratorFilterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:5678";

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

    @BeforeEach
    public void before() throws IOException, ServletException {
        lenient().doNothing().when(filterChain).doFilter(any(), any());
        lenient().doNothing().when(cloudbreakRestRequestThreadLocalService).removeRequestedWorkspaceId();
        lenient().doNothing().when(cloudbreakRestRequestThreadLocalService).setRequestedWorkspaceId(any());
    }

    @Test
    public void filterWithoutCloudbreakUser() throws ServletException, IOException {
        when(authenticatedUserService.getCbUser(any(HttpServletRequest.class))).thenReturn(null);

        underTest.doFilterInternal(request, response, filterChain);

        verify(authenticatedUserService, times(1)).getCbUser(any(HttpServletRequest.class));
        verifyNoInteractions(userService, workspaceService);
        verify(cloudbreakRestRequestThreadLocalService, times(1)).removeRequestedWorkspaceId();
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    @Test
    public void filterWhenWorkspaceExists() throws ServletException, IOException {
        CloudbreakUser cbUser = createCbUserWithCrn();
        when(authenticatedUserService.getCbUser(any(HttpServletRequest.class))).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.of(createWorkspace()));

        underTest.doFilterInternal(request, response, filterChain);

        verify(workspaceService, times(1)).getByName(eq(Crn.fromString(cbUser.getUserCrn()).getAccountId()), any());
        verify(workspaceService, times(0)).getByName(eq(cbUser.getTenant()), any());
    }

    @Test
    public void filterWhenWorkspaceDoesNotExist() {
        CloudbreakUser cbUser = createCbUserWithCrn();
        when(authenticatedUserService.getCbUser(any(HttpServletRequest.class))).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> underTest.doFilterInternal(request, response, filterChain),
                "Tenant default workspace does not exist!");

        verify(workspaceService, times(1)).getByName(eq(Crn.fromString(cbUser.getUserCrn()).getAccountId()), any());
        verify(workspaceService, times(0)).getByName(eq(cbUser.getTenant()), any());
    }

    @Test
    public void testGettingHeaders() {
        CloudbreakUser cbUser = createCbUserWithCrn();
        when(authenticatedUserService.getCbUser(any(HttpServletRequest.class))).thenReturn(cbUser);
        when(userService.getOrCreate(any())).thenReturn(new User());
        when(workspaceService.getByName(anyString(), any())).thenReturn(Optional.of(createWorkspace()));
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Set.of("header1", "header2")));
        when(request.getHeaders("header1")).thenReturn(Collections.enumeration(Set.of("value1")));
        when(request.getHeaders("header2")).thenReturn(Collections.enumeration(Set.of("value2", "value3")));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            try {
                underTest.doFilterInternal(request, response, filterChain);
            } catch (Exception e) {

            }
        });

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
