package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintTextProcessorFactory;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.common.api.type.CdpResourceType;

@Service
public class CloudResourceAdvisor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceAdvisor.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private BlueprintService blueprintService;

    @Inject
    private BlueprintTextProcessorFactory blueprintTextProcessorFactory;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    public PlatformRecommendation createForBlueprint(Long workspaceId, String blueprintName, String credentialName,
            String region, String platformVariant, String availabilityZone, CdpResourceType cdpResourceType) {
        Credential credential = credentialClientService.getByName(credentialName);
        String cloudPlatform = credential.cloudPlatform();
        Map<String, VmType> vmTypesByHostGroup = new HashMap<>();
        Map<String, Boolean> hostGroupContainsMasterComp = new HashMap<>();
        LOGGER.debug("Advising resources for blueprintName: {}, provider: {} and region: {}.",
                blueprintName, cloudPlatform, region);

        Blueprint blueprint = getBlueprint(blueprintName, workspaceId);
        String blueprintText = blueprint.getBlueprintText();
        BlueprintTextProcessor blueprintTextProcessor =
                blueprintTextProcessorFactory.createBlueprintTextProcessor(blueprintText);
        Map<String, Set<String>> componentsByHostGroup = blueprintTextProcessor.getComponentsByHostGroup();
        componentsByHostGroup.forEach((hGName, components) -> hostGroupContainsMasterComp.put(hGName,
                isThereMasterComponents(blueprintTextProcessor.getClusterManagerType(), hGName, components)));

        PlatformDisks platformDisks = cloudParameterService.getDiskTypes();
        Platform platform = platform(cloudPlatform);
        DiskTypes diskTypes = new DiskTypes(
                platformDisks.getDiskTypes().get(platform),
                platformDisks.getDefaultDisks().get(platform),
                platformDisks.getDiskMappings().get(platform),
                platformDisks.getDiskDisplayNames().get(platform));

        CloudVmTypes vmTypes = cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential),
                region,
                platformVariant,
                cdpResourceType,
                Maps.newHashMap());
        VmType defaultVmType = getDefaultVmType(availabilityZone, vmTypes);
        if (defaultVmType != null) {
            componentsByHostGroup.keySet().forEach(comp -> vmTypesByHostGroup.put(comp, defaultVmType));
        }
        VmRecommendations recommendations = cloudParameterService.getRecommendation(cloudPlatform);

        Set<VmType> availableVmTypes = null;
        if (StringUtils.isNotBlank(availabilityZone)) {
            availableVmTypes = vmTypes.getCloudVmResponses().get(availabilityZone);
        } else if (vmTypes.getCloudVmResponses() != null && !vmTypes.getCloudVmResponses().isEmpty()) {
            availableVmTypes = vmTypes.getCloudVmResponses().values().iterator().next();
        }
        if (availableVmTypes == null) {
            availableVmTypes = Collections.emptySet();
        }
        if (recommendations != null) {
            Map<String, VmType> masterVmTypes = getVmTypesForComponentType(
                    true,
                    recommendations.getMaster(),
                    hostGroupContainsMasterComp,
                    availableVmTypes,
                    cloudPlatform,
                    diskTypes,
                    recommendations.getMaster());
            vmTypesByHostGroup.putAll(masterVmTypes);
            Map<String, VmType> workerVmTypes = getVmTypesForComponentType(
                    false,
                    recommendations.getWorker(),
                    hostGroupContainsMasterComp,
                    availableVmTypes,
                    cloudPlatform,
                    diskTypes,
                    recommendations.getWorker(), recommendations.getBroker(), recommendations.getQuorum());
            vmTypesByHostGroup.putAll(workerVmTypes);
        } else {
            componentsByHostGroup.keySet().forEach(hg -> vmTypesByHostGroup.put(hg, null));
        }

        Map<String, InstanceCount> instanceCounts = recommendInstanceCounts(blueprintTextProcessor);

        GatewayRecommendation gateway = recommendGateway(blueprintTextProcessor);

        return new PlatformRecommendation(vmTypesByHostGroup, availableVmTypes, diskTypes, instanceCounts, gateway);
    }

    private GatewayRecommendation recommendGateway(BlueprintTextProcessor blueprintTextProcessor) {
        GatewayRecommendation recommendation = blueprintTextProcessor.recommendGateway();

        if (recommendation.getHostGroups().isEmpty()) {
            Set<String> gatewayGroups = blueprintTextProcessor.getComponentsByHostGroup().keySet().stream()
                    .filter(this::fallbackGatewayFilter)
                    .collect(Collectors.toSet());
            if (!gatewayGroups.isEmpty()) {
                recommendation = new GatewayRecommendation(gatewayGroups);
            }
        }

        return recommendation;
    }

    /**
     * Logic from UI.
     */
    private boolean fallbackGatewayFilter(String hostGroupName) {
        String lowerName = hostGroupName.toLowerCase();
        return lowerName.contains("master")
                || lowerName.contains("services");
    }

    private Map<String, InstanceCount> recommendInstanceCounts(BlueprintTextProcessor blueprintProcessor) {
        Map<String, InstanceCount> cardinality = new TreeMap<>(blueprintProcessor.getCardinalityByHostGroup());
        for (String hostGroup : blueprintProcessor.getComponentsByHostGroup().keySet()) {
            cardinality.computeIfAbsent(hostGroup, InstanceCount::fallbackInstanceCountRecommendation);
        }
        return cardinality;
    }

    private Blueprint getBlueprint(String blueprintName, Long workspaceId) {
        Blueprint blueprint = null;
        if (!Strings.isNullOrEmpty(blueprintName)) {
            LOGGER.debug("Try to get validation by name: {}.", blueprintName);
            blueprint = blueprintService.getByNameForWorkspaceId(blueprintName, workspaceId);
        }
        return blueprint;
    }

    private boolean isThereMasterComponents(ClusterManagerType clusterManagerType, String hostGroupName, Collection<String> components) {
        return hostGroupName.toLowerCase().contains("master");
    }

    private VmType getDefaultVmType(String availabilityZone, CloudVmTypes vmtypes) {
        VmType vmType = null;
        if (StringUtils.isNotBlank(availabilityZone)) {
            vmtypes.getDefaultCloudVmResponses().get(availabilityZone);
        } else if (vmtypes.getDefaultCloudVmResponses() != null && !vmtypes.getDefaultCloudVmResponses().isEmpty()) {
            vmType = vmtypes.getDefaultCloudVmResponses().values().iterator().next();
        }
        if (vmType == null || Strings.isNullOrEmpty(vmType.value())) {
            return null;
        }
        return vmType;
    }

    private Map<String, VmType> getVmTypesForComponentType(
            boolean containsMasterComponent,
            VmRecommendation defaultRecommendation,
            Map<String, Boolean> hostGroupContainsMasterComp,
            Collection<VmType> availableVmTypes,
            String cloudPlatform,
            DiskTypes diskTypes,
            VmRecommendation... recommendations) {
        Map<String, VmType> result = new HashMap<>();
        Optional<VmType> availableVmType = getVmTypeByFlavor(defaultRecommendation.getFlavor(), availableVmTypes);
        if (availableVmType.isPresent()) {
            for (Entry<String, Boolean> entry : hostGroupContainsMasterComp.entrySet()) {
                Boolean hasMasterComponentType = entry.getValue();
                if (hasMasterComponentType == containsMasterComponent) {
                    defaultRecommendation = getVmRecommendation(defaultRecommendation, entry.getKey(), recommendations);
                    VmType vmType = availableVmType.get();
                    decorateWithRecommendation(vmType, defaultRecommendation, cloudPlatform, diskTypes);
                    result.put(entry.getKey(), vmType);
                }
            }
        }
        return result;
    }

    private VmRecommendation getVmRecommendation(VmRecommendation defaultRecommendation, String group, VmRecommendation[] recommendations) {
        for (VmRecommendation recommendation : recommendations) {
            if (recommendation.getType().equals(group.toLowerCase())) {
                defaultRecommendation = recommendation;
                break;
            }
        }
        return defaultRecommendation;
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
                if (diskTypes.displayNames().containsKey(diskType) && diskTypes.displayNames().get(diskType) != null) {
                    vmMetaDataProps.put("recommendedVolumeDisplayName", diskTypes.displayNames().get(diskType).value());
                }
            }
        }
        vmMetaDataProps.put("recommendedvolumeCount", recommendation.getVolumeCount());
        vmMetaDataProps.put("recommendedvolumeSizeGB", recommendation.getVolumeSizeGB());
        vmMetaDataProps.put("recommendedRootVolumeSize", defaultRootVolumeSizeProvider.getForPlatform(cloudPlatform));
    }

}
