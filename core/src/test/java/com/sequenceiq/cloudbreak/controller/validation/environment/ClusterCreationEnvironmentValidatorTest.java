package com.sequenceiq.cloudbreak.controller.validation.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.KerberosConfig.KerberosConfigBuilder;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.dto.ProxyConfig.ProxyConfigBuilder;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class ClusterCreationEnvironmentValidatorTest {

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private User user;

    @Mock
    private KerberosConfigService kerberosConfigService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CloudConnector<Object> connector;

    @Mock
    private PlatformParameters platformParameters;

    @InjectMocks
    private ClusterCreationEnvironmentValidator underTest;

    @BeforeEach
    void setUp() {
        KerberosConfig kerberosConfig = KerberosConfigBuilder.aKerberosConfig().withType(KerberosType.FREEIPA).build();
        when(kerberosConfigService.get(any(), any())).thenReturn(Optional.of(kerberosConfig));
        when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        when(connector.parameters()).thenReturn(platformParameters);
        when(connector.displayNameToRegion(any())).thenReturn("region1");
        when(platformParameters.isAutoTlsSupported()).thenReturn(true);
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsValidAndEnvironmentsResourcesAreNotGiven() {
        // GIVEN
        Stack stack = getStack();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsRegionNameAndEnvironmentsRegionsAreDisplayNames() {
        // GIVEN
        String westUs2RegionName = "westus2";
        String westUs2RegionDisplayName = "West US 2";
        Stack stack = getStack();
        stack.setRegion(westUs2RegionName);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.getNames().add(westUs2RegionDisplayName);
        regions.getNames().add("West US");
        environment.setRegions(regions);
        when(connector.displayNameToRegion(any())).thenReturn(westUs2RegionName);
        when(connector.regionToDisplayName(any())).thenReturn(westUs2RegionDisplayName);
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsRegionDisplayNameAndEnvironmentsRegionsAreDisplayNames() {
        // GIVEN
        String westUs2RegionName = "westus2";
        String westUs2RegionDisplayName = "West US 2";
        Stack stack = getStack();
        stack.setRegion(westUs2RegionDisplayName);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.getNames().add(westUs2RegionDisplayName);
        regions.getNames().add("West US");
        environment.setRegions(regions);
        when(connector.displayNameToRegion(any())).thenReturn(westUs2RegionName);
        when(connector.regionToDisplayName(any())).thenReturn(westUs2RegionDisplayName);
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsRegionNameAndEnvironmentsRegionsAreRegionNames() {
        // GIVEN
        String westUs2RegionName = "westus2";
        String westUs2RegionDisplayName = "West US 2";
        Stack stack = getStack();
        stack.setRegion(westUs2RegionName);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.getNames().add(westUs2RegionName);
        regions.getNames().add("westus");
        environment.setRegions(regions);
        when(connector.displayNameToRegion(any())).thenReturn(westUs2RegionName);
        when(connector.regionToDisplayName(any())).thenReturn(westUs2RegionDisplayName);
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsRegionDisplayNameAndEnvironmentsRegionsAreRegionNames() {
        // GIVEN
        String westUs2RegionName = "westus2";
        String westUs2RegionDisplayName = "West US 2";
        Stack stack = getStack();
        stack.setRegion(westUs2RegionDisplayName);
        ClusterV4Request clusterRequest = new ClusterV4Request();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.getNames().add(westUs2RegionName);
        regions.getNames().add("westus");
        environment.setRegions(regions);
        when(connector.displayNameToRegion(any())).thenReturn(westUs2RegionName);
        when(connector.regionToDisplayName(any())).thenReturn(westUs2RegionDisplayName);
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenNoEnvironmentProvided() {
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
    void testValidateShouldBeSuccessWhenResourcesAreInTheSameEnvironmentOrGlobals() {
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
    void testValidateShouldBeFailedWhenStackRegionIsInvalidAndEnvironmentsResourcesAreNotInGoodEnvironment() {
        // GIVEN
        Stack stack = getStack();
        stack.setRegion("region3");
        when(connector.displayNameToRegion(any())).thenReturn("region666");
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ProxyConfig proxyConfig = createProxyConfig("proxy");
        clusterRequest.setProxyConfigCrn(proxyConfig.getName());
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        environment.getRegions().setNames(Lists.newArrayList("region1", "region2"));
        environment.setName("env1");
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertTrue(actualResult.hasError());
        assertEquals(1, actualResult.getErrors().size());
        assertEquals("[region3] region is not enabled in [env1] environment. Enabled regions: [region1,region2]",
                actualResult.getErrors().get(0));
    }

    @Test
    void testValidateShouldWorkWhenStackEnvIsNullButResourcesCouldBeFoundInTheSameWorkspace() {
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
    void testValidateShouldFailWhenProxyCouldNotBeFoundInTheSameWorkspace() {
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
//        when(user.getUserCrn()).thenReturn("aUserCRN");
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertTrue(actualResult.hasError());
        assertEquals(1, actualResult.getErrors().size());
        assertEquals("The specified 'proxy' Proxy config resource couldn't be used: Some reason.",
                actualResult.getErrors().get(0));
    }

    @Test
    void testValidateShouldFailWhenRdsCouldNotBeFoundInTheSameWorkspace() {
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
        assertEquals(2, actualResult.getErrors().size());
        assertTrue(actualResult.getErrors().contains("Stack cannot use 'anrds' Database resource which doesn't exist in the same workspace."));
        assertTrue(actualResult.getErrors().contains("Stack cannot use 'aSecondrds' Database resource which doesn't exist in the same workspace."));
    }

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] autoTlsConfigurations() {
        return new Object[][] {
                // testCaseName                      cmAutoTls   providerAutoTls   hasValidationErrors
                { "CM AutoTls, Provider supports",       true,   true,             false },
                { "CM No AutoTls, Provider supports",   false,   true,             false },
                { "CM AutoTls, No provider support",     true,  false,              true },
                { "CM No AutoTls, No provider support", false,  false,             false },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("autoTlsConfigurations")
    @MockitoSettings(strictness = Strictness.LENIENT)
    void testAutoTlsConfigurations(String testName, boolean cmAutoTls, boolean providerAutoTls, boolean expectedHasErrors) {
        // GIVEN
        Stack stack = getStack();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        cmRequest.setEnableAutoTls(cmAutoTls);
        clusterRequest.setCm(cmRequest);
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        when(platformParameters.isAutoTlsSupported()).thenReturn(providerAutoTls);

        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertEquals(expectedHasErrors, actualResult.hasError());
        if (expectedHasErrors) {
            assertEquals(1, actualResult.getErrors().size());
            assertTrue(actualResult.getErrors().contains("AutoTLS is not supported by 'aws' platform!"));
        }
    }

    @Test
    void testAutoTlsWithFreeIPA() {
        // GIVEN
        Stack stack = getStack();
        ClusterV4Request clusterRequest = new ClusterV4Request();
        ClouderaManagerV4Request cmRequest = new ClouderaManagerV4Request();
        cmRequest.setEnableAutoTls(true);
        clusterRequest.setCm(cmRequest);
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        when(platformParameters.isAutoTlsSupported()).thenReturn(true);
        KerberosConfig kerberosConfig = KerberosConfigBuilder.aKerberosConfig().withType(KerberosType.ACTIVE_DIRECTORY).build();
        when(kerberosConfigService.get(any(), any())).thenReturn(Optional.of(kerberosConfig));
        // WHEN
        ValidationResult actualResult = underTest.validate(clusterRequest, stack, environment);
        // THEN
        assertTrue(actualResult.hasError());
        assertEquals(1, actualResult.getErrors().size());
        assertTrue(actualResult.getErrors().contains("FreeIPA is not available in your environment!"));
    }

    private DetailedEnvironmentResponse getEnvironmentResponse() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");
        environmentResponse.setCredential(new CredentialResponse());
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(Lists.newArrayList("region1"));
        environmentResponse.setRegions(compactRegionResponse);
        return environmentResponse;
    }

    private Stack getStack() {
        Stack stack = new Stack();
        stack.setRegion("region1");
        stack.setCloudPlatform("aws");
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
