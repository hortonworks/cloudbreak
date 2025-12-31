package com.sequenceiq.environment.environment.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.support.response.DataHubPlatformSupportRequirements;
import com.sequenceiq.cloudbreak.cloud.model.CloudDatabaseVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformRequirementsResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.support.DataHubSupportRequirements;
import com.sequenceiq.environment.api.v1.platformresource.model.support.DataLakeSupportRequirements;
import com.sequenceiq.environment.api.v1.platformresource.model.support.DatabaseSupportRequirements;
import com.sequenceiq.environment.api.v1.platformresource.model.support.FreeIpaSupportRequirements;
import com.sequenceiq.environment.environment.service.database.RedBeamsService;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;
import com.sequenceiq.freeipa.api.v1.support.response.FreeIpaPlatformSupportRequirements;
import com.sequenceiq.redbeams.api.endpoint.v4.support.RedBeamsPlatformSupportRequirements;
import com.sequenceiq.sdx.api.model.support.DatalakePlatformSupportRequirements;

@Service
public class EnvironmentRequirementService {

    private static final Map<String, String> X86 = Map.of("architecture", Architecture.X86_64.getName());

    private static final Map<String, String> ARM64 = Map.of("architecture", Architecture.ARM64.getName());

    private final FreeIpaService freeIpaService;

    private final SdxService sdxService;

    private final RedBeamsService redBeamsService;

    private final DatahubService datahubService;

    private final PlatformParameterService platformParameterService;

    public EnvironmentRequirementService(
            FreeIpaService freeIpaService,
            SdxService sdxService,
            RedBeamsService redBeamsService,
            DatahubService datahubService,
            PlatformParameterService platformParameterService) {
        this.freeIpaService = freeIpaService;
        this.sdxService = sdxService;
        this.redBeamsService = redBeamsService;
        this.datahubService = datahubService;
        this.platformParameterService = platformParameterService;
    }

    public PlatformRequirementsResponse getPlatformRequirementsResponse(PlatformResourceRequest platformResourceRequest) {
        String cloudPlatform = platformResourceRequest.getCloudPlatform();
        Set<String> defaultInstanceTypesInRegion = getInstanceTypesInRegion(platformResourceRequest);
        Set<String> freeIpaInstanceTypesInRegion = getFreeIpaInstanceTypesInRegion(platformResourceRequest);
        Set<String> databaseInstanceTypesInRegion = getCloudDatabaseVmTypes(platformResourceRequest);

        return PlatformRequirementsResponse.builder()
                .withDataHub(getDataHubSupportRequirements(defaultInstanceTypesInRegion, datahubService.getInstanceTypesByPlatform(cloudPlatform)))
                .withDataLake(getDataLakeSupportRequirements(defaultInstanceTypesInRegion, sdxService.getInstanceTypesByPlatform(cloudPlatform)))
                .withFreeIpa(getFreeIpaSupportRequirements(freeIpaInstanceTypesInRegion, freeIpaService.internalGetInstanceTypesByPlatform(cloudPlatform)))
                .withDatabase(getDatabaseSupportRequirements(databaseInstanceTypesInRegion, redBeamsService.getInstanceTypesByPlatform(cloudPlatform)))
                .withInstanceTypesInRegion(defaultInstanceTypesInRegion)
                .withDatabaseInstanceTypesInRegion(databaseInstanceTypesInRegion)
                .withFreeIpaInstanceTypesInRegion(freeIpaInstanceTypesInRegion)
                .build();
    }

    private Set<String> getCloudDatabaseVmTypes(PlatformResourceRequest platformResourceRequest) {
        CloudDatabaseVmTypes x86Vms = platformParameterService.getDatabaseVmTypesByCredential(getX86Request(platformResourceRequest));
        CloudDatabaseVmTypes armVms = platformParameterService.getDatabaseVmTypesByCredential(getARMRequest(platformResourceRequest));

        Set<String> vmTypes = new HashSet<>();
        vmTypes.addAll(x86Vms.getCloudDatabaseVmResponses().values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
        vmTypes.addAll(armVms.getCloudDatabaseVmResponses().values().stream().flatMap(Set::stream).collect(Collectors.toSet()));

        return vmTypes.stream()
                .collect(Collectors.toSet());
    }

    private Set<String> getInstanceTypesInRegion(PlatformResourceRequest platformResourceRequest) {
        CloudVmTypes x86Vms = platformParameterService.getVmTypesByCredential(getX86Request(platformResourceRequest));
        CloudVmTypes armVms = platformParameterService.getVmTypesByCredential(getARMRequest(platformResourceRequest));

        Set<VmType> vmTypes = new HashSet<>();
        vmTypes.addAll(x86Vms.getCloudVmResponses().values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
        vmTypes.addAll(armVms.getCloudVmResponses().values().stream().flatMap(Set::stream).collect(Collectors.toSet()));

        return vmTypes.stream()
                .map(VmType::getValue)
                .collect(Collectors.toSet());
    }

    private Set<String> getFreeIpaInstanceTypesInRegion(PlatformResourceRequest platformResourceRequest) {
        CloudVmTypes x86Vms = platformParameterService.getVmTypesByCredential(getFreeIpaX86Request(platformResourceRequest));
        CloudVmTypes armVms = platformParameterService.getVmTypesByCredential(getFreeIpaARMRequest(platformResourceRequest));

        Set<VmType> vmTypes = new HashSet<>();
        vmTypes.addAll(x86Vms.getCloudVmResponses().values().stream().flatMap(Set::stream).collect(Collectors.toSet()));
        vmTypes.addAll(armVms.getCloudVmResponses().values().stream().flatMap(Set::stream).collect(Collectors.toSet()));

        return vmTypes.stream()
                .map(VmType::getValue)
                .collect(Collectors.toSet());
    }

    private PlatformResourceRequest getX86Request(PlatformResourceRequest request) {
        request.setFilters(X86);
        request.setCdpResourceType(CdpResourceType.DEFAULT);
        return request;
    }

    private PlatformResourceRequest getFreeIpaX86Request(PlatformResourceRequest request) {
        request = getX86Request(request);
        request.setCdpResourceType(CdpResourceType.FREEIPA);
        return request;
    }

    private PlatformResourceRequest getARMRequest(PlatformResourceRequest request) {
        request.setFilters(ARM64);
        request.setCdpResourceType(CdpResourceType.DEFAULT);
        return request;
    }

    private PlatformResourceRequest getFreeIpaARMRequest(PlatformResourceRequest request) {
        request = getARMRequest(request);
        request.setCdpResourceType(CdpResourceType.FREEIPA);
        return request;
    }

    private FreeIpaSupportRequirements getFreeIpaSupportRequirements(
            Set<String> instanceTypesInRegion, FreeIpaPlatformSupportRequirements requirements) {
        FreeIpaSupportRequirements freeIpaSupportRequirements = new FreeIpaSupportRequirements();
            freeIpaSupportRequirements.setMissingDefaultX86InstancesTypes(
                    getMissingInstanceTypes(requirements.getDefaultX86InstanceTypeRequirements(), instanceTypesInRegion));
            freeIpaSupportRequirements.setMissingDefaultArmInstanceTypes(
                    getMissingInstanceTypes(requirements.getDefaultArmInstanceTypeRequirements(), instanceTypesInRegion));
        return freeIpaSupportRequirements;
    }

    private DatabaseSupportRequirements getDatabaseSupportRequirements(
            Set<String> instanceTypesInRegion, RedBeamsPlatformSupportRequirements requirements) {
        DatabaseSupportRequirements databaseSupportRequirements = new DatabaseSupportRequirements();
        // in case of custom type we will not able to validate on GCP
        Set<String> collectedX86Instances = filterOutCustomTypes(requirements.getDefaultX86InstanceTypeRequirements());
        Set<String> collectedArmInstances = filterOutCustomTypes(requirements.getDefaultArmInstanceTypeRequirements());

        databaseSupportRequirements.setMissingDefaultX86InstancesTypes(
                getMissingInstanceTypes(collectedX86Instances, instanceTypesInRegion));
        databaseSupportRequirements.setMissingDefaultArmInstanceTypes(
                getMissingInstanceTypes(collectedArmInstances, instanceTypesInRegion));

        return databaseSupportRequirements;
    }

    private Set<String> filterOutCustomTypes(Set<String> requirements) {
        return requirements
                .stream()
                .filter(type -> !type.contains("custom"))
                .collect(Collectors.toSet());
    }

    private DataHubSupportRequirements getDataHubSupportRequirements(
            Set<String> instanceTypesInRegion, DataHubPlatformSupportRequirements requirements) {
        DataHubSupportRequirements dataHubSupportRequirements = new DataHubSupportRequirements();
        dataHubSupportRequirements.setMissingDefaultX86InstancesTypes(
                getMissingInstanceTypes(requirements.getDefaultX86InstanceTypeRequirements(), instanceTypesInRegion));
        dataHubSupportRequirements.setMissingDefaultArmInstanceTypes(
                getMissingInstanceTypes(requirements.getDefaultArmInstanceTypeRequirements(), instanceTypesInRegion));
        return dataHubSupportRequirements;
    }

    private DataLakeSupportRequirements getDataLakeSupportRequirements(
            Set<String> instanceTypesInRegion, DatalakePlatformSupportRequirements requirements) {
        DataLakeSupportRequirements dataLakeSupportRequirements = new DataLakeSupportRequirements();
        dataLakeSupportRequirements.setMissingDefaultX86InstancesTypes(
                getMissingInstanceTypes(requirements.getDefaultX86InstanceTypeRequirements(), instanceTypesInRegion));
        dataLakeSupportRequirements.setMissingDefaultArmInstanceTypes(
                getMissingInstanceTypes(requirements.getDefaultArmInstanceTypeRequirements(), instanceTypesInRegion));
        return dataLakeSupportRequirements;
    }

    private Set<String> getMissingInstanceTypes(Set<String> requiredInstanceTypes, Set<String> instanceTypesInRegion) {
        return requiredInstanceTypes.stream()
                .filter(req -> !instanceTypesInRegion.contains(req))
                .collect(Collectors.toSet());
    }
}
