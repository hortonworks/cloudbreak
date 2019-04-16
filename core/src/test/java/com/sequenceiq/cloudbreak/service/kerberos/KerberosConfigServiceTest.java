package com.sequenceiq.cloudbreak.service.kerberos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.KerberosConfigRepository;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public class KerberosConfigServiceTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private KerberosConfigRepository kerberosConfigRepository;

    @Mock
    private ClusterService clusterService;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private UserService userService;

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private KerberosConfigService underTest;

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void testCreateAlreadyExists() {
        KerberosConfig resource = new KerberosConfig();
        resource.setName("kerberos");
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        resource.setWorkspace(workspace);
        User user = new User();
        CloudbreakUser cloudbreakUser = new CloudbreakUser("1", "userCrn", "user", "user@user.com", "tenant");

        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(userService.getOrCreate(any())).thenReturn(user);
        when(workspaceService.retrieveForUser(eq(user))).thenReturn(Sets.newHashSet(workspace));
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(environmentViewService.findByNamesInWorkspace(any(), anyLong())).thenReturn(Sets.newHashSet());
        when(kerberosConfigRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(resource));

        thrown.expect(BadRequestException.class);

        underTest.createInEnvironment(resource, Sets.newHashSet(), 1L);
    }

    @Test
    public void testCreation() {
        KerberosConfig resource = new KerberosConfig();
        resource.setName("kerberos");
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        resource.setWorkspace(workspace);
        User user = new User();
        CloudbreakUser cloudbreakUser = new CloudbreakUser("1", "userCrn", "user", "user@user.com", "tenant");

        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(userService.getOrCreate(any())).thenReturn(user);
        when(workspaceService.retrieveForUser(eq(user))).thenReturn(Sets.newHashSet(workspace));
        when(workspaceService.get(anyLong(), any())).thenReturn(workspace);
        when(environmentViewService.findByNamesInWorkspace(any(), anyLong())).thenReturn(Sets.newHashSet());
        when(kerberosConfigRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.empty());
        when(kerberosConfigRepository.save(any())).thenReturn(resource);

        KerberosConfig kerberosConfig = underTest.createInEnvironment(resource, Sets.newHashSet(), 1L);
        assertEquals(kerberosConfig.getId(), resource.getId());
    }

    @Test
    public void testGetClusterUsingResource() {
        Cluster cluster = new Cluster();
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setId(1L);
        when(clusterService.findByKerberosConfig(eq(1L))).thenReturn(Sets.newHashSet(cluster));

        Set<Cluster> clustersUsingResource = underTest.getClustersUsingResource(kerberosConfig);

        assertTrue(clustersUsingResource.contains(cluster));
    }

    @Test
    public void testGetClusterUsingResourceInEnv() {
        Cluster cluster = new Cluster();
        KerberosConfig kerberosConfig = new KerberosConfig();
        kerberosConfig.setId(1L);
        when(clusterService.findAllClustersByKerberosConfigInEnvironment(eq(kerberosConfig), eq(1L))).thenReturn(Sets.newHashSet(cluster));

        Set<Cluster> clustersUsingResourceInEnvironment = underTest.getClustersUsingResourceInEnvironment(kerberosConfig, 1L);

        assertTrue(clustersUsingResourceInEnvironment.contains(cluster));
    }

    @Test
    public void testGetResourceType() {
        assertEquals(underTest.resource(), WorkspaceResource.KERBEROS_CONFIG);
    }

}
