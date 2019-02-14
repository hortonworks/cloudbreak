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
import org.testng.collections.Maps;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.clusterdefinition.AmbariBlueprintProcessorFactory;
import com.sequenceiq.cloudbreak.clusterdefinition.validation.StackServiceComponentDescriptors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;

@Service
public class CloudResourceAdvisor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceAdvisor.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private ClusterDefinitionService clusterDefinitionService;

    @Inject
    private AmbariBlueprintProcessorFactory ambariBlueprintProcessorFactory;

    @Inject
    private StackServiceComponentDescriptors stackServiceComponentDescs;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private CredentialService credentialService;

    public PlatformRecommendation createForBlueprint(Long workspaceId, String blueprintName, String credentialName,
            String region, String platformVariant, String availabilityZone) {
        Credential credential = credentialService.getByNameForWorkspaceId(credentialName, workspaceId);
        String cloudPlatform = credential.cloudPlatform();
        Map<String, VmType> vmTypesByHostGroup = new HashMap<>();
        Map<String, Boolean> hostGroupContainsMasterComp = new HashMap<>();
        LOGGER.debug("Advising resources for blueprintName: {}, provider: {} and region: {}.",
                blueprintName, cloudPlatform, region);

        ClusterDefinition clusterDefinition = getClusterDefinition(blueprintName, workspaceId);
        String blueprintText = clusterDefinition.getClusterDefinitionText();
        Map<String, Set<String>> componentsByHostGroup = ambariBlueprintProcessorFactory.get(blueprintText).getComponentsByHostGroup();
        componentsByHostGroup
                .forEach((hGName, components) -> hostGroupContainsMasterComp.put(hGName, isThereMasterComponents(components)));

        PlatformDisks platformDisks = cloudParameterService.getDiskTypes();
        Platform platform = platform(cloudPlatform);
        DiskTypes diskTypes = new DiskTypes(
                platformDisks.getDiskTypes().get(platform),
                platformDisks.getDefaultDisks().get(platform),
                platformDisks.getDiskMappings().get(platform),
                platformDisks.getDiskDisplayNames().get(platform));

        CloudVmTypes vmTypes = cloudParameterService.getVmTypesV2(credential, region, platformVariant, Maps.newHashMap());
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
                    hostGroupContainsMasterComp, availableVmTypes, cloudPlatform, diskTypes);
            vmTypesByHostGroup.putAll(masterVmTypes);
            Map<String, VmType> workerVmTypes = getVmTypesForComponentType(false, recommendations.getWorker(),
                    hostGroupContainsMasterComp, availableVmTypes, cloudPlatform, diskTypes);
            vmTypesByHostGroup.putAll(workerVmTypes);
        }

        return new PlatformRecommendation(vmTypesByHostGroup, availableVmTypes, diskTypes);
    }

    private ClusterDefinition getClusterDefinition(String blueprintName, Long workspaceId) {
        ClusterDefinition clusterDefinition = null;
        if (!Strings.isNullOrEmpty(blueprintName)) {
            LOGGER.debug("Try to get validation by name: {}.", blueprintName);
            clusterDefinition = clusterDefinitionService.getByNameForWorkspaceId(blueprintName, workspaceId);
        }
        return clusterDefinition;
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

    private Map<String, VmType> getVmTypesForComponentType(boolean containsMasterComponent, VmRecommendation recommendation,
            Map<String, Boolean> hostGroupContainsMasterComp, Collection<VmType> availableVmTypes, String cloudPlatform, DiskTypes diskTypes) {
        Map<String, VmType> result = new HashMap<>();
        Optional<VmType> masterVmType = getVmTypeByFlavor(recommendation.getFlavor(), availableVmTypes);
        if (masterVmType.isPresent()) {
            for (Entry<String, Boolean> entry : hostGroupContainsMasterComp.entrySet()) {
                Boolean hasMasterComponentType = entry.getValue();
                if (hasMasterComponentType == containsMasterComponent) {
                    VmType vmType = masterVmType.get();
                    decorateWithRecommendation(vmType, recommendation, cloudPlatform, diskTypes);
                    result.put(entry.getKey(), vmType);
                }
            }
        }
        return result;
    }

    private Optional<VmType> getVmTypeByFlavor(String flavor, Collection<VmType> availableVmTypes) {
        return availableVmTypes.stream().filter(vm -> vm.value().equals(flavor)).findFirst();
    }

    private void decorateWithRecommendation(VmType vmType, VmRecommendation recommendation, String cloudPlatform, DiskTypes diskTypes) {
        Map<String, Object> vmMetaDataProps = vmType.getMetaData().getProperties();
        vmMetaDataProps.put("recommendedVolumeType", recommendation.getVolumeType());
        VolumeParameterType volumeParameterType = VolumeParameterType.valueOf(recommendation.getVolumeType());
        if (diskTypes.diskMapping().containsValue(volumeParameterType)) {
            Optional<Entry<String, VolumeParameterType>> recommendedVolumeName = diskTypes
                    .diskMapping()
                    .entrySet()
                    .stream()
                    .filter(entry -> volumeParameterType.equals(entry.getValue()))
                    .findFirst();
            if (recommendedVolumeName.isPresent()) {
                vmMetaDataProps.put("recommendedVolumeName", recommendedVolumeName.get().getKey());
                DiskType diskType = DiskType.diskType(recommendedVolumeName.get().getKey());
                if (diskTypes.displayNames().containsValue(diskType)) {
                    vmMetaDataProps.put("recommendedVolumeDisplayName", diskTypes.displayNames().get(diskType).value());
                }
            }
        }
        vmMetaDataProps.put("recommendedvolumeCount", recommendation.getVolumeCount());
        vmMetaDataProps.put("recommendedvolumeSizeGB", recommendation.getVolumeSizeGB());
        vmMetaDataProps.put("recommendedRootVolumeSize", defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform));
    }
}
