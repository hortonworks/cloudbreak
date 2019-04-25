package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.network.EnvironmentNetworkValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCreationEnvironmentCreationValidatorTest {

    @Mock
    private ProxyConfigService proxyConfigService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private Map<CloudPlatform, EnvironmentNetworkValidator> environmentNetworkValidatorsByCloudPlatform;

    @InjectMocks
    private ClusterCreationEnvironmentValidator underTest;

    @Test
    public void testValidateShouldBeSuccessWhenStackRegionIsValidAndEnvironmentsResourcesAreNotGiven() throws IOException {
        // GIVEN
        Stack stack = getStack();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack);
        // THEN
        Assert.assertFalse(actualResult.hasError());
    }

    @Test
    public void testValidateShouldBeSuccessWhenNoEnvironmentProvided() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironment(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy", Sets.newHashSet());
        clusterRequest.setProxyName(proxyConfig.getName());
        Mockito.when(proxyConfigService.getByNameForWorkspaceId(proxyConfig.getName(), stack.getWorkspace().getId())).thenReturn(proxyConfig);
        LdapConfig ldapConfig = createLdapConfig("ldap", Sets.newHashSet());
        Mockito.when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        RDSConfig rdsConfig1 = createRdsConfig("rds1", Sets.newHashSet());
        Mockito.when(rdsConfigService.getByNameForWorkspaceId(rdsConfig1.getName(), stack.getWorkspace().getId())).thenReturn(rdsConfig1);
        RDSConfig rdsConfig2 = createRdsConfig("rds2", Sets.newHashSet());
        Mockito.when(rdsConfigService.getByNameForWorkspaceId(rdsConfig2.getName(), stack.getWorkspace().getId())).thenReturn(rdsConfig2);
        clusterRequest.setDatabases(Sets.newHashSet(rdsConfig1.getName(), rdsConfig2.getName()));
        RDSConfig rdsConfig3 = createRdsConfig("rds3", Sets.newHashSet());
        Mockito.when(rdsConfigService.get(rdsConfig3.getId())).thenReturn(rdsConfig3);
        RDSConfig rdsConfig4 = createRdsConfig("rds4", Sets.newHashSet());
        Mockito.when(rdsConfigService.get(rdsConfig4.getId())).thenReturn(rdsConfig4);
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack);
        // THEN
        Assert.assertFalse(actualResult.hasError());
    }

    @Test
    public void testValidateShouldBeSuccessWhenResourcesAreInTheSameEnvironmentOrGlobals() throws IOException {
        // GIVEN
        Stack stack = getStack();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy", Sets.newHashSet("env1", "env2"));
        clusterRequest.setProxyName(proxyConfig.getName());
        Mockito.when(proxyConfigService.getByNameForWorkspaceId(proxyConfig.getName(), stack.getWorkspace().getId())).thenReturn(proxyConfig);
        LdapConfig ldapConfig = createLdapConfig("ldap", Sets.newHashSet());
        Mockito.when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        RDSConfig rdsConfig1 = createRdsConfig("rds1", Sets.newHashSet());
        Mockito.when(rdsConfigService.getByNameForWorkspaceId(rdsConfig1.getName(), stack.getWorkspace().getId())).thenReturn(rdsConfig1);
        RDSConfig rdsConfig2 = createRdsConfig("rds2", Sets.newHashSet("env1", "env3"));
        Mockito.when(rdsConfigService.getByNameForWorkspaceId(rdsConfig2.getName(), stack.getWorkspace().getId())).thenReturn(rdsConfig2);
        clusterRequest.setDatabases(Sets.newHashSet(rdsConfig1.getName(), rdsConfig2.getName()));
        RDSConfig rdsConfig3 = createRdsConfig("rds3", Sets.newHashSet("env1", "env2"));
        Mockito.when(rdsConfigService.get(rdsConfig3.getId())).thenReturn(rdsConfig3);
        RDSConfig rdsConfig4 = createRdsConfig("rds4", Sets.newHashSet("env1", "env5"));
        Mockito.when(rdsConfigService.get(rdsConfig4.getId())).thenReturn(rdsConfig4);
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack);
        // THEN
        Assert.assertFalse(actualResult.hasError());
    }

    @Test
    public void testValidateShouldBeFailedWhenStackRegionIsInvalidAndEnvironmentsResourcesAreNotInGoodEnvironment() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setRegion("region3");
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy", Sets.newHashSet("env2", "env3"));
        clusterRequest.setProxyName(proxyConfig.getName());
        Mockito.when(proxyConfigService.getByNameForWorkspaceId(proxyConfig.getName(), stack.getWorkspace().getId())).thenReturn(proxyConfig);
        LdapConfig ldapConfig = createLdapConfig("ldap", Sets.newHashSet());
        Mockito.when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        RDSConfig rdsConfig3 = createRdsConfig("rds1", Sets.newHashSet("env2", "env3"));
        Mockito.when(rdsConfigService.get(rdsConfig3.getId())).thenReturn(rdsConfig3);
        RDSConfig rdsConfig4 = createRdsConfig("rds2", Sets.newHashSet("env4", "env5"));
        Mockito.when(rdsConfigService.get(rdsConfig4.getId())).thenReturn(rdsConfig4);
        RDSConfig rdsConfig1 = createRdsConfig("rds3", Sets.newHashSet());
        Mockito.when(rdsConfigService.getByNameForWorkspaceId(rdsConfig1.getName(), stack.getWorkspace().getId())).thenReturn(rdsConfig1);
        RDSConfig rdsConfig2 = createRdsConfig("rds4", Sets.newHashSet("env2", "env3"));
        Mockito.when(rdsConfigService.getByNameForWorkspaceId(rdsConfig2.getName(), stack.getWorkspace().getId())).thenReturn(rdsConfig2);
        clusterRequest.setDatabases(Sets.newLinkedHashSet(Arrays.asList(rdsConfig1.getName(), rdsConfig2.getName())));
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack);
        // THEN
        Assert.assertTrue(actualResult.hasError());
        Assert.assertEquals(3, actualResult.getErrors().size());
        Assert.assertEquals("[region3] region is not enabled in [env1] environment. Enabled environments: [region1,region2]",
                actualResult.getErrors().get(0));
        Assert.assertEquals("Stack cannot use proxy ProxyConfig resource which is not attached to env1 environment and not global.",
                actualResult.getErrors().get(1));
        Assert.assertEquals("Stack cannot use rds4 RDSConfig resource which is not attached to env1 environment and not global.",
                actualResult.getErrors().get(2));
    }

    @Test
    public void testValidateShouldBeFailedWhenStackEnvIsNullButEnvironmentsResourcesAreNotGlobals() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironment(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy", Sets.newHashSet("env2", "env3"));
        clusterRequest.setProxyName(proxyConfig.getName());
        Mockito.when(proxyConfigService.getByNameForWorkspaceId(proxyConfig.getName(), stack.getWorkspace().getId())).thenReturn(proxyConfig);
        LdapConfig ldapConfig = createLdapConfig("ldap", Sets.newHashSet());
        Mockito.when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        RDSConfig rdsConfig1 = createRdsConfig("rds3", Sets.newHashSet());
        Mockito.when(rdsConfigService.getByNameForWorkspaceId(rdsConfig1.getName(), stack.getWorkspace().getId())).thenReturn(rdsConfig1);
        RDSConfig rdsConfig2 = createRdsConfig("rds4", Sets.newHashSet("env2", "env3"));
        Mockito.when(rdsConfigService.getByNameForWorkspaceId(rdsConfig2.getName(), stack.getWorkspace().getId())).thenReturn(rdsConfig2);
        clusterRequest.setDatabases(Sets.newLinkedHashSet(Arrays.asList(rdsConfig1.getName(), rdsConfig2.getName())));
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack);
        // THEN
        Assert.assertTrue(actualResult.hasError());
        Assert.assertEquals(2, actualResult.getErrors().size());
        Assert.assertEquals("Stack without environment cannot use proxy ProxyConfig resource which attached to an environment.",
                actualResult.getErrors().get(0));
        Assert.assertEquals("Stack without environment cannot use rds4 RDSConfig resource which attached to an environment.",
                actualResult.getErrors().get(1));
    }

    private Stack getStack() throws IOException {
        Stack stack = new Stack();
        stack.setRegion("region1");
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        stack.setWorkspace(workspace);
        EnvironmentView environmentView = new EnvironmentView();
        environmentView.setName("env1");
        Region region1 = new Region();
        region1.setName("region1");
        Region region2 = new Region();
        region2.setName("region2");
        environmentView.setRegions(new Json(Set.of(region1, region2)));
        stack.setEnvironment(environmentView);
        return stack;
    }

    private LdapConfig createLdapConfig(String name, Set<String> environments) {
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setId(TestUtil.generateUniqueId());
        ldapConfig.setName(name);
        ldapConfig.setEnvironments(environments.stream().map(env -> createEnvironmentView(env)).collect(Collectors.toSet()));
        return ldapConfig;
    }

    private ProxyConfig createProxyConfig(String name, Set<String> environments) {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setId(TestUtil.generateUniqueId());
        proxyConfig.setName(name);
        proxyConfig.setEnvironments(environments.stream().map(env -> createEnvironmentView(env)).collect(Collectors.toSet()));
        return proxyConfig;
    }

    private DatabaseV4Request createRdsConfigRequest(String name, Set<String> environments) {
        DatabaseV4Request rdsConfigRequest = new DatabaseV4Request();
        rdsConfigRequest.setName(name);
        rdsConfigRequest.setEnvironments(environments);
        return rdsConfigRequest;
    }

    private RDSConfig createRdsConfig(String name, Set<String> environments) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(TestUtil.generateUniqueId());
        rdsConfig.setName(name);
        rdsConfig.setEnvironments(environments.stream().map(env -> createEnvironmentView(env)).collect(Collectors.toSet()));
        return rdsConfig;
    }

    private EnvironmentView createEnvironmentView(String name) {
        EnvironmentView env = new EnvironmentView();
        env.setId(TestUtil.generateUniqueId());
        env.setName(name);
        return env;
    }
}
