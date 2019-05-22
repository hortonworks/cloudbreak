package com.sequenceiq.cloudbreak.controller.validation.environment;

import java.io.IOException;
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
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.environment.network.EnvironmentNetworkValidator;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

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
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack);
        // THEN
        Assert.assertTrue(actualResult.hasError());
        Assert.assertEquals(2, actualResult.getErrors().size());
        Assert.assertEquals("[region3] region is not enabled in [env1] environment. Enabled environments: [region1,region2]",
                actualResult.getErrors().get(0));
        Assert.assertEquals("Stack cannot use proxy ProxyConfig resource which is not attached to env1 environment and not global.",
                actualResult.getErrors().get(1));
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
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack);
        // THEN
        Assert.assertTrue(actualResult.hasError());
        Assert.assertEquals(1, actualResult.getErrors().size());
        Assert.assertEquals("Stack without environment cannot use proxy ProxyConfig resource which attached to an environment.",
                actualResult.getErrors().get(0));
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
        return ldapConfig;
    }

    private ProxyConfig createProxyConfig(String name, Set<String> environments) {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setId(TestUtil.generateUniqueId());
        proxyConfig.setName(name);
        proxyConfig.setEnvironments(environments.stream().map(env -> createEnvironmentView(env)).collect(Collectors.toSet()));
        return proxyConfig;
    }

    private RDSConfig createRdsConfig(String name, Set<String> environments) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(TestUtil.generateUniqueId());
        rdsConfig.setName(name);
        return rdsConfig;
    }

    private EnvironmentView createEnvironmentView(String name) {
        EnvironmentView env = new EnvironmentView();
        env.setId(TestUtil.generateUniqueId());
        env.setName(name);
        return env;
    }
}
