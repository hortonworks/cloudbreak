package com.sequenceiq.cloudbreak.service.ldap;

import static org.junit.Assert.assertEquals;
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
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapMinimalV4Request;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.controller.validation.ldapconfig.LdapConfigValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class LdapConfigServiceTest {

    private static final Long WORKSPACE_ID = 1L;

    private static final String ENV_1 = "env1";

    private static final String ENV_2 = "env2";

    private static final String NONEXISTENT_WORKSPACE = "I am missing!";

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
    private ConversionService conversionService;

    @Mock
    private LdapConfigRepository ldapConfigRepository;

    @Mock
    private ClusterService clusterService;

    @Mock
    private LdapConfigValidator ldapConfigValidator;

    @Mock
    private Clock clock;

    @InjectMocks
    private LdapConfigService underTest;

    private final User user = new User();

    private final Workspace workspace = new Workspace();

    private LdapConfig ldapConfig = new LdapConfig();

    @Before
    public void setup() {
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
        ldapConfig.setWorkspace(workspace);
    }

    @Test
    public void testCreateInWorkspace() {
        LdapConfig created = underTest.createForLoggedInUser(ldapConfig, WORKSPACE_ID);

        assertEquals(workspace.getId(), created.getWorkspace().getId());
    }

    @Test
    public void testCreateInEnvironmentWithNonexistentEnvironment() {
        when(workspaceService.get(WORKSPACE_ID, user)).thenThrow(new NotFoundException(NONEXISTENT_WORKSPACE));
        exceptionRule.expect(NotFoundException.class);
        exceptionRule.expectMessage(NONEXISTENT_WORKSPACE);

        underTest.createForLoggedInUser(ldapConfig, WORKSPACE_ID);
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