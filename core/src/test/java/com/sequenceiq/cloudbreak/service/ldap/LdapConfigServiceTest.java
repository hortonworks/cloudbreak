package com.sequenceiq.cloudbreak.service.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapMinimalV4Request;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.environment.ResourceDetachValidator;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentViewService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class LdapConfigServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String ENV_1 = "env1";

    private static final String ENV_2 = "env2";

    private static final String NONEXISTENT_ENV = "I am missing!";

    private static final String LDAP_1 = "ldap1";

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private UserService userService;

    @Mock
    private EnvironmentViewService environmentViewService;

    @Mock
    private ConversionService conversionService;

    @Mock
    private LdapConfigRepository ldapConfigRepository;

    @Mock
    private ClusterService clusterService;

    @Mock
    private LdapConfigValidator ldapConfigValidator;

    @Mock
    private Clock clock;

    @Spy
    private ResourceDetachValidator resourceDetachValidator = new ResourceDetachValidator();

    @InjectMocks
    private LdapConfigService underTest;

    private final User user = new User();

    private final Workspace workspace = new Workspace();

    private final EnvironmentView env1 = new EnvironmentView();

    private final EnvironmentView env2 = new EnvironmentView();

    private LdapConfig ldapConfig = new LdapConfig();

    @Before
    public void setup() {
        workspace.setId(WORKSPACE_ID);
        env1.setId(1L);
        env1.setName(ENV_1);
        env2.setId(2L);
        env2.setName(ENV_2);
        initLdapConfig();
        when(userService.getOrCreate(any())).thenReturn(user);
        when(workspaceService.get(WORKSPACE_ID, user)).thenReturn(workspace);
        when(workspaceService.retrieveForUser(user)).thenReturn(Set.of(workspace));
        when(ldapConfigRepository.save(any(LdapConfig.class)))
                .thenAnswer((Answer<LdapConfig>) invocation -> (LdapConfig) invocation.getArgument(0));
        assertNotNull(resourceDetachValidator);
    }

    private void initLdapConfig() {
        ldapConfig = new LdapConfig();
        ldapConfig.setName(LDAP_1);
        ldapConfig.setId(1L);
        ldapConfig.setWorkspace(workspace);
    }

    @Test
    public void testCreateInEnvironment() {
        Set<String> environments = Set.of(ENV_1, ENV_2);
        Set<EnvironmentView> environmentViews = Set.of(env1, env2);

        when(environmentViewService.findByNamesInWorkspace(environments, WORKSPACE_ID)).thenReturn(environmentViews);

        LdapConfig created = underTest.createInEnvironment(ldapConfig, environments, WORKSPACE_ID);

        assertEquals(workspace.getId(), created.getWorkspace().getId());
        assertEquals(environments.size(), created.getEnvironments().size());
        assertTrue(created.getEnvironments().contains(env1));
        assertTrue(created.getEnvironments().contains(env2));
    }

    @Test
    public void testCreateInEnvironmentWithNonexistentEnvironment() {
        Set<String> environments = Set.of(ENV_1, ENV_2, NONEXISTENT_ENV);
        Set<EnvironmentView> environmentViews = Set.of(env1, env2);

        when(environmentViewService.findByNamesInWorkspace(environments, WORKSPACE_ID)).thenReturn(environmentViews);
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(NONEXISTENT_ENV);

        underTest.createInEnvironment(ldapConfig, environments, WORKSPACE_ID);
    }

    @Test
    public void testAttach() {
        Set<String> environments = Set.of(ENV_1, ENV_2);
        Set<EnvironmentView> environmentViews = Set.of(env1, env2);

        when(environmentViewService.findByNamesInWorkspace(environments, WORKSPACE_ID)).thenReturn(environmentViews);
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(Optional.ofNullable(ldapConfig));

        LdapConfig created = underTest.attachToEnvironments(LDAP_1, environments, WORKSPACE_ID);

        assertEquals(environments.size(), created.getEnvironments().size());
        assertTrue(created.getEnvironments().contains(env1));
        assertTrue(created.getEnvironments().contains(env2));
    }

    @Test
    public void testAttachToNonExistentEnv() {
        Set<String> environments = Set.of(ENV_1, ENV_2, NONEXISTENT_ENV);
        Set<EnvironmentView> environmentViews = Set.of(env1, env2);

        when(environmentViewService.findByNamesInWorkspace(environments, WORKSPACE_ID)).thenReturn(environmentViews);

        when(environmentViewService.findByNamesInWorkspace(environments, WORKSPACE_ID)).thenReturn(environmentViews);
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(NONEXISTENT_ENV);

        underTest.attachToEnvironments(LDAP_1, environments, WORKSPACE_ID);
    }

    @Test
    public void testAttachWithAlreadyAttachedEnv() {
        ldapConfig.setEnvironments(Sets.newHashSet(env1, env2));
        Set<String> environments = Sets.newHashSet(ENV_1, ENV_2);
        Set<EnvironmentView> environmentViews = Sets.newHashSet(env1, env2);

        when(environmentViewService.findByNamesInWorkspace(environments, WORKSPACE_ID)).thenReturn(environmentViews);
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(Optional.ofNullable(ldapConfig));

        LdapConfig created = underTest.attachToEnvironments(LDAP_1, environments, WORKSPACE_ID);

        assertEquals(2, created.getEnvironments().size());
        assertTrue(created.getEnvironments().contains(env1));
        assertTrue(created.getEnvironments().contains(env2));
    }

    @Test
    public void testDelete() {
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(Optional.ofNullable(ldapConfig));
        when(clusterService.findByLdapConfig(ldapConfig)).thenReturn(Collections.emptySet());

        underTest.deleteByNameFromWorkspace(ldapConfig.getName(), WORKSPACE_ID);

        verify(ldapConfigRepository, never()).delete(ldapConfig);
        assertTrue(ldapConfig.isArchived());
    }

    @Test
    public void testDeleteWithSingleDependingCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(Optional.ofNullable(ldapConfig));
        when(clusterService.findByLdapConfig(ldapConfig)).thenReturn(Set.of(cluster));

        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(String.format("LDAP config '%s' cannot be deleted because there are clusters associated with it: [%s].",
                ldapConfig.getName(), cluster.getName()));

        underTest.deleteByNameFromWorkspace(ldapConfig.getName(), WORKSPACE_ID);

        verify(ldapConfigRepository, never()).delete(any());
    }

    @Test
    public void testDeleteWithMultipleDependingClusters() {
        Cluster cluster1 = new Cluster();
        cluster1.setId(1L);
        String clusterName1 = "cluster1";
        cluster1.setName(clusterName1);
        Cluster cluster2 = new Cluster();
        cluster2.setId(2L);
        String clusterName2 = "cluster2";
        cluster2.setName(clusterName2);
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(Optional.ofNullable(ldapConfig));
        when(clusterService.findByLdapConfig(ldapConfig)).thenReturn(Set.of(cluster1, cluster2));

        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(String.format("LDAP config '%s' cannot be deleted because there are clusters associated with it: ", ldapConfig.getName()));
        exceptionRule.expectMessage(clusterName1);
        exceptionRule.expectMessage(clusterName2);

        underTest.deleteByNameFromWorkspace(ldapConfig.getName(), WORKSPACE_ID);

        verify(ldapConfigRepository, never()).delete(any());
    }

    @Test
    public void testDetachHappyPath() {
        EnvironmentView env1 = new EnvironmentView();
        Long envId1 = 1L;
        env1.setId(envId1);
        String envName1 = "env1";
        env1.setName(envName1);

        EnvironmentView env2 = new EnvironmentView();
        Long envId2 = 2L;
        env2.setId(envId2);
        String envName2 = "env2";
        env2.setName(envName2);

        Set<String> envNames = Sets.newHashSet(envName1, envName2);
        Set<EnvironmentView> environments = Sets.newHashSet(env1, env2);

        EnvironmentView env3 = new EnvironmentView();
        Long envId3 = 3L;
        env3.setId(envId3);
        String envName3 = "env3";
        env3.setName(envName3);

        ldapConfig.setEnvironments(Sets.newHashSet(env1, env2, env3));

        when(environmentViewService.findByNamesInWorkspace(envNames, WORKSPACE_ID)).thenReturn(environments);
        when(ldapConfigRepository.findByNameAndWorkspaceId(ldapConfig.getName(), WORKSPACE_ID)).thenReturn(Optional.ofNullable(ldapConfig));
        when(clusterService.findAllClustersByLdapConfigInEnvironment(ldapConfig, envId1)).thenReturn(Collections.emptySet());
        when(clusterService.findAllClustersByLdapConfigInEnvironment(ldapConfig, envId2)).thenReturn(Collections.emptySet());

        LdapConfig result = underTest.detachFromEnvironments(LDAP_1, envNames, WORKSPACE_ID);
        assertEquals(1, result.getEnvironments().size());
        assertTrue(result.getEnvironments().contains(env3));
    }

    @Test
    public void testDetachWithLdapIsUsedByClustersInEnvironments() {
        EnvironmentView env1 = new EnvironmentView();
        Long envId1 = 1L;
        env1.setId(envId1);
        String envName1 = "env1";
        env1.setName(envName1);

        EnvironmentView env2 = new EnvironmentView();
        Long envId2 = 2L;
        env2.setId(envId2);
        String envName2 = "env2";
        env2.setName(envName2);
        Set<String> envNames = Sets.newHashSet(envName1, envName2);
        Set<EnvironmentView> environments = Sets.newHashSet(env1, env2);

        Cluster env1Cluster1 = new Cluster();
        env1Cluster1.setId(1L);
        String env1Cluster1Name = "env1Cluster1";
        env1Cluster1.setName(env1Cluster1Name);
        env1Cluster1.setEnvironment(env1);

        Cluster env1Cluster2 = new Cluster();
        env1Cluster2.setId(2L);
        String env1Cluster2Name = "env1Cluster2";
        env1Cluster2.setName(env1Cluster2Name);
        env1Cluster2.setEnvironment(env1);

        Cluster env2Cluster1 = new Cluster();
        env2Cluster1.setId(3L);
        String env2Cluster1Name = "env2Cluster1";
        env2Cluster1.setName(env2Cluster1Name);
        env2Cluster1.setEnvironment(env2);

        Cluster env2Cluster2 = new Cluster();
        env2Cluster2.setId(4L);
        String env2Cluster2Name = "env2Cluster2";
        env2Cluster2.setName(env2Cluster2Name);
        env2Cluster2.setEnvironment(env2);

        when(environmentViewService.findByNamesInWorkspace(envNames, WORKSPACE_ID)).thenReturn(environments);
        when(ldapConfigRepository.findByNameAndWorkspaceId(ldapConfig.getName(), WORKSPACE_ID)).thenReturn(Optional.ofNullable(ldapConfig));
        when(clusterService.findAllClustersByLdapConfigInEnvironment(ldapConfig, envId1)).thenReturn(Sets.newHashSet(env1Cluster1, env1Cluster2));
        when(clusterService.findAllClustersByLdapConfigInEnvironment(ldapConfig, envId2)).thenReturn(Sets.newHashSet(env2Cluster1, env2Cluster2));

        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(String.format("%s '%s' cannot be detached from environment '%s' because it is used by the following cluster(s): [%s, %s]",
                ldapConfig.getResource().getReadableName(), ldapConfig.getName(), envName1, env1Cluster1Name, env1Cluster2Name));
        exceptionRule.expectMessage(String.format("%s '%s' cannot be detached from environment '%s' because it is used by the following cluster(s): [%s, %s]",
                ldapConfig.getResource().getReadableName(), ldapConfig.getName(), envName2, env2Cluster1Name, env2Cluster2Name));

        underTest.detachFromEnvironments(LDAP_1, envNames, WORKSPACE_ID);
    }

    @Test
    public void testConnectionWithEmptyInput() {
        exceptionRule.expect(BadRequestException.class);

        underTest.testConnection(1L, null, null);
    }

    @Test
    public void testConnectionWithNameWhenValidatorThrowsAnException() {
        String exceptionMessage = "test connection failed";
        doThrow(new BadRequestException(exceptionMessage)).when(ldapConfigValidator).validateLdapConnection(any(LdapConfig.class));
        when(ldapConfigRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(new LdapConfig()));

        String result = underTest.testConnection(1L, "LDAP", null);

        assertEquals(exceptionMessage, result);
        verify(ldapConfigValidator, never()).validateLdapConnection(any(LdapMinimalV4Request.class));
        verify(ldapConfigValidator, times(1)).validateLdapConnection(any(LdapConfig.class));
    }

    @Test
    public void testConnectionWithName() {
        doNothing().when(ldapConfigValidator).validateLdapConnection(any(LdapConfig.class));
        when(ldapConfigRepository.findByNameAndWorkspaceId(anyString(), anyLong())).thenReturn(Optional.of(new LdapConfig()));

        String result = underTest.testConnection(1L, "LDAP", null);

        assertEquals("connected", result);
        verify(ldapConfigValidator, never()).validateLdapConnection(any(LdapMinimalV4Request.class));
        verify(ldapConfigValidator, times(1)).validateLdapConnection(any(LdapConfig.class));
    }

    @Test
    public void testConnectionWithResourceWhenValidatorThrowsAnException() {
        String exceptionMessage = "test connection failed";
        doThrow(new BadRequestException(exceptionMessage)).when(ldapConfigValidator).validateLdapConnection(any(LdapMinimalV4Request.class));

        String result = underTest.testConnection(1L, null, new LdapMinimalV4Request());

        assertEquals(exceptionMessage, result);
        verify(ldapConfigValidator, never()).validateLdapConnection(any(LdapConfig.class));
        verify(ldapConfigValidator, times(1)).validateLdapConnection(any(LdapMinimalV4Request.class));
        verify(ldapConfigRepository, never()).findByNameAndWorkspaceId(anyString(), anyLong());
    }

    @Test
    public void testConnectionWitResource() {
        doNothing().when(ldapConfigValidator).validateLdapConnection(any(LdapMinimalV4Request.class));

        String result = underTest.testConnection(1L, null, new LdapMinimalV4Request());

        assertEquals("connected", result);
        verify(ldapConfigValidator, never()).validateLdapConnection(any(LdapConfig.class));
        verify(ldapConfigValidator, times(1)).validateLdapConnection(any(LdapMinimalV4Request.class));
        verify(ldapConfigRepository, never()).findByNameAndWorkspaceId(anyString(), anyLong());
    }
}