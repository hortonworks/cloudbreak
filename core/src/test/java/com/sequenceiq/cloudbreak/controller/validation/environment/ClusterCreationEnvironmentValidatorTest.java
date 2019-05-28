package com.sequenceiq.cloudbreak.controller.validation.environment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.environment.Region;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.EnvironmentView;
import com.sequenceiq.cloudbreak.dto.ProxyConfig.ProxyConfigBuilder;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.ldapconfig.LdapConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCreationEnvironmentValidatorTest {

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock
    private LdapConfigService ldapConfigService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private User user;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @InjectMocks
    private ClusterCreationEnvironmentValidator underTest;

    @Test
    public void testValidateShouldBeSuccessWhenStackRegionIsValidAndEnvironmentsResourcesAreNotGiven() throws IOException {
        // GIVEN
        Stack stack = getStack();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, user);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    public void testValidateShouldBeSuccessWhenNoEnvironmentProvided() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironment(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        LdapConfig ldapConfig = createLdapConfig("ldap");
        when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, user);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    public void testValidateShouldBeSuccessWhenResourcesAreInTheSameEnvironmentOrGlobals() throws IOException {
        // GIVEN
        Stack stack = getStack();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        LdapConfig ldapConfig = createLdapConfig("ldap");
        when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, user);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    public void testValidateShouldBeFailedWhenStackRegionIsInvalidAndEnvironmentsResourcesAreNotInGoodEnvironment() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setRegion("region3");
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        LdapConfig ldapConfig = createLdapConfig("ldap");
        when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, user);
        // THEN
        Assert.assertTrue(actualResult.hasError());
        Assert.assertEquals(1, actualResult.getErrors().size());
        Assert.assertEquals("[region3] region is not enabled in [env1] environment. Enabled environments: [region1,region2]",
                actualResult.getErrors().get(0));
    }

    @Test
    public void testValidateShouldWorkWhenStackEnvIsNullButResourcesCouldBeFoundInTheSameWorkspace() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironment(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        LdapConfig ldapConfig = createLdapConfig("ldap");
        when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        String rdsName = "anrds";
        RDSConfig rdsConfig = createRdsConfig(rdsName);
        when(rdsConfigService.getByNamesForWorkspaceId(Set.of(rdsConfig.getName()), stack.getWorkspace().getId())).thenReturn(Set.of(rdsConfig));
        clusterRequest.setDatabases(Set.of(rdsName));
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, user);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    public void testValidateShouldFailWhenProxyCouldNotBeFoundInTheSameWorkspace() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironment(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        when(proxyConfigDtoService.get(anyString(), anyString(), anyString())).thenThrow(new CloudbreakServiceException("Some reason"));
        LdapConfig ldapConfig = createLdapConfig("ldap");
        when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        String rdsName = "anrds";
        RDSConfig rdsConfig = createRdsConfig(rdsName);
        when(rdsConfigService.getByNamesForWorkspaceId(Set.of(rdsConfig.getName()), stack.getWorkspace().getId())).thenReturn(Set.of(rdsConfig));
        clusterRequest.setDatabases(Set.of(rdsName));
        when(user.getUserCrn()).thenReturn("aUserCRN");
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, user);
        // THEN
        assertTrue(actualResult.hasError());
        Assert.assertEquals(1, actualResult.getErrors().size());
        Assert.assertEquals("The specified 'proxy' Proxy config resource couldn't be used: Some reason.",
                actualResult.getErrors().get(0));
    }

    @Test
    public void testValidateShouldFailWhenLdapCouldNotBeFoundInTheSameWorkspace() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironment(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        LdapConfig ldapConfig = createLdapConfig("ldap");
        when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenThrow(new NotFoundException(""));
        clusterRequest.setLdapName("ldap");
        String rdsName = "anrds";
        RDSConfig rdsConfig = createRdsConfig(rdsName);
        when(rdsConfigService.getByNamesForWorkspaceId(Set.of(rdsConfig.getName()), stack.getWorkspace().getId())).thenReturn(Set.of(rdsConfig));
        clusterRequest.setDatabases(Set.of(rdsName));
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, user);
        // THEN
        assertTrue(actualResult.hasError());
        Assert.assertEquals(1, actualResult.getErrors().size());
        Assert.assertEquals("Stack cannot use 'ldap' LdapConfig resource which doesn't exist in the same workspace.",
                actualResult.getErrors().get(0));
    }

    @Test
    public void testValidateShouldFailWhenRdsCouldNotBeFoundInTheSameWorkspace() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironment(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        LdapConfig ldapConfig = createLdapConfig("ldap");
        when(ldapConfigService.getByNameForWorkspaceId(ldapConfig.getName(), stack.getWorkspace().getId())).thenReturn(ldapConfig);
        clusterRequest.setLdapName("ldap");
        String rdsName = "anrds";
        String rdsName2 = "aSecondrds";
        when(rdsConfigService.getByNamesForWorkspaceId(Set.of(rdsName, rdsName2), stack.getWorkspace().getId())).thenReturn(Set.of());
        clusterRequest.setDatabases(Set.of(rdsName, rdsName2));
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, user);
        // THEN
        assertTrue(actualResult.hasError());
        Assert.assertEquals(2, actualResult.getErrors().size());
        Assert.assertTrue(actualResult.getErrors().contains("Stack cannot use 'anrds' Database resource which doesn't exist in the same workspace."));
        Assert.assertTrue(actualResult.getErrors().contains("Stack cannot use 'aSecondrds' Database resource which doesn't exist in the same workspace."));
    }

    private Stack getStack() throws IOException {
        Stack stack = new Stack();
        stack.setRegion("region1");
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        Tenant tenant = new Tenant();
        tenant.setName("test-tenant-name");
        workspace.setTenant(tenant);
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

    private LdapConfig createLdapConfig(String name) {
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setId(TestUtil.generateUniqueId());
        ldapConfig.setName(name);
        return ldapConfig;
    }

    private ProxyConfig createProxyConfig(String name) {
        ProxyConfigBuilder proxyConfigBuilder = ProxyConfig.builder();
        proxyConfigBuilder.withCrn(UUID.randomUUID().toString());
        proxyConfigBuilder.withName(name);
        return proxyConfigBuilder.build();
    }

    private RDSConfig createRdsConfig(String name) {
        RDSConfig rdsConfig = new RDSConfig();
        rdsConfig.setId(TestUtil.generateUniqueId());
        rdsConfig.setName(name);
        return rdsConfig;
    }
}
