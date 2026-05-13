package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.sdx.api.model.SdxClusterShape.MICRO_DUTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.InstanceTemplateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDisksResponse;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;
import com.sequenceiq.sdx.api.model.VmTypeMetaJson;
import com.sequenceiq.sdx.api.model.VmTypeResponse;
import com.sequenceiq.sdx.api.model.VolumeParameterConfigResponse;

@ExtendWith(MockitoExtension.class)
class SdxInstanceServiceTest {
    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxRecommendationService sdxRecommendationService;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private SdxInstanceService underTest;

    @Test
    void testOverrideDefaultInstanceTypeWithCustomInstanceGroup() throws Exception {
        final String runtime = "7.2.12";
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        List<SdxInstanceGroupRequest> customInstanceGroups = List.of(withInstanceGroup("master", "verylarge"),
                withInstanceGroup("idbroker", "notverylarge"));
        StackV4Request stackV4Request = JsonUtil.readValue(microDutyJson, StackV4Request.class);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("GCP");
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn("crn");
        environmentResponse.setCredential(credentialResponse);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(List.of("region1", "region2"));
        environmentResponse.setRegions(compactRegionResponse);

        when(sdxRecommendationService.getAvailableVmTypes("crn", "GCP", "region1", null, Architecture.ALL_ARCHITECTURE)).thenReturn(List.of());

        underTest.overrideDefaultInstanceType(environmentResponse, stackV4Request, customInstanceGroups, Collections.emptyList(),
                Collections.emptyList(), MICRO_DUTY);

        Optional<InstanceGroupV4Request> masterGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(masterGroup.isPresent());
        assertEquals("verylarge", masterGroup.get().getTemplate().getInstanceType());
        Optional<InstanceGroupV4Request> idbrokerGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "idbroker".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(idbrokerGroup.isPresent());
        assertEquals("notverylarge", idbrokerGroup.get().getTemplate().getInstanceType());
    }

    @Test
    void testOverrideDefaultInstanceTypeWithDifferentVolumeType() throws Exception {
        final String runtime = "7.2.14";
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        List<SdxInstanceGroupRequest> customInstanceGroups = List.of(withInstanceGroup("master", "newinstancetype"));
        StackV4Request stackV4Request = JsonUtil.readValue(microDutyJson, StackV4Request.class);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn("crn");
        environmentResponse.setCredential(credentialResponse);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(List.of("us-east-1"));
        environmentResponse.setRegions(compactRegionResponse);

        PlatformDisksResponse platformDisksResponse = new PlatformDisksResponse();
        Map<String, Map<String, String>> diskMappings = new HashMap<>();
        Map<String, String> awsDiskMappings = new HashMap<>();
        awsDiskMappings.put("standard", "MAGNETIC");
        diskMappings.put("AWS", awsDiskMappings);
        platformDisksResponse.setDiskMappings(diskMappings);
        when(environmentService.getDiskTypes()).thenReturn(platformDisksResponse);

        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(Map.of("DefaultDiskType", "ssd"));
        List<VolumeParameterConfigResponse> configs = List.of(new VolumeParameterConfigResponse("SSD", 10, 4095, 1, 8));
        vmTypeMetaJson.setConfigs(configs);
        VmTypeResponse vmTypeResponse = new VmTypeResponse("newinstancetype", vmTypeMetaJson);

        when(sdxRecommendationService.getAvailableVmTypes("crn", "AWS", "us-east-1", null, Architecture.ALL_ARCHITECTURE))
                .thenReturn(List.of(vmTypeResponse));

        underTest.overrideDefaultInstanceType(environmentResponse, stackV4Request, customInstanceGroups, Collections.emptyList(),
                Collections.emptyList(), MICRO_DUTY);

        Optional<InstanceGroupV4Request> masterGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(masterGroup.isPresent());
        assertEquals("newinstancetype", masterGroup.get().getTemplate().getInstanceType());
        assertEquals("ssd", masterGroup.get().getTemplate().getAttachedVolumes().stream().findFirst().get().getType());
    }

    @Test
    void testOverrideDefaultInstanceTypeWithResize() throws Exception {
        final String runtime = "7.2.14";
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        StackV4Request stackV4Request = JsonUtil.readValue(microDutyJson, StackV4Request.class);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn("crn");
        environmentResponse.setCredential(credentialResponse);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(List.of("us-east-1"));
        environmentResponse.setRegions(compactRegionResponse);

        List<InstanceGroupV4Request> originalInstanceGroups = List.of(withOriginalInstanceGroup("master", "oldinstancetype"));

        InstanceTemplateV4Response currentTemplate = new InstanceTemplateV4Response();
        currentTemplate.setInstanceType("newinstancetype");
        VolumeV4Response currentVolume = new VolumeV4Response();
        currentVolume.setType("standard");
        currentVolume.setSize(100);
        currentTemplate.setAttachedVolumes(Set.of(currentVolume));

        InstanceGroupV4Response currentInstanceGroup = new InstanceGroupV4Response();
        currentInstanceGroup.setName("master");
        currentInstanceGroup.setTemplate(currentTemplate);
        List<InstanceGroupV4Response> currentInstanceGroups = List.of(currentInstanceGroup);

        PlatformDisksResponse platformDisksResponse = new PlatformDisksResponse();
        Map<String, Map<String, String>> diskMappings = new HashMap<>();
        Map<String, String> awsDiskMappings = new HashMap<>();
        awsDiskMappings.put("standard", "MAGNETIC");
        diskMappings.put("AWS", awsDiskMappings);
        platformDisksResponse.setDiskMappings(diskMappings);
        when(environmentService.getDiskTypes()).thenReturn(platformDisksResponse);

        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(Map.of("DefaultDiskType", "ssd"));
        List<VolumeParameterConfigResponse> configs = List.of(new VolumeParameterConfigResponse("SSD", 10, 4095, 1, 8));
        vmTypeMetaJson.setConfigs(configs);
        VmTypeResponse vmTypeResponse = new VmTypeResponse("newinstancetype", vmTypeMetaJson);

        when(sdxRecommendationService.getAvailableVmTypes("crn", "AWS", "us-east-1", null, Architecture.ALL_ARCHITECTURE)).thenReturn(List.of(vmTypeResponse));

        underTest.overrideDefaultInstanceType(environmentResponse, stackV4Request, Collections.emptyList(),
                originalInstanceGroups, currentInstanceGroups, MICRO_DUTY);

        Optional<InstanceGroupV4Request> masterGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(masterGroup.isPresent());
        assertEquals("newinstancetype", masterGroup.get().getTemplate().getInstanceType());
        assertEquals("ssd", masterGroup.get().getTemplate().getAttachedVolumes().stream().findFirst().get().getType());
    }

    @Test
    void testOverrideDefaultInstanceTypeWithResizeAndOriginalVolumeDifferent() throws Exception {
        final String runtime = "7.2.14";
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        StackV4Request stackV4Request = JsonUtil.readValue(microDutyJson, StackV4Request.class);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn("crn");
        environmentResponse.setCredential(credentialResponse);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(List.of("us-east-1"));
        environmentResponse.setRegions(compactRegionResponse);

        InstanceTemplateV4Request originalTemplate = new InstanceTemplateV4Request();
        originalTemplate.setInstanceType("oldinstancetype");
        InstanceGroupV4Request originalInstanceGroup = new InstanceGroupV4Request();
        originalInstanceGroup.setName("master");
        originalInstanceGroup.setTemplate(originalTemplate);
        List<InstanceGroupV4Request> originalInstanceGroups = List.of(originalInstanceGroup);

        InstanceTemplateV4Response currentTemplate = new InstanceTemplateV4Response();
        currentTemplate.setInstanceType("newinstancetype");
        VolumeV4Response currentVolume = new VolumeV4Response();
        currentVolume.setType("gp2");
        currentVolume.setSize(100);
        currentTemplate.setAttachedVolumes(Set.of(currentVolume));

        InstanceGroupV4Response currentInstanceGroup = new InstanceGroupV4Response();
        currentInstanceGroup.setName("master");
        currentInstanceGroup.setTemplate(currentTemplate);
        List<InstanceGroupV4Response> currentInstanceGroups = List.of(currentInstanceGroup);

        PlatformDisksResponse platformDisksResponse = new PlatformDisksResponse();
        Map<String, Map<String, String>> diskMappings = new HashMap<>();
        Map<String, String> awsDiskMappings = new HashMap<>();
        awsDiskMappings.put("gp2", "SSD");
        awsDiskMappings.put("standard", "MAGNETIC");
        diskMappings.put("AWS", awsDiskMappings);
        platformDisksResponse.setDiskMappings(diskMappings);
        when(environmentService.getDiskTypes()).thenReturn(platformDisksResponse);

        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(Map.of("DefaultDiskType", "standard"));
        List<VolumeParameterConfigResponse> configs = List.of(new VolumeParameterConfigResponse("MAGNETIC", 10, 4095, 1, 8));
        vmTypeMetaJson.setConfigs(configs);
        VmTypeResponse vmTypeResponse = new VmTypeResponse("newinstancetype", vmTypeMetaJson);

        when(sdxRecommendationService.getAvailableVmTypes("crn", "AWS", "us-east-1", null, Architecture.ALL_ARCHITECTURE))
                .thenReturn(List.of(vmTypeResponse));

        underTest.overrideDefaultInstanceType(environmentResponse, stackV4Request, Collections.emptyList(),
                originalInstanceGroups, currentInstanceGroups, MICRO_DUTY);

        Optional<InstanceGroupV4Request> masterGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(masterGroup.isPresent());
        assertEquals("newinstancetype", masterGroup.get().getTemplate().getInstanceType());
        assertEquals("standard", masterGroup.get().getTemplate().getAttachedVolumes().stream().findFirst().get().getType());
    }

    @Test
    void testOverrideDefaultInstanceTypeWithSameVolumeType() throws Exception {
        final String runtime = "7.2.14";
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        List<SdxInstanceGroupRequest> customInstanceGroups = List.of(withInstanceGroup("master", "newinstancetype"));
        StackV4Request stackV4Request = JsonUtil.readValue(microDutyJson, StackV4Request.class);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn("crn");
        environmentResponse.setCredential(credentialResponse);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(List.of("us-east-1"));
        environmentResponse.setRegions(compactRegionResponse);

        PlatformDisksResponse platformDisksResponse = new PlatformDisksResponse();
        Map<String, Map<String, String>> diskMappings = new HashMap<>();
        Map<String, String> awsDiskMappings = new HashMap<>();
        awsDiskMappings.put("gp2", "SSD");
        diskMappings.put("AWS", awsDiskMappings);
        platformDisksResponse.setDiskMappings(diskMappings);
        when(environmentService.getDiskTypes()).thenReturn(platformDisksResponse);

        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(Map.of());
        List<VolumeParameterConfigResponse> configs = List.of(
                new VolumeParameterConfigResponse("SSD", 10, 4095, 1, 8)
        );
        vmTypeMetaJson.setConfigs(configs);
        VmTypeResponse vmTypeResponse = new VmTypeResponse("newinstancetype", vmTypeMetaJson);

        when(sdxRecommendationService.getAvailableVmTypes("crn", "AWS", "us-east-1", null, Architecture.ALL_ARCHITECTURE))
                .thenReturn(List.of(vmTypeResponse));

        underTest.overrideDefaultInstanceType(environmentResponse, stackV4Request, customInstanceGroups, Collections.emptyList(),
                Collections.emptyList(), MICRO_DUTY);

        Optional<InstanceGroupV4Request> masterGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(masterGroup.isPresent());
        assertEquals("newinstancetype", masterGroup.get().getTemplate().getInstanceType());
        String originalVolumeType = stackV4Request.getInstanceGroups()
                .stream()
                .filter(ig -> "master".equals(ig.getName()))
                .findFirst()
                .map(ig -> ig.getTemplate().getAttachedVolumes())
                .flatMap(volumes -> volumes.stream().findFirst())
                .map(VolumeV4Request::getType)
                .orElse("gp2");
        String resultVolumeType = masterGroup.get().getTemplate().getAttachedVolumes().stream().findFirst().get().getType();
        assertEquals(originalVolumeType, resultVolumeType, "Volume type should not be changed when compatible");
    }

    @Test
    void testOverrideDefaultInstanceTypeWithUnsupportedVolumeAndNoDefault() throws Exception {
        final String runtime = "7.2.14";
        String microDutyJson = FileReaderUtils.readFileFromClasspath("/duties/" + runtime + "/aws/micro_duty.json");
        List<SdxInstanceGroupRequest> customInstanceGroups = List.of(withInstanceGroup("master", "newinstancetype"));
        StackV4Request stackV4Request = JsonUtil.readValue(microDutyJson, StackV4Request.class);
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCloudPlatform("AWS");
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setCrn("crn");
        environmentResponse.setCredential(credentialResponse);
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        compactRegionResponse.setNames(List.of("us-east-1"));
        environmentResponse.setRegions(compactRegionResponse);

        PlatformDisksResponse platformDisksResponse = new PlatformDisksResponse();
        Map<String, Map<String, String>> diskMappings = new HashMap<>();
        Map<String, String> awsDiskMappings = new HashMap<>();
        awsDiskMappings.put("gp2", "SSD");
        awsDiskMappings.put("standard", "MAGNETIC");
        diskMappings.put("AWS", awsDiskMappings);
        platformDisksResponse.setDiskMappings(diskMappings);
        when(environmentService.getDiskTypes()).thenReturn(platformDisksResponse);

        VmTypeMetaJson vmTypeMetaJson = new VmTypeMetaJson();
        vmTypeMetaJson.setProperties(Map.of());
        List<VolumeParameterConfigResponse> configs = List.of(new VolumeParameterConfigResponse("MAGNETIC", 10, 4095, 1, 8));
        vmTypeMetaJson.setConfigs(configs);
        VmTypeResponse vmTypeResponse = new VmTypeResponse("newinstancetype", vmTypeMetaJson);

        when(sdxRecommendationService.getAvailableVmTypes("crn", "AWS", "us-east-1", null, Architecture.ALL_ARCHITECTURE))
                .thenReturn(List.of(vmTypeResponse));

        underTest.overrideDefaultInstanceType(environmentResponse, stackV4Request, customInstanceGroups, Collections.emptyList(),
                Collections.emptyList(), MICRO_DUTY);

        Optional<InstanceGroupV4Request> masterGroup = stackV4Request.getInstanceGroups()
                .stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny();
        assertTrue(masterGroup.isPresent());
        assertEquals("newinstancetype", masterGroup.get().getTemplate().getInstanceType());
        String volumeType = masterGroup.get().getTemplate().getAttachedVolumes().stream().findFirst().get().getType();
        assertNotNull(volumeType, "Volume type should exist");
    }

    private SdxInstanceGroupRequest withInstanceGroup(String name, String instanceType) {
        SdxInstanceGroupRequest masterInstanceGroup = new SdxInstanceGroupRequest();
        masterInstanceGroup.setName(name);
        masterInstanceGroup.setInstanceType(instanceType);
        return masterInstanceGroup;
    }

    private InstanceGroupV4Request withOriginalInstanceGroup(String name, String instanceType) {
        InstanceGroupV4Request instanceGroupV4Request = new InstanceGroupV4Request();
        instanceGroupV4Request.setName(name);
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setInstanceType(instanceType);
        instanceGroupV4Request.setTemplate(template);
        return instanceGroupV4Request;
    }
}
