package com.sequenceiq.cloudbreak.service.stack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.controller.validation.blueprint.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.cluster.flow.blueprint.BlueprintProcessor;

@Service
public class CloudResourceAdvisor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceAdvisor.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    public Map<String, VmType> createForBlueprint(String blueprint, PlatformResourceRequest resourceRequest, IdentityUser cbUser) {
        String cloudPlatform = resourceRequest.getCloudPlatform();
        String region = resourceRequest.getRegion();
        String availabilityZone = resourceRequest.getAvailabilityZone();
        Map<String, VmType> vmTypesByHostGroup = new HashMap<>();
        Map<String, Boolean> hostGroupContainsMasterComp = new HashMap<>();
        LOGGER.info("Advising resources for blueprint: %s, provider: %s and region: %s.", blueprint, cloudPlatform, region);

        String blueprintText = getBlueprint(blueprint, cbUser).getBlueprintText();
        Map<String, Set<String>> componentsByHostGroup = blueprintProcessor.getComponentsByHostGroup(blueprintText);
        componentsByHostGroup
                .forEach((hGName, components) -> hostGroupContainsMasterComp.put(hGName, isThereMasterComponents(components)));

        PlatformVirtualMachines vmTypes = cloudParameterService.getVmtypes(cloudPlatform, false);
        VmType defaultVmType = getDefaultVmType(cloudPlatform, availabilityZone, vmTypes);
        componentsByHostGroup.keySet().forEach(comp -> vmTypesByHostGroup.put(comp, defaultVmType));

        VmRecommendations recommendations = cloudParameterService.getRecommendation(cloudPlatform);
        if (recommendations != null) {
            Collection<VmType> availableVmTypes = vmTypes.getVmTypesPerZones().get(Platform.platform(cloudPlatform))
                    .get(AvailabilityZone.availabilityZone(availabilityZone));

            Map<String, VmType> masterVmTypes = getVmTypesForComponentType(true, recommendations.getMaster(), hostGroupContainsMasterComp, availableVmTypes);
            vmTypesByHostGroup.putAll(masterVmTypes);
            Map<String, VmType> workerVmTypes = getVmTypesForComponentType(false, recommendations.getWorker(), hostGroupContainsMasterComp, availableVmTypes);
            vmTypesByHostGroup.putAll(workerVmTypes);
        }
        return vmTypesByHostGroup;
    }

    private Blueprint getBlueprint(String blueprint, IdentityUser cbUser) {
        Blueprint bp;
        try {
            Long bpId = Long.valueOf(blueprint);
            LOGGER.debug("Try to get blueprint by id: %d.", bpId);
            bp = blueprintService.get(bpId);
        } catch (NumberFormatException e) {
            LOGGER.debug("Try to get blueprint by name: %d.", blueprint);
            bp = blueprintService.getByName(blueprint, cbUser);
        }
        return bp;
    }

    private boolean isThereMasterComponents(Set<String> components) {
        return components.stream()
                .anyMatch(component -> stackServiceComponentDescs.get(component) != null && stackServiceComponentDescs.get(component).isMaster());
    }

    private VmType getDefaultVmType(String cloudPlatform, String availabilityZone, PlatformVirtualMachines vmtypes) {
        VmType vmType;
        Map<AvailabilityZone, VmType> vmTypesByAZones = vmtypes.getDefaultVmTypePerZones().get(Platform.platform(cloudPlatform));
        if (vmTypesByAZones != null) {
            vmType = vmTypesByAZones.get(AvailabilityZone.availabilityZone(availabilityZone));
            if (vmType == null || Strings.isNullOrEmpty(vmType.value())) {
                throw new NotFoundException(String.format("Could not determine VM type for availability zone: '%s'.", availabilityZone));
            }
        } else {
            throw new NotFoundException(String.format("Could not determine VM type for cloud platform: '%s'.", cloudPlatform));
        }
        return vmType;
    }

    private Map<String, VmType> getVmTypesForComponentType(boolean containsMasterComponent,
            VmRecommendation recommendation,
            Map<String, Boolean> hostGroupContainsMasterComp,
            Collection<VmType> availableVmTypes) {
        Map<String, VmType> result = new HashMap<>();
        Optional<VmType> masterVmType = getVmTypeByFlavor(recommendation.getFlavor(), availableVmTypes);
        if (masterVmType.isPresent()) {
            for (Map.Entry<String, Boolean> entry : hostGroupContainsMasterComp.entrySet()) {
                Boolean hasMasterComponentType = entry.getValue();
                if (hasMasterComponentType == containsMasterComponent) {
                    VmType vmType = masterVmType.get();
                    decorateWithRecommendation(vmType, recommendation);
                    result.put(entry.getKey(), vmType);
                }
            }
        }
        return result;
    }

    private Optional<VmType> getVmTypeByFlavor(String flavor, Collection<VmType> availableVmTypes) {
        return availableVmTypes.stream().filter(vm -> vm.value().equals(flavor)).findFirst();
    }

    private void decorateWithRecommendation(VmType vmType, VmRecommendation recommendation) {
        Map<String, String> vmMetaDataProps = vmType.getMetaData().getProperties();
        vmMetaDataProps.put("recommendedVolumeType", recommendation.getVolumeType());
        vmMetaDataProps.put("recommendedvolumeCount", String.valueOf(recommendation.getVolumeCount()));
        vmMetaDataProps.put("recommendedvolumeSizeGB", String.valueOf(recommendation.getVolumeSizeGB()));
    }
}
