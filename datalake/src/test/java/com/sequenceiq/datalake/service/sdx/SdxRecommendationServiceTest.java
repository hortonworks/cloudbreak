package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.sdx.api.model.SdxClusterShape.CUSTOM;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.LIGHT_DUTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.converter.VmTypeConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VirtualMachinesResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeMetaJson;
import com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDefaultTemplateResponse;
import com.sequenceiq.sdx.api.model.SdxRecommendationResponse;

@ExtendWith(MockitoExtension.class)
class SdxRecommendationServiceTest {

    @Mock
    private CDPConfigService cdpConfigService;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Spy
    private VmTypeConverter vmTypeConverter;

    @InjectMocks
    private SdxRecommendationService underTest;

    @Test
    public void testGetDefaultTemplateWhenMissingRequiredParameters() {
        assertThrows(BadRequestException.class, () -> underTest.getDefaultTemplateResponse(null,  "7.2.14", "AWS"));
        assertThrows(BadRequestException.class, () -> underTest.getDefaultTemplateResponse(LIGHT_DUTY,  null, "AWS"));
        assertThrows(BadRequestException.class, () -> underTest.getDefaultTemplateResponse(LIGHT_DUTY,  "7.2.14", null));
    }

    @Test
    public void testGetDefaultTemplateWhenMissingTemplateForParameters() {
        when(cdpConfigService.getConfigForKey(any())).thenReturn(null);
        assertThrows(NotFoundException.class, () -> underTest.getDefaultTemplateResponse(LIGHT_DUTY,  "7.2.14", "AWS"));
    }

    @Test
    public void testGetDefaultTemplate() {
        StackV4Request defaultTemplate = createStackRequest();
        when(cdpConfigService.getConfigForKey(any())).thenReturn(defaultTemplate);

        SdxDefaultTemplateResponse response = underTest.getDefaultTemplateResponse(LIGHT_DUTY, "7.2.14", "AWS");
        assertEquals(defaultTemplate, response.getTemplate());
    }

    @Test
    public void testGetRecommendation() {
        when(cdpConfigService.getConfigForKey(any())).thenReturn(createStackRequest());
        when(environmentClientService.getVmTypesByCredential(anyString(), anyString(), anyString(), eq(CdpResourceType.DATALAKE), any()))
                .thenReturn(createPlatformVmtypesResponse());

        SdxRecommendationResponse recommendation = underTest.getRecommendation("cred", LIGHT_DUTY, "7.2.14", "AWS", "ec-central-1", null);

        StackV4Request defaultTemplate = recommendation.getTemplate();
        assertNotNull(defaultTemplate);
        assertThat(defaultTemplate.getInstanceGroups())
                .hasSize(2)
                .extracting(ig -> ig.getName(), ig -> ig.getTemplate().getInstanceType())
                .containsExactlyInAnyOrder(
                        tuple("master", "large"),
                        tuple("idbroker", "medium"));

        Map<String, List<com.sequenceiq.sdx.api.model.VmTypeResponse>> availableVmTypesByInstanceGroup = recommendation.getAvailableVmTypesByInstanceGroup();
        assertThat(availableVmTypesByInstanceGroup).containsOnlyKeys("master", "idbroker");
        assertThat(availableVmTypesByInstanceGroup.get("master")).extracting(vmType -> vmType.getValue())
                .containsExactlyInAnyOrder("large");
        assertThat(availableVmTypesByInstanceGroup.get("idbroker")).extracting(vmType -> vmType.getValue())
                .containsExactlyInAnyOrder("large", "medium", "mediumv2");
    }

    @Test
    public void testGetRecommendationFailed() {
        when(cdpConfigService.getConfigForKey(any())).thenReturn(createStackRequest());
        when(environmentClientService.getVmTypesByCredential(anyString(), anyString(), anyString(), eq(CdpResourceType.DATALAKE), any()))
                .thenThrow(new javax.ws.rs.BadRequestException("The provided client secret keys for app 1234."));

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.getRecommendation("cred", LIGHT_DUTY, "7.2.14", "AWS", "ec-central-1", null));

        assertEquals("The provided client secret keys for app 1234. Please update your CDP Credential with the newly generated application key value!",
                badRequestException.getMessage());
    }

    @Test
    public void validateVmTypeOverrideWhenInstanceGroupIsMissingFromDefaultTemplate() {
        when(cdpConfigService.getConfigForKey(any())).thenReturn(createStackRequest());
        when(environmentClientService.getVmTypesByCredential(anyString(), anyString(), anyString(), eq(CdpResourceType.DATALAKE), any()))
                .thenReturn(createPlatformVmtypesResponse());
        StackV4Request stackRequest = createStackRequest();
        stackRequest.getInstanceGroups().get(0).setName("unknown");
        SdxCluster sdxCluster = createSdxCluster(stackRequest, LIGHT_DUTY);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateVmTypeOverride(createEnvironment("AWS"), sdxCluster));

        assertEquals("Instance group is missing from default template: unknown", badRequestException.getMessage());
    }

    @Test
    public void validateVmTypeOverrideWhenDefaultVmTypeIsMissingFromAvailableVmTypes() {
        StackV4Request defaultTemplate = createStackRequest();
        defaultTemplate.getInstanceGroups().get(0).getTemplate().setInstanceType("unknown");
        when(cdpConfigService.getConfigForKey(any())).thenReturn(defaultTemplate);
        when(environmentClientService.getVmTypesByCredential(anyString(), anyString(), anyString(), eq(CdpResourceType.DATALAKE), any()))
                .thenReturn(createPlatformVmtypesResponse());
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateVmTypeOverride(createEnvironment("AWS"), createSdxCluster(createStackRequest(), LIGHT_DUTY)));

        assertEquals("Missing vm type for default template instance group: master - unknown", badRequestException.getMessage());
    }

    @Test
    public void validateVmTypeOverrideSkippedForYCoud() {
        assertDoesNotThrow(() -> underTest.validateVmTypeOverride(createEnvironment("YARN"), createSdxCluster(createStackRequest(), LIGHT_DUTY)));

        verify(cdpConfigService, never()).getConfigForKey(any());
        verify(environmentClientService, never()).getVmTypesByCredential(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    public void validateVmTypeOverrideWhenNewVmTypeIsSmaller() {
        when(cdpConfigService.getConfigForKey(any())).thenReturn(createStackRequest());
        when(environmentClientService.getVmTypesByCredential(anyString(), anyString(), anyString(), eq(CdpResourceType.DATALAKE), any()))
                .thenReturn(createPlatformVmtypesResponse());
        StackV4Request stackRequest = createStackRequest();
        stackRequest.getInstanceGroups().stream().forEach(ig -> ig.getTemplate().setInstanceType("small"));
        SdxCluster sdxCluster = createSdxCluster(stackRequest, LIGHT_DUTY);
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateVmTypeOverride(createEnvironment("AWS"), sdxCluster));

        assertEquals("Invalid custom instance type for instance group: master - small", badRequestException.getMessage());
    }

    @Test
    public void validateVmTypeOverrideWhenOverrideIsValid() {
        when(cdpConfigService.getConfigForKey(any())).thenReturn(createStackRequest());
        when(environmentClientService.getVmTypesByCredential(anyString(), anyString(), anyString(), eq(CdpResourceType.DATALAKE), any()))
                .thenReturn(createPlatformVmtypesResponse());
        assertDoesNotThrow(() -> underTest.validateVmTypeOverride(createEnvironment("AWS"), createSdxCluster(createStackRequest(), LIGHT_DUTY)));
    }

    @Test
    public void validateVmTypeOverrideWhenSdxClusterShapeIsCustom() {
        assertDoesNotThrow(() -> underTest.validateVmTypeOverride(createEnvironment("AWS"), createSdxCluster(createStackRequest(), CUSTOM)));
        verify(cdpConfigService, never()).getConfigForKey(any());
        verify(environmentClientService, never()).getVmTypesByCredential(anyString(), anyString(), anyString(), any(), anyString());
    }

    private SdxCluster createSdxCluster(StackV4Request stackV4Request, SdxClusterShape clusterShape) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterShape(clusterShape);
        sdxCluster.setRuntime("7.2.14");
        sdxCluster.setStackRequest(stackV4Request);
        return sdxCluster;
    }

    private DetailedEnvironmentResponse createEnvironment(String cloudPlatform) {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        CredentialResponse credential = new CredentialResponse();
        credential.setCrn("cred");
        environment.setCredential(credential);
        CompactRegionResponse regions = new CompactRegionResponse();
        regions.setNames(List.of("eu-central-1"));
        environment.setRegions(regions);
        environment.setCloudPlatform(cloudPlatform);
        return environment;
    }

    private PlatformVmtypesResponse createPlatformVmtypesResponse() {
        Map<String, VirtualMachinesResponse> vmTypes = new HashMap<>();
        VirtualMachinesResponse virtualMachinesResponse = new VirtualMachinesResponse();
        Set<VmTypeResponse> virtualMachines = new HashSet<>();
        virtualMachines.add(new VmTypeResponse("large", createVmTypeMetaJson(10, 1000.0F)));
        virtualMachines.add(new VmTypeResponse("medium", createVmTypeMetaJson(8, 800.0F)));
        virtualMachines.add(new VmTypeResponse("mediumv2", createVmTypeMetaJson(8, 1000.0F)));
        virtualMachines.add(new VmTypeResponse("small", createVmTypeMetaJson(2, 200.0F)));
        virtualMachinesResponse.setVirtualMachines(virtualMachines);
        vmTypes.put("eu-central-1", virtualMachinesResponse);
        return new PlatformVmtypesResponse(vmTypes);
    }

    private VmTypeMetaJson createVmTypeMetaJson(Integer cpu, Float memory) {
        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        Map<String, Object> properties = new HashMap<>();
        properties.put("Cpu", cpu);
        properties.put("Memory", memory);
        vmTypeMetaJson.setProperties(properties);
        return vmTypeMetaJson;
    }

    private StackV4Request createStackRequest() {
        StackV4Request defaultTemplate = new StackV4Request();
        defaultTemplate.getInstanceGroups().add(createInstanceGroup("master", "large"));
        defaultTemplate.getInstanceGroups().add(createInstanceGroup("idbroker", "medium"));
        return defaultTemplate;
    }

    private InstanceGroupV4Request createInstanceGroup(String name, String instanceType) {
        InstanceGroupV4Request instanceGroup = new InstanceGroupV4Request();
        instanceGroup.setName(name);
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setInstanceType(instanceType);
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }

}