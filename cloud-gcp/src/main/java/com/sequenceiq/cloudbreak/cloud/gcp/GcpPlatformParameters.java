package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.TTL_MILLIS;
import static com.sequenceiq.cloudbreak.cloud.gcp.DistroxEnabledInstanceTypes.GCP_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.TagValidator;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.DisplayName;
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrator;
import com.sequenceiq.cloudbreak.cloud.model.ScriptParams;
import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;
import com.sequenceiq.cloudbreak.cloud.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.model.VmRecommendations;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.OrchestratorConstants;
import com.sequenceiq.cloudbreak.service.CloudbreakResourceReaderService;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.CredentialType;

@Service
public class GcpPlatformParameters implements PlatformParameters {

    private static final Integer START_LABEL = 97;

    private static final ScriptParams SCRIPT_PARAMS = new ScriptParams("sd", START_LABEL);

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpPlatformParameters.class);

    @Value("${cb.gcp.zone.parameter.default:europe-west1}")
    private String gcpZoneParameterDefault;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private GcpTagValidator gcpTagValidator;

    @Inject
    @Qualifier("GcpTagSpecification")
    private TagSpecification tagSpecification;

    private VmRecommendations vmRecommendations;

    private String prerequisitesCreationCommand;

    private String prerequisitesAuditCreationCommand;

    private String minimalPrerequisitesCreationCommand;

    private String minimalPrerequisitesCreationPermissions;

    @PostConstruct
    public void init() {
        vmRecommendations = initVmRecommendations();
        prerequisitesCreationCommand = resourceDefinition("prerequisites-creation-command");
        minimalPrerequisitesCreationCommand = resourceDefinition("minimal-prerequisites-creation-command");
        prerequisitesAuditCreationCommand = resourceDefinition("audit-prerequisites-creation-command");
        minimalPrerequisitesCreationPermissions = resourceDefinition("environment-minimal-policy");
    }

    public SpecialParameters specialParameters() {
        SpecialParameters specialParameters = PlatformParameters.super.specialParameters();
        specialParameters.getSpecialParameters().put(PlatformParametersConsts.DELETE_VOLUMES_SUPPORTED, Boolean.FALSE);
        specialParameters.getSpecialParameters().put(PlatformParametersConsts.DISK_TYPE_CHANGE_SUPPORTED, Boolean.FALSE);
        specialParameters.getSpecialParameters().put(PlatformParametersConsts.ADD_VOLUMES_SUPPORTED, Boolean.FALSE);
        return specialParameters;
    }

    @Override
    public TagValidator tagValidator() {
        return gcpTagValidator;
    }

    @Override
    public ScriptParams scriptParams() {
        return SCRIPT_PARAMS;
    }

    @Override
    public DiskTypes diskTypes() {
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings(), diskDisplayNames());
    }

    @Override
    public String resourceDefinition(String resource) {
        return cloudbreakResourceReaderService.resourceDefinition("gcp", resource);
    }

    @Override
    public String resourceDefinitionInSubDir(String subDir, String resource) {
        return cloudbreakResourceReaderService.resourceDefinitionInSubDir(subDir, "gcp", resource);
    }

    @Override
    public List<StackParamValidation> additionalStackParameters() {
        List<StackParamValidation> additionalStackParameterValidations = Lists.newArrayList();
        additionalStackParameterValidations.add(new StackParamValidation(TTL_MILLIS, false, String.class, Optional.of("^[0-9]*$")));
        return additionalStackParameterValidations;
    }

    @Override
    public Set<String> getDistroxEnabledInstanceTypes(Architecture architecture) {
        return new HashSet<>(GCP_ENABLED_TYPES_LIST);
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
        return GcpConstants.GCP_PLATFORM.value();
    }

    @Override
    public boolean isAutoTlsSupported() {
        return true;
    }

    @Override
    public VmRecommendations recommendedVms() {
        return vmRecommendations;
    }

    private Map<DiskType, DisplayName> diskDisplayNames() {
        Map<DiskType, DisplayName> map = new HashMap<>();
        map.put(diskType(GcpDiskType.HDD.value()), displayName(GcpDiskType.HDD.displayName()));
        map.put(diskType(GcpDiskType.BALANCED.value()), displayName(GcpDiskType.BALANCED.displayName()));
        map.put(diskType(GcpDiskType.EXTREME.value()), displayName(GcpDiskType.EXTREME.displayName()));
        map.put(diskType(GcpDiskType.SSD.value()), displayName(GcpDiskType.SSD.displayName()));
        map.put(diskType(GcpDiskType.LOCAL_SSD.value()), displayName(GcpDiskType.LOCAL_SSD.displayName()));
        return map;
    }

    private Map<String, VolumeParameterType> diskMappings() {
        Map<String, VolumeParameterType> map = new HashMap<>();
        map.put(GcpDiskType.HDD.value(), VolumeParameterType.MAGNETIC);
        map.put(GcpDiskType.BALANCED.value(), VolumeParameterType.MAGNETIC);
        map.put(GcpDiskType.EXTREME.value(), VolumeParameterType.SSD);
        map.put(GcpDiskType.SSD.value(), VolumeParameterType.SSD);
        map.put(GcpDiskType.LOCAL_SSD.value(), VolumeParameterType.LOCAL_SSD);

        return map;
    }

    private Collection<DiskType> getDiskTypes() {
        Collection<DiskType> disks = Lists.newArrayList();
        for (GcpDiskType diskType : GcpDiskType.values()) {
            disks.add(diskType(diskType.value()));
        }
        return disks;
    }

    private DiskType defaultDiskType() {
        return diskType(GcpDiskType.HDD.value());
    }

    @Override
    public DiskType defaultRootDiskType() {
        return diskType(GcpDiskType.HDD.value());
    }

    public String getPrerequisitesCreationCommand(CredentialType type) {
        if (CredentialType.AUDIT.equals(type)) {
            return prerequisitesAuditCreationCommand;
        } else {
            return prerequisitesCreationCommand;
        }
    }

    public String getMinimalPrerequisitesCreationCommand() {
        return minimalPrerequisitesCreationCommand;
    }

    public String getMinimalPrerequisitesCreationPermissions() {
        return minimalPrerequisitesCreationPermissions;
    }

    public enum GcpDiskType {
        SSD("pd-ssd", "Solid-state persistent disks (SSD)"),
        BALANCED("pd-balanced", "Balanced persistent disks (BALANCED)"),
        EXTREME("pd-extreme", "Extreme persistent disks (EXTREME)"),
        HDD("pd-standard", "Standard persistent disks (HDD)"),
        LOCAL_SSD("local-ssd", "Local scratch disk (SSD)");

        private final String value;

        private final String displayName;

        GcpDiskType(String value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        public static String getUrl(String projectId, String zone, String volumeId) {
            return String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/diskTypes/%s", projectId, zone, volumeId);
        }

        public String value() {
            return value;
        }

        public String displayName() {
            return displayName;
        }

        public String getUrl(String projectId, String zone) {
            return getUrl(projectId, zone, value);
        }

    }

    private VmRecommendations initVmRecommendations() {
        VmRecommendations result = null;
        String vmRecommendation = resourceDefinition("vm-recommendation");
        try {
            result = JsonUtil.readValue(vmRecommendation, VmRecommendations.class);
        } catch (IOException e) {
            LOGGER.error("Cannot initialize Virtual machine recommendations for GCP", e);
        }
        return result;
    }
}
