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
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig.ProxyConfigBuilder;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@RunWith(MockitoJUnitRunner.class)
public class ClusterCreationEnvironmentValidatorTest {

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private User user;

    @InjectMocks
    private ClusterCreationEnvironmentValidator underTest;

    @Test
    public void testValidateShouldBeSuccessWhenStackRegionIsValidAndEnvironmentsResourcesAreNotGiven() throws IOException {
        // GIVEN
        Stack stack = getStack();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertFalse(actualResult.hasError());
    }

    private DetailedEnvironmentResponse getEnvironmentResponse() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.setCredential(new CredentialResponse());
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(Set.of("region1"));
        environmentResponse.setRegions(compactRegionResponse);
        return environmentResponse;
    }

    @Test
    public void testValidateShouldBeSuccessWhenNoEnvironmentProvided() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironmentCrn(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
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
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
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
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        environment.getRegions().setNames(Set.of("region1", "region2"));
        environment.setName("env1");
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        Assert.assertTrue(actualResult.hasError());
        Assert.assertEquals(1, actualResult.getErrors().size());
        Assert.assertEquals("[region3] region is not enabled in [env1] environment. Enabled regions: [region1,region2]",
                actualResult.getErrors().get(0));
    }

    @Test
    public void testValidateShouldWorkWhenStackEnvIsNullButResourcesCouldBeFoundInTheSameWorkspace() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironmentCrn(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        String rdsName = "anrds";
        RDSConfig rdsConfig = createRdsConfig(rdsName);
        when(rdsConfigService.getByNamesForWorkspaceId(Set.of(rdsConfig.getName()), stack.getWorkspace().getId())).thenReturn(Set.of(rdsConfig));
        clusterRequest.setDatabases(Set.of(rdsName));
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    public void testValidateShouldFailWhenProxyCouldNotBeFoundInTheSameWorkspace() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironmentCrn(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        when(proxyConfigDtoService.getByCrn(anyString())).thenThrow(new CloudbreakServiceException("Some reason"));
        String rdsName = "anrds";
        RDSConfig rdsConfig = createRdsConfig(rdsName);
        when(rdsConfigService.getByNamesForWorkspaceId(Set.of(rdsConfig.getName()), stack.getWorkspace().getId())).thenReturn(Set.of(rdsConfig));
        clusterRequest.setDatabases(Set.of(rdsName));
        when(user.getUserCrn()).thenReturn("aUserCRN");
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertTrue(actualResult.hasError());
        Assert.assertEquals(1, actualResult.getErrors().size());
        Assert.assertEquals("The specified 'proxy' Proxy config resource couldn't be used: Some reason.",
                actualResult.getErrors().get(0));
    }

    @Test
    public void testValidateShouldFailWhenRdsCouldNotBeFoundInTheSameWorkspace() throws IOException {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironmentCrn(null);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        String rdsName = "anrds";
        String rdsName2 = "aSecondrds";
        when(rdsConfigService.getByNamesForWorkspaceId(Set.of(rdsName, rdsName2), stack.getWorkspace().getId())).thenReturn(Set.of());
        clusterRequest.setDatabases(Set.of(rdsName, rdsName2));
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
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
        stack.setEnvironmentCrn("");
        return stack;
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
