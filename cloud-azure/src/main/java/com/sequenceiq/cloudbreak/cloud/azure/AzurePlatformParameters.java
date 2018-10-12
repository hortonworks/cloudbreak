package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType.GEO_REDUNDANT;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType.LOCALLY_REDUNDANT;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType.PREMIUM_LOCALLY_REDUNDANT;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.api.model.ArmAttachedStorageOption;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterRequest;
import com.sequenceiq.cloudbreak.cloud.model.InstanceGroupParameterResponse;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class AzurePlatformParameters implements PlatformParameters {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePlatformParameters.class);

    private static final int DEFAULT_FAULT_DOMAIN_COUNTER = 2;

    private static final int DEFAULT_UPDATE_DOMAIN_COUNTER = 20;

    private static final int START_LABEL = 98;

    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("sd", START_LABEL);

    @Value("${cb.platform.default.regions:}")
    private String defaultRegions;

    @Value("${cb.arm.vm.parameter.definition.path:}")
    private String armVmParameterDefinitionPath;

    @Value("${cb.arm.zone.parameter.definition.path:}")
    private String armZoneParameterDefinitionPath;

    @Value("${cb.arm.zone.parameter.default:North Europe}")
    private String armZoneParameterDefault;

    @Inject
    private Environment environment;

    @Inject
    @Qualifier("AzureTagSpecification")
    private TagSpecification tagSpecification;

    private VmRecommendations vmRecommendations;

    @PostConstruct
    public void init() {
        vmRecommendations = initVmRecommendations();
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings(), diskDisplayNames());
    }

    private Map<DiskType, DisplayName> diskDisplayNames() {
        Map<DiskType, DisplayName> map = new HashMap<>();
        map.put(diskType(GEO_REDUNDANT.value()), displayName(GEO_REDUNDANT.displayName()));
        map.put(diskType(LOCALLY_REDUNDANT.value()), displayName(LOCALLY_REDUNDANT.displayName()));
        map.put(diskType(PREMIUM_LOCALLY_REDUNDANT.value()), displayName(PREMIUM_LOCALLY_REDUNDANT.displayName()));
        return map;
    }

    private Collection<DiskType> getDiskTypes() {
        Collection<DiskType> disks = Lists.newArrayList();
        for (AzureDiskType diskType : AzureDiskType.values()) {
            disks.add(diskType(diskType.value()));
        }
        return disks;
    }

    private Map<String, VolumeParameterType> diskMappings() {
        Map<String, VolumeParameterType> map = new HashMap<>();
        map.put(GEO_REDUNDANT.value(), VolumeParameterType.MAGNETIC);
        map.put(LOCALLY_REDUNDANT.value(), VolumeParameterType.MAGNETIC);
        map.put(PREMIUM_LOCALLY_REDUNDANT.value(), VolumeParameterType.MAGNETIC);
        return map;
    }

    private DiskType defaultDiskType() {
        return diskType(LOCALLY_REDUNDANT.value());
    }

    @Override
    public String resourceDefinition(String resource) {
        return FileReaderUtils.readFileFromClasspathQuietly("definitions/azure-" + resource + ".json");
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        additionalStackParameterValidations.add(new StackParamValidation(PlatformParametersConsts.TTL, false, String.class, Optional.of("^[0-9]*$")));
        additionalStackParameterValidations.add(new StackParamValidation("diskPerStorage", false, String.class, Optional.empty()));
        additionalStackParameterValidations.add(new StackParamValidation("encryptStorage", false, Boolean.class, Optional.empty()));
        additionalStackParameterValidations.add(new StackParamValidation("persistentStorage", false, String.class,
                Optional.of("^[a-z0-9]{0,24}$")));
        additionalStackParameterValidations.add(new StackParamValidation("attachedStorageOption", false, ArmAttachedStorageOption.class,
                Optional.empty()));
        additionalStackParameterValidations.add(new StackParamValidation(AzureResourceConnector.RESOURCE_GROUP_NAME, false, String.class,
                Optional.empty()));
        return additionalStackParameterValidations;
    }

    @Override
    public PlatformOrchestrator orchestratorParams() {
        return new PlatformOrchestrator(Collections.singletonList(orchestrator(OrchestratorConstants.SALT)), orchestrator(OrchestratorConstants.SALT));
    }

    @Override
    public TagSpecification tagSpecification() {
        return tagSpecification;
    }

    @Override
    public String platforName() {
        return AzureConstants.PLATFORM.value();
    }

    @Override
    public Map<String, InstanceGroupParameterResponse> collectInstanceGroupParameters(Set<InstanceGroupParameterRequest> instanceGroupParameterRequests) {
        Map<String, InstanceGroupParameterResponse> result = new HashMap<>();
        for (InstanceGroupParameterRequest groupParameterRequest : instanceGroupParameterRequests) {
            InstanceGroupParameterResponse instanceGroupParameterResponse = new InstanceGroupParameterResponse();
            if (groupParameterRequest.getParameters().keySet().contains("availabilitySet")) {
                instanceGroupParameterResponse.setGroupName(groupParameterRequest.getGroupName());
                instanceGroupParameterResponse.setParameters(groupParameterRequest.getParameters());

            } else if (groupParameterRequest.getNodeCount() > 1) {
                Map<String, Object> parameters = groupParameterRequest.getParameters();

                Map<String, Object> availabilitySet = new HashMap<>();
                availabilitySet.put("name", String.format("%s-%s-as", groupParameterRequest.getGroupName(), groupParameterRequest.getStackName()));
                availabilitySet.put("faultDomainCount", DEFAULT_FAULT_DOMAIN_COUNTER);
                availabilitySet.put("updateDomainCount", DEFAULT_UPDATE_DOMAIN_COUNTER);

                parameters.put("availabilitySet", availabilitySet);

                instanceGroupParameterResponse.setGroupName(groupParameterRequest.getGroupName());
                instanceGroupParameterResponse.setParameters(parameters);
            }
            result.put(instanceGroupParameterResponse.getGroupName(), instanceGroupParameterResponse);
        }
        return result;
    }

    @Override
    public VmRecommendations recommendedVms() {
        return vmRecommendations;
    }

    private VmRecommendations initVmRecommendations() {
        VmRecommendations result = null;
        String vmRecommendation = resourceDefinition("vm-recommendation");
        try {
            result = JsonUtil.readValue(vmRecommendation, VmRecommendations.class);
        } catch (IOException e) {
            LOGGER.error("Cannot initialize Virtual machine recommendations for Azure", e);
        }
        return result;
    }
}
