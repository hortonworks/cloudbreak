package com.sequenceiq.cloudbreak.service.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
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
    }

    private void initLdapConfig() {
        ldapConfig = new LdapConfig();
        ldapConfig.setName(LDAP_1);
        ldapConfig.setId(1L);
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
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(ldapConfig);

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
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(ldapConfig);

        LdapConfig created = underTest.attachToEnvironments(LDAP_1, environments, WORKSPACE_ID);

        assertEquals(2, created.getEnvironments().size());
        assertTrue(created.getEnvironments().contains(env1));
        assertTrue(created.getEnvironments().contains(env2));
    }

    @Test
    public void testDelete() {
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(ldapConfig);
        when(clusterService.findByLdapConfigWithoutAuth(ldapConfig)).thenReturn(Collections.emptyList());

        underTest.deleteByNameFromWorkspace(ldapConfig.getName(), WORKSPACE_ID);

        verify(ldapConfigRepository, times(1)).delete(ldapConfig);
    }

    @Test
    public void testDeleteWithSingleDependingCluster() {
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        cluster.setName("cluster1");
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(ldapConfig);
        when(clusterService.findByLdapConfigWithoutAuth(ldapConfig)).thenReturn(List.of(cluster));

        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(String.format("There is a cluster ['%s'] which uses LDAP config '%s'.",
                cluster.getName(), ldapConfig.getName()));

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
        when(ldapConfigRepository.findByNameAndWorkspaceId(LDAP_1, WORKSPACE_ID)).thenReturn(ldapConfig);
        when(clusterService.findByLdapConfigWithoutAuth(ldapConfig)).thenReturn(List.of(cluster1, cluster2));

        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage(String.format("There are clusters associated with LDAP config '%s'.", ldapConfig.getName()));
        exceptionRule.expectMessage(clusterName1);
        exceptionRule.expectMessage(clusterName2);

        underTest.deleteByNameFromWorkspace(ldapConfig.getName(), WORKSPACE_ID);

        verify(ldapConfigRepository, never()).delete(any());
    }
}