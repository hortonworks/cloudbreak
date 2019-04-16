package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceStatus;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.rds.RdsConnectionValidator;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.Tenant;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.RdsConfigRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

public class RdsConfigServiceTest {

    private static final String TEST_RDS_CONFIG_NAME = "RDSConfigTest";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private RdsConfigService underTest;

    @Mock
    private RdsConfigRepository rdsConfigRepository;

    @Mock
    private RdsConnectionValidator rdsConnectionValidator;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private TransactionService transactionService;

    @Mock
    private User user;

    @Mock
    private CloudbreakUser cloudbreakUser;

    @Mock
    private UserService userService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private Clock clock;

    @Captor
    private ArgumentCaptor<RDSConfig> rdsConfigCaptor;

    private Workspace defaultWorkspace;

    private RDSConfig testRdsConfig;

    @Before
    public void setUp() {
        initMocks(this);
        defaultWorkspace = new Workspace();
        defaultWorkspace.setName("HortonWorkspace");
        defaultWorkspace.setId(1L);
        defaultWorkspace.setStatus(WorkspaceStatus.ACTIVE);
        defaultWorkspace.setDescription("This is a real Horton defaultWorkspace!");
        defaultWorkspace.setTenant(new Tenant());
        testRdsConfig = new RDSConfig();
        testRdsConfig.setId(1L);
        testRdsConfig.setName(TEST_RDS_CONFIG_NAME);
        doNothing().when(rdsConfigRepository).delete(any());
        when(userService.getOrCreate(any())).thenReturn(user);
        when(restRequestThreadLocalService.getCloudbreakUser()).thenReturn(cloudbreakUser);
        when(workspaceService.get(anyLong(), any())).thenReturn(defaultWorkspace);
    }

    @Test
    public void testRetrieveRdsConfigsInDefaultWorkspace() {
        when(rdsConfigRepository.findAllByWorkspaceId(eq(1L))).thenReturn(Collections.singleton(testRdsConfig));

        Set<RDSConfig> rdsConfigs = underTest.retrieveRdsConfigsInWorkspace(defaultWorkspace);

        verify(rdsConfigRepository).findAllByWorkspaceId(eq(1L));
        assertEquals(1, rdsConfigs.size());
    }

    @Test
    public void testGetExistingRdsConfigByNameAndDefaultWorkspace() {
        when(rdsConfigRepository.findByNameAndWorkspaceId(eq(TEST_RDS_CONFIG_NAME), eq(1L))).thenReturn(Optional.ofNullable(testRdsConfig));

        underTest.getByNameForWorkspace(TEST_RDS_CONFIG_NAME, defaultWorkspace);

        verify(rdsConfigRepository).findByNameAndWorkspaceId(anyString(), eq(1L));
    }

    @Test
    public void testGetNonExistingRdsConfigByNameAndDefaultWorkspace() {
        when(rdsConfigRepository.findByNameAndWorkspaceId(anyString(), eq(1L))).thenReturn(Optional.empty());

        thrown.expect(NotFoundException.class);

        underTest.getByNameForWorkspace(TEST_RDS_CONFIG_NAME + "X", defaultWorkspace);
    }

    @Test
    public void testGetRdsConfigById() {
        when(rdsConfigRepository.findById(eq(1L))).thenReturn(Optional.of(testRdsConfig));

        RDSConfig rdsConfig = underTest.get(1L);

        verify(rdsConfigRepository).findById(eq(1L));
        assertEquals(TEST_RDS_CONFIG_NAME, rdsConfig.getName());
    }

    @Test
    public void testGetNonExistingRdsConfigById() {
        when(rdsConfigRepository.findById(anyLong())).thenReturn(Optional.empty());

        thrown.expect(NotFoundException.class);

        underTest.get(1L);
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
    public void testDeleteExistingUserManagedRdsConfigWithoutClusterById() {
        testRdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        mockClusterServiceWithEmptyList();
        when(rdsConfigRepository.findById(eq(1L))).thenReturn(Optional.of(testRdsConfig));

        underTest.delete(1L);

        verify(rdsConfigRepository).findById(eq(1L));
        verify(rdsConfigRepository, never()).delete(any());
        verify(rdsConfigRepository).save(testRdsConfig);
        assertTrue(testRdsConfig.isArchived());
    }

    @Test
    public void testDeleteExistingRdsConfigWithOneClusterById() {
        mockClusterServiceWithSingletonList();
        when(rdsConfigRepository.findById(eq(1L))).thenReturn(Optional.of(testRdsConfig));

        thrown.expect(BadRequestException.class);

        underTest.delete(1L);
    }

    @Test
    public void testDeleteExistingRdsConfigWithMoreClustersById() {
        mockClusterServiceWithList();
        when(rdsConfigRepository.findById(eq(1L))).thenReturn(Optional.of(testRdsConfig));

        thrown.expect(BadRequestException.class);

        underTest.delete(1L);
    }

    @Test
    public void testDeleteExistingNonUserManagedRdsConfigWithoutClusterById() {
        testRdsConfig.setStatus(ResourceStatus.DEFAULT);
        mockClusterServiceWithEmptyList();
        when(rdsConfigRepository.findById(eq(1L))).thenReturn(Optional.of(testRdsConfig));

        underTest.delete(1L);

        verify(rdsConfigRepository).save(rdsConfigCaptor.capture());
        assertEquals(ResourceStatus.DEFAULT, rdsConfigCaptor.getValue().getStatus());
        assertTrue(rdsConfigCaptor.getValue().isArchived());
    }

    @Test
    public void testDeleteNonExistingRdsConfigById() {
        when(rdsConfigRepository.findById(anyLong())).thenReturn(Optional.empty());

        thrown.expect(NotFoundException.class);
        thrown.expectMessage("RDS configuration '1' not found.");

        underTest.delete(1L);

        verify(rdsConfigRepository, never()).delete(any());
    }

    @Test
    public void testDeleteExistingRdsConfigByName() {
        testRdsConfig.setStatus(ResourceStatus.USER_MANAGED);
        mockClusterServiceWithEmptyList();
        when(rdsConfigRepository.findUserManagedByName(eq(TEST_RDS_CONFIG_NAME))).thenReturn(testRdsConfig);

        RDSConfig deleted = underTest.delete(TEST_RDS_CONFIG_NAME);

        verify(rdsConfigRepository).findUserManagedByName(eq(TEST_RDS_CONFIG_NAME));
        verify(rdsConfigRepository).save(any());
        verify(rdsConfigRepository, never()).delete(any());
        assertEquals(TEST_RDS_CONFIG_NAME, deleted.getName());
    }

    @Test
    public void testDeleteNonExistingRdsConfigByName() {
        when(rdsConfigRepository.findUserManagedByName(anyString())).thenReturn(null);

        thrown.expect(NotFoundException.class);
        thrown.expectMessage("RDS configuration '" + TEST_RDS_CONFIG_NAME + "' not found.");

        underTest.delete(TEST_RDS_CONFIG_NAME);

        verify(rdsConfigRepository, never()).delete(any());
    }

    @Test
    public void testRdsConnectionTestByNullRdsConfig() {
        doNothing().when(rdsConnectionValidator).validateRdsConnection(any());
        thrown.expect(BadRequestException.class);

        String result = underTest.testRdsConnection(1L, null, null);

        assertEquals("access is denied", result);
    }

    @Test
    public void testRdsConnectionTestByExistingRdsConfig() {
        doNothing().when(rdsConnectionValidator).validateRdsConnection(any());

        String result = underTest.testRdsConnection(1L, null, testRdsConfig);

        assertEquals("connected", result);
    }

    @Test
    public void testRdsConnectionTestWithException() {
        String testErrorString = "Test error";
        doThrow(new BadRequestException(testErrorString)).when(rdsConnectionValidator).validateRdsConnection(any());

        String result = underTest.testRdsConnection(1L, null, testRdsConfig);

        assertEquals(testErrorString, result);
    }

    @Test
    public void testRdsConnectionTestByName() {
        when(rdsConfigRepository.findByNameAndWorkspaceId(any(), any())).thenReturn(Optional.ofNullable(testRdsConfig));
        doNothing().when(rdsConnectionValidator).validateRdsConnection(any());

        String result = underTest.testRdsConnection(1L, TEST_RDS_CONFIG_NAME, null);

        assertEquals("connected", result);
    }

    @Test
    public void testRdsConnectionTestByNameWithException() {
        when(rdsConfigRepository.findByNameAndWorkspaceId(eq(TEST_RDS_CONFIG_NAME), eq(defaultWorkspace.getId()))).thenReturn(Optional.empty());

        String result = underTest.testRdsConnection(1L, TEST_RDS_CONFIG_NAME, null);

        assertEquals("access is denied", result);
    }

    @Test
    public void testNewRdsConfigCreation() throws TransactionExecutionException {
        when(rdsConfigRepository.findByNameAndWorkspaceId(eq(TEST_RDS_CONFIG_NAME), eq(1L))).thenReturn(Optional.empty());
        when(workspaceService.get(eq(1L), any(User.class))).thenReturn(defaultWorkspace);
        doAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get()).when(transactionService).required(any());
        when(workspaceService.retrieveForUser(any())).thenReturn(Collections.singleton(defaultWorkspace));
        when(rdsConfigRepository.save(any())).thenReturn(testRdsConfig);

        RDSConfig rdsConfig = underTest.createIfNotExists(new User(), testRdsConfig, 1L);

        assertEquals(testRdsConfig, rdsConfig);
    }

    @Test
    public void testExistingRdsConfigCreation() {
        when(rdsConfigRepository.findByNameAndWorkspaceId(eq(TEST_RDS_CONFIG_NAME), eq(1L))).thenReturn(Optional.ofNullable(testRdsConfig));

        RDSConfig rdsConfig = underTest.createIfNotExists(new User(), testRdsConfig, 1L);

        verify(workspaceService, never()).get(anyLong(), any(User.class));
        verifyZeroInteractions(transactionService);
        assertEquals(testRdsConfig, rdsConfig);
    }

    @Test
    public void testDeleteDefaultRdsConfigs() {
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
