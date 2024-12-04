package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.cloud.model.Platform.platform;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_2_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.sequenceiq.cloudbreak.api.endpoint.v4.clustertemplate.requests.DefaultClusterTemplateV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cloud.model.AutoscaleRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.GatewayRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.InstanceCount;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ResizeRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.ScaleRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendation;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterTemplate;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintTextProcessorFactory;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.template.ClusterTemplateService;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Base;

@Service
public class CloudResourceAdvisor {

    static final String ARCHITECTURE = "architecture";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudResourceAdvisor.class);

    private static final String GATEWAY_GROUP = "gatewayGroup";

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
    private ClusterTemplateService clusterTemplateService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private TransactionService transactionService;

    @Inject
    private StackTemplateService stackTemplateService;

    @Inject
    private VmAdvisor vmAdvisor;

    public PlatformRecommendation createForBlueprint(Long workspaceId, String definitionName, String blueprintName, String credentialName,
            String region, String platformVariant, String availabilityZone, CdpResourceType cdpResourceType) {
        Credential credential = credentialClientService.getByName(credentialName);
        return getPlatformRecommendationByCredential(workspaceId, definitionName, blueprintName, region,
                platformVariant, availabilityZone, cdpResourceType, credential);
    }

    public PlatformRecommendation createForBlueprintByCredCrn(Long workspaceId, String definitionName, String blueprintName, String credentialCrn,
            String region, String platformVariant, String availabilityZone, CdpResourceType cdpResourceType) {
        Credential credential = ThreadBasedUserCrnProvider.doAsInternalActor(
                regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                () -> credentialClientService.getByCrn(credentialCrn));
        return createForBlueprintByCred(workspaceId, definitionName, blueprintName, credential, region,
                platformVariant, availabilityZone, cdpResourceType);
    }

    public PlatformRecommendation createForBlueprintByCred(Long workspaceId, String definitionName, String blueprintName, Credential credential,
            String region, String platformVariant, String availabilityZone, CdpResourceType cdpResourceType) {
        return getPlatformRecommendationByCredential(workspaceId, definitionName, blueprintName, region,
                platformVariant, availabilityZone, cdpResourceType, credential);
    }

    private PlatformRecommendation getPlatformRecommendationByCredential(Long workspaceId, String definitionName, String blueprintName, String region,
            String platformVariant, String availabilityZone, CdpResourceType cdpResourceType, Credential credential) {
        String cloudPlatform = credential.cloudPlatform();
        Map<String, VmType> vmTypesByHostGroup = new HashMap<>();
        Map<String, Boolean> hostGroupContainsMasterComp = new HashMap<>();
        LOGGER.debug("Advising resources for blueprintName: {}, provider: {} and region: {}.",
                blueprintName, cloudPlatform, region);
        List<String> entitlements = entitlementService.getEntitlements(credential.getAccount());
        BlueprintTextProcessor blueprintTextProcessor = getBlueprintTextProcessor(workspaceId, blueprintName);
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

        Map<String, String> templateInfo = getInfoFromClusterTemplate(definitionName, workspaceId);
        Architecture architecture = Architecture.fromStringWithFallback(templateInfo.get(ARCHITECTURE));
        CloudVmTypes vmTypes = vmAdvisor.recommendVmTypes(blueprintTextProcessor, region, platformVariant, cdpResourceType, credential, architecture);
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
        GatewayRecommendation gateway = recommendGateway(blueprintTextProcessor, templateInfo);

        AutoscaleRecommendation autoscale = recommendAutoscale(blueprintTextProcessor, entitlements);

        ResizeRecommendation resize = recommendResize(blueprintTextProcessor, entitlements);

        return new PlatformRecommendation(vmTypesByHostGroup, availableVmTypes, diskTypes, instanceCounts, gateway, autoscale, resize);
    }

    public ScaleRecommendation createForBlueprint(Long workspaceId, String blueprintName) {
        Blueprint blueprint = getBlueprint(blueprintName, workspaceId);
        return createForBlueprint(workspaceId, blueprint);
    }

    public ScaleRecommendation createForBlueprint(Long workspaceId, Blueprint blueprint) {
        LOGGER.debug("Scale advice for blueprintName: {}.", blueprint.getName());
        BlueprintTextProcessor blueprintTextProcessor = getBlueprintTextProcessor(blueprint);
        List<String> entitlements = entitlementService.getEntitlements(blueprint.getWorkspace().getTenant().getName());
        AutoscaleRecommendation autoscale = recommendAutoscale(blueprintTextProcessor, entitlements);
        ResizeRecommendation resize = recommendResize(blueprintTextProcessor, entitlements);

        return new ScaleRecommendation(autoscale, resize);
    }

    public AutoscaleRecommendation getAutoscaleRecommendation(Long workspaceId, String blueprintName) {
        LOGGER.debug("Autoscale advice for blueprintName: {}.", blueprintName);
        BlueprintTextProcessor blueprintTextProcessor = getBlueprintTextProcessor(workspaceId, blueprintName);
        Blueprint blueprint = getBlueprint(blueprintName, workspaceId);
        List<String> entitlements = entitlementService.getEntitlements(blueprint.getWorkspace().getTenant().getName());
        return recommendAutoscale(blueprintTextProcessor, entitlements);
    }

    private Map<String, String> getInfoFromClusterTemplate(String definitionName, Long workspaceId) {
        Map<String, String> templateInfo = new HashMap<>();
        if (!Strings.isNullOrEmpty(definitionName)) {
            try {
                templateInfo = transactionService.required(() -> {
                    Optional<ClusterTemplate> templateByName = clusterTemplateService.getTemplateByName(definitionName, workspaceId);
                    if (templateByName.isPresent()) {
                        if (templateByName.get().getStackTemplate() != null) {
                            return getInfoFromCustomTemplate(templateByName.get().getStackTemplate());
                        } else {
                            return getInfoFromDefaultTemplate(definitionName, templateByName.get());
                        }
                    }
                    return new HashMap<>();
                });
            } catch (Exception e) {
                LOGGER.error("Could not parse Default Cluster with name {}. Error: {}", definitionName, e);
            }
        }
        return templateInfo;
    }

    private Map<String, String> getInfoFromDefaultTemplate(String definitionName, ClusterTemplate clusterTemplate) {
        Map<String, String> clusterTemplateInfo = new HashMap<>();
        try {
            DefaultClusterTemplateV4Request clusterTemplateV4Request = new Json(getTemplateString(clusterTemplate))
                    .get(DefaultClusterTemplateV4Request.class);
            clusterTemplateV4Request.getDistroXTemplate().getInstanceGroups()
                    .stream()
                    .filter(e -> InstanceGroupType.GATEWAY.equals(e.getType()))
                    .map(InstanceGroupV1Base::getName)
                    .findFirst()
                    .ifPresent(groupName -> clusterTemplateInfo.put(GATEWAY_GROUP, groupName));
            if (clusterTemplateV4Request.getDistroXTemplate().getArchitecture() != null) {
                clusterTemplateInfo.put(ARCHITECTURE, clusterTemplateV4Request.getDistroXTemplate().getArchitecture());
            }
        } catch (IOException e) {
            LOGGER.error("Could not parse Default Cluster with name {}. Error: {}", definitionName, e);
        }
        return clusterTemplateInfo;
    }

    private Map<String, String> getInfoFromCustomTemplate(Stack stackTemplate) {
        Map<String, String> clusterTemplateInfo = new HashMap<>();
        stackTemplateService.getByIdWithLists(stackTemplate.getId())
                .map(Stack::getInstanceGroups)
                .map(Set::stream)
                .flatMap(igs -> igs
                        .filter(e -> InstanceGroupType.GATEWAY.equals(e.getInstanceGroupType()))
                        .map(InstanceGroup::getGroupName)
                        .findFirst())
                .ifPresent(groupName -> clusterTemplateInfo.put(GATEWAY_GROUP, groupName));
        if (stackTemplate.getArchitecture() != null) {
            clusterTemplateInfo.put(ARCHITECTURE, stackTemplate.getArchitectureName());
        }
        return clusterTemplateInfo;
    }

    private String getTemplateString(ClusterTemplate clusterTemplate) {
        return new String(BaseEncoding.base64().decode(clusterTemplate.getTemplateContent()));
    }

    private BlueprintTextProcessor getBlueprintTextProcessor(Blueprint blueprint) {
        String blueprintText = blueprint.getBlueprintJsonText();
        return blueprintTextProcessorFactory.createBlueprintTextProcessor(blueprintText);
    }

    private BlueprintTextProcessor getBlueprintTextProcessor(Long workspaceId, String blueprintName) {
        Blueprint blueprint = getBlueprint(blueprintName, workspaceId);
        return getBlueprintTextProcessor(blueprint);
    }

    private GatewayRecommendation recommendGateway(BlueprintTextProcessor blueprintTextProcessor, Map<String, String> templateInfo) {
        GatewayRecommendation recommendation;
        if (templateInfo.containsKey(GATEWAY_GROUP)) {
            recommendation = new GatewayRecommendation(Set.of(templateInfo.get(GATEWAY_GROUP)));
        } else {
            recommendation = blueprintTextProcessor.recommendGateway();
            if (recommendation.getHostGroups().isEmpty()) {
                Set<String> gatewayGroups = filterHostGroupByPredicate(blueprintTextProcessor, this::fallbackGatewayFilter);
                if (!gatewayGroups.isEmpty()) {
                    recommendation = new GatewayRecommendation(gatewayGroups);
                }
            }
        }

        return recommendation;
    }

    private Set<String> filterHostGroupByPredicate(BlueprintTextProcessor blueprintTextProcessor, Predicate<String> predicate) {
        return blueprintTextProcessor.getComponentsByHostGroup().keySet().stream()
                .filter(predicate)
                .collect(Collectors.toSet());
    }

    /**
     * Logic from UI.
     */
    private boolean fallbackGatewayFilter(String hostGroupName) {
        String lowerName = hostGroupName.toLowerCase(Locale.ROOT);
        return lowerName.contains("master")
                || lowerName.contains("services")
                || lowerName.contains("manager");
    }

    private ResizeRecommendation recommendResize(BlueprintTextProcessor blueprintTextProcessor, List<String> entitlements) {
        Versioned blueprintVersion = () -> blueprintTextProcessor.getVersion().get();
        ResizeRecommendation resizeRecommendation = blueprintTextProcessor.recommendResize(entitlements, blueprintVersion);

        if (resizeRecommendation.getScaleUpHostGroups().isEmpty()) {
            Set<String> scaleUpHostGroups = filterHostGroupByPredicate(blueprintTextProcessor, this::fallbackScaleUpFilter);
            if (!scaleUpHostGroups.isEmpty()) {
                resizeRecommendation.setScaleUpHostGroups(scaleUpHostGroups);
            }
        }
        if (resizeRecommendation.getScaleDownHostGroups().isEmpty()) {
            Set<String> scaleDownHostGroups = filterHostGroupByPredicate(blueprintTextProcessor, this::fallbackScaleDownFilter);
            if (!scaleDownHostGroups.isEmpty()) {
                resizeRecommendation.setScaleDownHostGroups(scaleDownHostGroups);
            }
        }

        return resizeRecommendation;
    }

    private boolean fallbackScaleUpFilter(String hostGroupName) {
        String lowerCaseName = hostGroupName.toLowerCase(Locale.ROOT);
        return !lowerCaseName.contains("master")
                && !lowerCaseName.contains("manager");
    }

    private boolean fallbackScaleDownFilter(String hostGroupName) {
        String lowerCaseName = hostGroupName.toLowerCase(Locale.ROOT);
        return !lowerCaseName.contains("master")
                && !lowerCaseName.contains("manager");
    }

    private AutoscaleRecommendation recommendAutoscale(BlueprintTextProcessor blueprintTextProcessor, List<String> entitlements) {
        String version = blueprintTextProcessor.getVersion().orElse("");
        Versioned blueprintVersion = () -> blueprintTextProcessor.getVersion().get();
        if (!isVersionNewerOrEqualThanLimited(version, CLOUDERAMANAGER_VERSION_7_2_1)) {
            LOGGER.debug("Autoscale is not supported in this version {}.", version);
            return new AutoscaleRecommendation(Set.of(), Set.of());
        }

        return blueprintTextProcessor.recommendAutoscale(blueprintVersion, entitlements);
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
        return hostGroupName.toLowerCase(Locale.ROOT).contains("master");
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
                    decorateWithRecommendation(vmType, defaultRecommendation, cloudPlatform, diskTypes, hasMasterComponentType);
                    result.put(entry.getKey(), vmType);
                }
            }
        }
        return result;
    }

    private VmRecommendation getVmRecommendation(VmRecommendation defaultRecommendation, String group, VmRecommendation[] recommendations) {
        for (VmRecommendation recommendation : recommendations) {
            if (recommendation.getType().equals(group.toLowerCase(Locale.ROOT))) {
                defaultRecommendation = recommendation;
                break;
            }
        }
        return defaultRecommendation;
    }

    private Optional<VmType> getVmTypeByFlavor(String flavor, Collection<VmType> availableVmTypes) {
        return availableVmTypes.stream().filter(vm -> vm.value().equals(flavor)).findFirst();
    }

    private void decorateWithRecommendation(VmType vmType, VmRecommendation recommendation, String cloudPlatform, DiskTypes diskTypes,
            boolean hasMasterComponentType) {
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
        vmMetaDataProps.put("recommendedRootVolumeSize", defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(cloudPlatform, hasMasterComponentType));
    }

}
