package com.sequenceiq.datalake.service.sdx;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.datalake.converter.VmTypeConverter;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.EnvironmentClientService;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformVmtypesResponse;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDefaultTemplateResponse;
import com.sequenceiq.sdx.api.model.SdxRecommendationResponse;
import com.sequenceiq.sdx.api.model.VmTypeMetaJson;
import com.sequenceiq.sdx.api.model.VmTypeResponse;

@Service
public class SdxRecommendationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxRecommendationService.class);

    @Inject
    private CDPConfigService cdpConfigService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private VmTypeConverter vmTypeConverter;

    public SdxDefaultTemplateResponse getDefaultTemplateResponse(SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform) {
        StackV4Request defaultTemplate = getDefaultTemplate(clusterShape, runtimeVersion, cloudPlatform);
        return new SdxDefaultTemplateResponse(defaultTemplate);
    }

    public StackV4Request getDefaultTemplate(SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform) {
        if (clusterShape == null || StringUtils.isAnyBlank(runtimeVersion, cloudPlatform)) {
            throw new BadRequestException("The following query params needs to be filled for this request: clusterShape, runtimeVersion, cloudPlatform");
        }
        StackV4Request defaultTemplate = cdpConfigService.getConfigForKey(new CDPConfigKey(CloudPlatform.valueOf(cloudPlatform), clusterShape, runtimeVersion));
        if (defaultTemplate == null) {
            LOGGER.warn("Can't find template for cloudplatform: {}, shape {}, cdp version: {}", cloudPlatform, clusterShape, runtimeVersion);
            throw notFound("Default template", "cloudPlatform: " + cloudPlatform + ", shape: " + clusterShape +
                    ", runtime version: " + runtimeVersion).get();
        }
        return defaultTemplate;
    }

    public SdxRecommendationResponse getRecommendation(String credentialCrn, SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform,
            String region, String availabilityZone) {
        try {
            StackV4Request defaultTemplate = getDefaultTemplate(clusterShape, runtimeVersion, cloudPlatform);
            List<VmTypeResponse> availableVmTypes = getAvailableVmTypes(credentialCrn, cloudPlatform, region, availabilityZone);
            Map<String, VmTypeResponse> defaultVmTypesByInstanceGroup = getDefaultVmTypesByInstanceGroup(availableVmTypes, defaultTemplate);
            Map<String, List<VmTypeResponse>> availableVmTypesByInstanceGroup = filterAvailableVmTypesBasedOnDefault(
                    availableVmTypes, defaultVmTypesByInstanceGroup);
            LOGGER.debug("Return default template and available vm types for clusterShape: {}, " +
                            "runtimeVersion: {}, cloudPlatform: {}, region: {}, availabilityZone: {}",
                    clusterShape, runtimeVersion, cloudPlatform, region, availabilityZone);
            return new SdxRecommendationResponse(defaultTemplate, availableVmTypesByInstanceGroup);
        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Getting recommendation failed!", e);
            throw new RuntimeException("Getting recommendation failed: " + e.getMessage());
        }
    }

    public void validateVmTypeOverride(DetailedEnvironmentResponse environment, SdxCluster sdxCluster) {
        try {
            LOGGER.debug("Validate vm type override for sdx cluster: {}", sdxCluster.getCrn());
            String cloudPlatform = environment.getCloudPlatform();
            if (shouldValidateVmTypes(sdxCluster, cloudPlatform)) {
                StackV4Request stackV4Request = JsonUtil.readValue(sdxCluster.getStackRequest(), StackV4Request.class);
                StackV4Request defaultTemplate = getDefaultTemplate(sdxCluster.getClusterShape(), sdxCluster.getRuntime(), cloudPlatform);
                String region = environment.getRegions().getNames().stream().findFirst().orElse(null);
                List<VmTypeResponse> availableVmTypes = getAvailableVmTypes(environment.getCredential().getCrn(), cloudPlatform, region, null);
                Map<String, VmTypeResponse> defaultVmTypesByInstanceGroup = getDefaultVmTypesByInstanceGroup(availableVmTypes, defaultTemplate);
                Map<String, List<String>> availableVmTypeNamesByInstanceGroup = filterAvailableVmTypeNamesBasedOnDefault(availableVmTypes,
                        defaultVmTypesByInstanceGroup);

                stackV4Request.getInstanceGroups().forEach(instanceGroup -> {
                    if (!defaultVmTypesByInstanceGroup.containsKey(instanceGroup.getName())) {
                        String message = "Instance group is missing from default template: " + instanceGroup.getName();
                        LOGGER.warn(message);
                        throw new BadRequestException(message);
                    }
                    VmTypeResponse defaultTemplateVmType = defaultVmTypesByInstanceGroup.get(instanceGroup.getName());
                    if (isCustomInstanceTypeProvided(instanceGroup, defaultTemplateVmType.getValue())
                            && !isProvidedInstanceTypeIsAvailable(availableVmTypeNamesByInstanceGroup, instanceGroup)) {
                        String message = String.format("Invalid custom instance type for instance group: %s - %s",
                                instanceGroup.getName(), instanceGroup.getTemplate().getInstanceType());
                        LOGGER.warn(message);
                        throw new BadRequestException(message);
                    }
                });
            }
        } catch (NotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.warn("Validate VM type override failed!", e);
            throw new RuntimeException("Validate VM type override failed: " + e.getMessage());
        }
    }

    private boolean shouldValidateVmTypes(SdxCluster sdxCluster, String cloudPlatform) {
        return !StringUtils.isBlank(sdxCluster.getStackRequest())
                && !SdxClusterShape.CUSTOM.equals(sdxCluster.getClusterShape())
                && !CloudPlatform.YARN.equalsIgnoreCase(cloudPlatform);
    }

    private boolean isProvidedInstanceTypeIsAvailable(Map<String, List<String>> availableVmTypesByInstanceGroup, InstanceGroupV4Request instanceGroup) {
        return availableVmTypesByInstanceGroup.containsKey(instanceGroup.getName())
                && availableVmTypesByInstanceGroup.get(instanceGroup.getName()).contains(instanceGroup.getTemplate().getInstanceType());
    }

    private boolean isCustomInstanceTypeProvided(InstanceGroupV4Request instanceGroup, String defaultTemplateVmType) {
        return !defaultTemplateVmType.equals(instanceGroup.getTemplate().getInstanceType());
    }

    private List<VmTypeResponse> getAvailableVmTypes(String credentialCrn, String cloudPlatform, String region, String availabilityZone) {
        PlatformVmtypesResponse platformVmtypesResponse = environmentClientService.getVmTypesByCredential(credentialCrn, region, cloudPlatform,
                CdpResourceType.DATALAKE, availabilityZone);

        Set<com.sequenceiq.environment.api.v1.platformresource.model.VmTypeResponse> vmTypes = Collections.emptySet();
        if (platformVmtypesResponse.getVmTypes() != null && StringUtils.isNotBlank(availabilityZone)) {
            vmTypes = platformVmtypesResponse.getVmTypes().get(availabilityZone).getVirtualMachines();
        } else if (platformVmtypesResponse.getVmTypes() != null && !platformVmtypesResponse.getVmTypes().isEmpty()) {
            vmTypes = platformVmtypesResponse.getVmTypes().values().iterator().next().getVirtualMachines();
        }
        return vmTypeConverter.convert(vmTypes);
    }

    private Map<String, List<VmTypeResponse>> filterAvailableVmTypesBasedOnDefault(List<VmTypeResponse> availableVmTypes,
            Map<String, VmTypeResponse> defaultVmTypes) {
        Map<String, List<VmTypeResponse>> filteredVmTypesByInstanceGroup = new HashMap<>();
        for (Entry<String, VmTypeResponse> defaultVmTypeByInstanceGroup : defaultVmTypes.entrySet()) {
            List<VmTypeResponse> filteredVmTypes = filterVmTypes(defaultVmTypeByInstanceGroup.getValue(), availableVmTypes);
            filteredVmTypesByInstanceGroup.put(defaultVmTypeByInstanceGroup.getKey(), filteredVmTypes);
        }

        return filteredVmTypesByInstanceGroup;
    }

    private Map<String, List<String>> filterAvailableVmTypeNamesBasedOnDefault(List<VmTypeResponse> availableVmTypes,
            Map<String, VmTypeResponse> defaultVmTypes) {
        Map<String, List<String>> filteredVmTypesByInstanceGroup = new HashMap<>();
        for (Entry<String, VmTypeResponse> defaultVmTypeByInstanceGroup : defaultVmTypes.entrySet()) {
            List<String> filteredVmTypeNames = filterVmTypes(defaultVmTypeByInstanceGroup.getValue(), availableVmTypes)
                    .stream().map(VmTypeResponse::getValue).collect(Collectors.toList());
            filteredVmTypesByInstanceGroup.put(defaultVmTypeByInstanceGroup.getKey(), filteredVmTypeNames);
        }

        return filteredVmTypesByInstanceGroup;
    }

    private Map<String, VmTypeResponse> getDefaultVmTypesByInstanceGroup(List<VmTypeResponse> availableVmTypes, StackV4Request defaultTemplate) {
        Map<String, VmTypeResponse> vmTypesByName = availableVmTypes.stream().collect(Collectors.toMap(VmTypeResponse::getValue, Function.identity()));
        Map<String, String> defaultInstanceTypesByInstanceGroup = defaultTemplate.getInstanceGroups().stream()
                .filter(instanceGroup ->
                        ObjectUtils.allNotNull(instanceGroup.getName(), instanceGroup.getTemplate(), instanceGroup.getTemplate().getInstanceType()))
                .collect(Collectors.toMap(InstanceGroupV4Request::getName, instanceGroup -> instanceGroup.getTemplate().getInstanceType()));

        Map<String, VmTypeResponse> defaultVmTypesByInstanceGroup = new HashMap<>();
        for (Entry<String, String> instanceGroup : defaultInstanceTypesByInstanceGroup.entrySet()) {
            String instanceGroupName = instanceGroup.getKey();
            String instanceType = instanceGroup.getValue();
            if (!vmTypesByName.containsKey(instanceType)) {
                String message = String.format("Missing vm type for default template instance group: %s - %s", instanceGroupName, instanceType);
                LOGGER.warn(message);
                throw new BadRequestException(message);
            }
            defaultVmTypesByInstanceGroup.put(instanceGroupName, vmTypesByName.get(instanceType));
        }
        return defaultVmTypesByInstanceGroup;
    }

    private List<VmTypeResponse> filterVmTypes(VmTypeResponse defaultVmType, List<VmTypeResponse> availableVmTypes) {
        return availableVmTypes.stream().filter(vmType -> filterVmTypeLargerThanDefault(defaultVmType, vmType)).collect(Collectors.toList());
    }

    private boolean filterVmTypeLargerThanDefault(VmTypeResponse defaultVmType, VmTypeResponse vmType) {
        if (!ObjectUtils.allNotNull(defaultVmType, defaultVmType.getVmTypeMetaJson(), vmType, vmType.getVmTypeMetaJson())) {
            return false;
        }
        VmTypeMetaJson defaultVmTypeMetaData = defaultVmType.getVmTypeMetaJson();
        VmTypeMetaJson vmTypeMetaData = vmType.getVmTypeMetaJson();
        if (!ObjectUtils.allNotNull(defaultVmTypeMetaData.getCPU(), defaultVmTypeMetaData.getMemoryInGb(),
                vmTypeMetaData.getCPU(), vmTypeMetaData.getMemoryInGb())) {
            return false;
        }
        return vmTypeMetaData.getCPU() >= defaultVmTypeMetaData.getCPU() && vmTypeMetaData.getMemoryInGb() >= defaultVmTypeMetaData.getMemoryInGb();
    }

}
