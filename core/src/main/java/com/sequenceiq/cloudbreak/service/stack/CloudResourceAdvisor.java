package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.blueprint.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Service
public class CloudResourceAdvisor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceAdvisor.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintProcessorFactory blueprintProcessorFactory;

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    public PlatformRecommendation createForBlueprint(String blueprintName, Long blueprintId, PlatformResourceRequest resourceRequest, User user,
            Workspace workspace) {
        String cloudPlatform = resourceRequest.getCloudPlatform();
        String region = resourceRequest.getRegion();
        String availabilityZone = resourceRequest.getAvailabilityZone();
        Map<String, VmType> vmTypesByHostGroup = new HashMap<>();
        Map<String, Boolean> hostGroupContainsMasterComp = new HashMap<>();
        LOGGER.info("Advising resources for blueprintId: {}, blueprintName: {}, provider: {} and region: {}.",
                blueprintId, blueprintName, cloudPlatform, region);

        Blueprint blueprint = getBlueprint(blueprintName, blueprintId, user, workspace);
        String blueprintText = blueprint.getBlueprintText().getRaw();
        Map<String, Set<String>> componentsByHostGroup = blueprintProcessorFactory.get(blueprintText).getComponentsByHostGroup();
        componentsByHostGroup
                .forEach((hGName, components) -> hostGroupContainsMasterComp.put(hGName, isThereMasterComponents(components)));

        CloudVmTypes vmTypes = cloudParameterService.getVmTypesV2(
                resourceRequest.getCredential(),
                resourceRequest.getRegion(),
                resourceRequest.getPlatformVariant(),
                resourceRequest.getFilters());
        VmType defaultVmType = getDefaultVmType(availabilityZone, vmTypes);
        if (defaultVmType != null) {
            componentsByHostGroup.keySet().forEach(comp -> vmTypesByHostGroup.put(comp, defaultVmType));
        }
        VmRecommendations recommendations = cloudParameterService.getRecommendation(cloudPlatform);

        Set<VmType> availableVmTypes = vmTypes.getCloudVmResponses().get(availabilityZone);
        if (availableVmTypes == null) {
            availableVmTypes = Collections.emptySet();
        }
        if (recommendations != null) {
            Map<String, VmType> masterVmTypes = getVmTypesForComponentType(true, recommendations.getMaster(),
                    hostGroupContainsMasterComp, availableVmTypes, cloudPlatform);
            vmTypesByHostGroup.putAll(masterVmTypes);
            Map<String, VmType> workerVmTypes = getVmTypesForComponentType(false, recommendations.getWorker(),
                    hostGroupContainsMasterComp, availableVmTypes, cloudPlatform);
            vmTypesByHostGroup.putAll(workerVmTypes);
        }

        PlatformDisks platformDisks = cloudParameterService.getDiskTypes();
        Platform platform = platform(cloudPlatform);
        DiskTypes diskTypes = new DiskTypes(
                platformDisks.getDiskTypes().get(platform),
                platformDisks.getDefaultDisks().get(platform),
                platformDisks.getDiskMappings().get(platform),
                platformDisks.getDiskDisplayNames().get(platform));

        return new PlatformRecommendation(vmTypesByHostGroup, availableVmTypes, diskTypes);
    }

    private Blueprint getBlueprint(String blueprintName, Long blueprintId, User user, Workspace workspace) {
        Blueprint bp;
        if (blueprintId != null) {
            LOGGER.debug("Try to get validation by id: {}.", blueprintId);
            bp = blueprintService.get(blueprintId);
        } else {
            LOGGER.debug("Try to get validation by name: {}.", blueprintName);
            bp = blueprintService.getByNameForWorkspace(blueprintName, workspace);
        }
        return bp;
    }

    private boolean isThereMasterComponents(Collection<String> components) {
        return components.stream()
                .anyMatch(component -> stackServiceComponentDescs.get(component) != null && stackServiceComponentDescs.get(component).isMaster());
    }

    private VmType getDefaultVmType(String availabilityZone, CloudVmTypes vmtypes) {
        VmType vmType = vmtypes.getDefaultCloudVmResponses().get(availabilityZone);
        if (vmType == null || Strings.isNullOrEmpty(vmType.value())) {
            return null;
        }
        return vmType;
    }

    private Map<String, VmType> getVmTypesForComponentType(boolean containsMasterComponent,
            VmRecommendation recommendation,
            Map<String, Boolean> hostGroupContainsMasterComp,
            Collection<VmType> availableVmTypes,
            String cloudPlatform) {
        Map<String, VmType> result = new HashMap<>();
        Optional<VmType> masterVmType = getVmTypeByFlavor(recommendation.getFlavor(), availableVmTypes);
        if (masterVmType.isPresent()) {
            for (Entry<String, Boolean> entry : hostGroupContainsMasterComp.entrySet()) {
                Boolean hasMasterComponentType = entry.getValue();
                if (hasMasterComponentType == containsMasterComponent) {
                    VmType vmType = masterVmType.get();
                    decorateWithRecommendation(vmType, recommendation, cloudPlatform);
                    result.put(entry.getKey(), vmType);
                }
            }
        }
        return result;
    }

    private Optional<VmType> getVmTypeByFlavor(String flavor, Collection<VmType> availableVmTypes) {
        return availableVmTypes.stream().filter(vm -> vm.value().equals(flavor)).findFirst();
    }

    private void decorateWithRecommendation(VmType vmType, VmRecommendation recommendation, String cloudPlatform) {
        Map<String, Object> vmMetaDataProps = vmType.getMetaData().getProperties();
        vmMetaDataProps.put("recommendedVolumeType", recommendation.getVolumeType());
        vmMetaDataProps.put("recommendedvolumeCount", String.valueOf(recommendation.getVolumeCount()));
        vmMetaDataProps.put("recommendedvolumeSizeGB", String.valueOf(recommendation.getVolumeSizeGB()));
        vmMetaDataProps.put("recommendedRootVolumeSize", String.valueOf(defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform)));
    }
}
