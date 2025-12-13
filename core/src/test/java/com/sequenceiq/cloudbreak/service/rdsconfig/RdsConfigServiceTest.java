package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.common.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.LegacyRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceStatus;

@ExtendWith(MockitoExtension.class)
class RdsConfigServiceTest {

    private static final String TEST_RDS_CONFIG_NAME = "RDSConfigTest";

    @InjectMocks
    private RdsConfigService underTest;

    @Mock
    private RdsConfigRepository rdsConfigRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private User user;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private UserService userService;

    @Mock
    private LegacyRestRequestThreadLocalService legacyRestRequestThreadLocalService;

    @Mock
    private Clock clock;

    @Captor
    private ArgumentCaptor<RDSConfig> rdsConfigCaptor;

    private Workspace defaultWorkspace;

    private RDSConfig testRdsConfig;

    @BeforeEach
    public void setUp() {
        defaultWorkspace = new Workspace();
        defaultWorkspace.setName("HortonWorkspace");
        defaultWorkspace.setId(1L);
        defaultWorkspace.setStatus(WorkspaceStatus.ACTIVE);
        defaultWorkspace.setDescription("This is a real Horton defaultWorkspace!");
        defaultWorkspace.setTenant(new Tenant());
        testRdsConfig = new RDSConfig();
        testRdsConfig.setId(1L);
        testRdsConfig.setName(TEST_RDS_CONFIG_NAME);
        lenient().when(userService.getOrCreate(any())).thenReturn(user);
        lenient().when(legacyRestRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        lenient().when(workspaceService.get(anyLong(), any())).thenReturn(defaultWorkspace);
    }

    @Test
    void testRetrieveRdsConfigsInDefaultWorkspace() {
        when(rdsConfigRepository.findAllByWorkspaceId(eq(1L))).thenReturn(Collections.singleton(testRdsConfig));

        Set<RDSConfig> rdsConfigs = underTest.retrieveRdsConfigsInWorkspace(defaultWorkspace);

        verify(rdsConfigRepository).findAllByWorkspaceId(eq(1L));
        assertEquals(1, rdsConfigs.size());
    }

    @Test
    void testGetExistingRdsConfigByNameAndDefaultWorkspace() {
        when(rdsConfigRepository.findByNameAndWorkspaceId(eq(TEST_RDS_CONFIG_NAME), eq(1L))).thenReturn(Optional.ofNullable(testRdsConfig));

        underTest.getByNameForWorkspace(TEST_RDS_CONFIG_NAME, defaultWorkspace);

        verify(rdsConfigRepository).findByNameAndWorkspaceId(anyString(), eq(1L));
    }

    @Test
    void testGetNonExistingRdsConfigByNameAndDefaultWorkspace() {
        when(rdsConfigRepository.findByNameAndWorkspaceId(anyString(), eq(1L))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> underTest.getByNameForWorkspace(TEST_RDS_CONFIG_NAME + "X", defaultWorkspace));
    }

    private void mockClusterServiceWithEmptyList() {
        when(clusterService.findByRdsConfig(anyLong())).thenReturn(Collections.emptySet());
    }

    private void mockClusterServiceWithSingletonList() {
        when(clusterService.findByRdsConfig(anyLong())).thenReturn(Collections.singleton(new Cluster()));
    }

    private void mockClusterServiceWithList() {
        Cluster cluster1 = new Cluster();
        cluster1.setName("ClusterWithRDS");
        Cluster cluster2 = new Cluster();
        cluster2.setName("ClusterWithRDS");
        when(clusterService.findByRdsConfig(anyLong())).thenReturn(Sets.newHashSet(cluster1, cluster2));
    }

    @Test
    void testNewRdsConfigCreation() throws TransactionExecutionException {
        when(rdsConfigRepository.findByNameAndWorkspaceId(eq(TEST_RDS_CONFIG_NAME), eq(1L))).thenReturn(Optional.empty());
        when(workspaceService.get(eq(1L), any(User.class))).thenReturn(defaultWorkspace);
        when(workspaceService.retrieveForUser(any())).thenReturn(Collections.singleton(defaultWorkspace));
        when(rdsConfigRepository.save(any())).thenReturn(testRdsConfig);

        RDSConfig rdsConfig = underTest.createIfNotExists(new User(), testRdsConfig, 1L);

        assertEquals(testRdsConfig, rdsConfig);
    }

    @Test
    void testExistingRdsConfigCreation() {
        when(rdsConfigRepository.findByNameAndWorkspaceId(eq(TEST_RDS_CONFIG_NAME), eq(1L))).thenReturn(Optional.ofNullable(testRdsConfig));

        RDSConfig rdsConfig = underTest.createIfNotExists(new User(), testRdsConfig, 1L);

        verify(workspaceService, never()).get(anyLong(), any(User.class));
        assertEquals(testRdsConfig, rdsConfig);
    }

    @Test
    void testDeleteDefaultRdsConfigs() {
        testRdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(2L);
        rdsConfig.setStatus(ResourceStatus.DEFAULT);

        underTest.deleteDefaultRdsConfigs(Sets.newHashSet(testRdsConfig, rdsConfig));

        verify(rdsConfigRepository).save(rdsConfig);
        verify(rdsConfigRepository, never()).delete(rdsConfig);
        assertEquals(ResourceStatus.USER_MANAGED, testRdsConfig.getStatus());
        assertEquals(ResourceStatus.DEFAULT, rdsConfig.getStatus());
        assertTrue(rdsConfig.isArchived());
    }
}
