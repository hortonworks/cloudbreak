package com.sequenceiq.cloudbreak.controller.validation.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

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
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.dto.KerberosConfig.KerberosConfigBuilder;
import com.sequenceiq.cloudbreak.kerberos.KerberosConfigService;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
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
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

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

    @Mock
    private SdxClientService sdxClientService;

    @InjectMocks
    private ClusterCreationEnvironmentValidator underTest;

    @BeforeEach
    void setUp() {
        lenient().when(cloudPlatformConnectors.getDefault(any())).thenReturn(connector);
        lenient().when(cloudPlatformConnectors.get(any(), any())).thenReturn(connector);
        lenient().when(connector.parameters()).thenReturn(platformParameters);
        lenient().when(connector.displayNameToRegion(any())).thenReturn("region1");
        lenient().when(platformParameters.isAutoTlsSupported()).thenReturn(true);
        ReflectionTestUtils.setField(underTest, "validateDatalakeAvailability", true);
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsValidAndEnvironmentsResourcesAreNotGiven() {
        // GIVEN
        Stack stack = getStack();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeFailedWhenSDXIsNotAvailableAndDistroxRequestInTheEnvironment() {
        // GIVEN
        Stack stack = getStack();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList());
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertTrue(actualResult.hasError());
        assertEquals(1, actualResult.getErrors().size());
        assertEquals("Data Lake is not available in your environment!",
                actualResult.getErrors().get(0));
    }

    @Test
    void testValidateShouldBeFailedWhenSDXIsNotAvailableAndNotDistroxRequestInTheEnvironment() {
        // GIVEN
        Stack stack = getStack();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, false, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsRegionNameAndEnvironmentsRegionsAreDisplayNames() {
        // GIVEN
        String westUs2RegionName = "westus2";
        String westUs2RegionDisplayName = "West US 2";
        Stack stack = getStack();
        stack.setRegion(westUs2RegionName);
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.getNames().add(westUs2RegionDisplayName);
        regions.getNames().add("West US");
        environment.setRegions(regions);
        when(connector.displayNameToRegion(any())).thenReturn(westUs2RegionName);
        when(connector.regionToDisplayName(any())).thenReturn(westUs2RegionDisplayName);
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsRegionDisplayNameAndEnvironmentsRegionsAreDisplayNames() {
        // GIVEN
        String westUs2RegionName = "westus2";
        String westUs2RegionDisplayName = "West US 2";
        Stack stack = getStack();
        stack.setRegion(westUs2RegionDisplayName);
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.getNames().add(westUs2RegionDisplayName);
        regions.getNames().add("West US");
        environment.setRegions(regions);
        when(connector.displayNameToRegion(any())).thenReturn(westUs2RegionName);
        when(connector.regionToDisplayName(any())).thenReturn(westUs2RegionDisplayName);
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsRegionNameAndEnvironmentsRegionsAreRegionNames() {
        // GIVEN
        String westUs2RegionName = "westus2";
        String westUs2RegionDisplayName = "West US 2";
        Stack stack = getStack();
        stack.setRegion(westUs2RegionName);
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.getNames().add(westUs2RegionName);
        regions.getNames().add("westus");
        environment.setRegions(regions);
        when(connector.displayNameToRegion(any())).thenReturn(westUs2RegionName);
        when(connector.regionToDisplayName(any())).thenReturn(westUs2RegionDisplayName);
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenStackRegionIsRegionDisplayNameAndEnvironmentsRegionsAreRegionNames() {
        // GIVEN
        String westUs2RegionName = "westus2";
        String westUs2RegionDisplayName = "West US 2";
        Stack stack = getStack();
        stack.setRegion(westUs2RegionDisplayName);
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.getNames().add(westUs2RegionName);
        regions.getNames().add("westus");
        environment.setRegions(regions);
        when(connector.displayNameToRegion(any())).thenReturn(westUs2RegionName);
        when(connector.regionToDisplayName(any())).thenReturn(westUs2RegionDisplayName);
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenNoEnvironmentProvided() {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironmentCrn(null);
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeSuccessWhenResourcesAreInTheSameEnvironmentOrGlobals() {
        // GIVEN
        Stack stack = getStack();
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldBeFailedWhenStackRegionIsInvalidAndEnvironmentsResourcesAreNotInGoodEnvironment() {
        // GIVEN
        Stack stack = getStack();
        stack.setRegion("region3");
        when(connector.displayNameToRegion(any())).thenReturn("region666");
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        environment.getRegions().setNames(Lists.newArrayList("region1", "region2"));
        environment.setName("env1");
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
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
        DetailedEnvironmentResponse environment = getEnvironmentResponse();
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validate(stack, environment, true, validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertFalse(actualResult.hasError());
    }

    @Test
    void testValidateShouldFailWhenProxyCouldNotBeFoundInTheSameWorkspace() {
        // GIVEN
        Stack stack = getStack();
        stack.setEnvironmentCrn(null);
        when(proxyConfigDtoService.getByCrn(anyString())).thenThrow(new CloudbreakServiceException("Some reason"));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validateProxyConfig("proxy", validationBuilder);
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertTrue(actualResult.hasError());
        assertEquals(1, actualResult.getErrors().size());
        assertEquals("The specified 'proxy' Proxy config resource couldn't be used: Some reason.",
                actualResult.getErrors().get(0));
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
        when(sdxClientService.getByEnvironmentCrn(any())).thenReturn(Arrays.asList(new SdxClusterResponse()));
        ValidationResult.ValidationResultBuilder validationBuilder = ValidationResult.builder();
        // WHEN
        underTest.validateAutoTls(clusterRequest, stack, validationBuilder, environment.getParentEnvironmentCloudPlatform());
        // THEN
        ValidationResult actualResult = validationBuilder.build();
        assertEquals(expectedHasErrors, actualResult.hasError());
        if (expectedHasErrors) {
            assertEquals(1, actualResult.getErrors().size());
            assertTrue(actualResult.getErrors().contains("AutoTLS is not supported by 'aws' platform!"));
        }
    }

    @Test
    void testAutoTlsWithFreeIpa() {
        // GIVEN
        Stack stack = getStack();
        KerberosConfig kerberosConfig = KerberosConfigBuilder.aKerberosConfig().withType(KerberosType.ACTIVE_DIRECTORY).build();
        when(kerberosConfigService.get(any(), any())).thenReturn(Optional.of(kerberosConfig));
        // WHEN
        boolean result = underTest.hasFreeIpaKerberosConfig(stack);
        // THEN
        assertFalse(result);
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
}
