package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts.TTL_MILLIS;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpEnabledInstanceTypes.GCP_ENABLED_TYPES_LIST;
import static com.sequenceiq.cloudbreak.cloud.model.DiskType.diskType;
import static com.sequenceiq.cloudbreak.cloud.model.DisplayName.displayName;
import static com.sequenceiq.cloudbreak.cloud.model.Orchestrator.orchestrator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpDiskUtil;
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

    @Value("${cb.gcp.disk.type.default:pd-balanced}")
    private String defaultDiskType;

    @Value("${cb.gcp.root.disk.type.default:pd-ssd}")
    private String defaultRootDiskType;

    @Value("${cb.gcp.root.disk.type.default:hyperdisk-balanced}")
    private String defaultRootHyperDiskType;

    @Value("${cb.gcp.database.disk.type:pd-ssd}")
    private String databaseDiskType;

    @Value("${cb.gcp.database.hyperdisk.type:hyperdisk-balanced}")
    private String databaseHyperDiskType;

    @Inject
    private CloudbreakResourceReaderService cloudbreakResourceReaderService;

    @Inject
    private GcpDiskUtil gcpDiskUtil;

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
        // Followup tasks to handle machinetype based disktypes
        // https://cloudera.atlassian.net/browse/CB-32446, https://cloudera.atlassian.net/browse/CB-32447
        return new DiskTypes(getDiskTypes(), defaultDiskType(), diskMappings(), diskDisplayNames());
    }

    @Override
    public String embeddedDatabaseDiskType(String flavor) {
        return gcpDiskUtil.isHyperdiskBalancedSupportedForInstanceType(flavor) ? databaseHyperDiskType : databaseDiskType;
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
        return Arrays.stream(GcpDiskType.values())
                .collect(Collectors.toMap(diskType -> diskType(diskType.value()), diskType -> displayName(diskType.displayName())));
    }

    private Map<String, VolumeParameterType> diskMappings() {
        return Arrays.stream(GcpDiskType.values())
                .collect(Collectors.toMap(diskType -> diskType(diskType.value()).getValue(), GcpDiskType::getVolumeParameterType));
    }

    private Collection<DiskType> getDiskTypes() {
        Collection<DiskType> disks = Lists.newArrayList();
        for (GcpDiskType diskType : GcpDiskType.values()) {
            disks.add(diskType(diskType.value()));
        }
        return disks;
    }

    @Override
    public DiskType defaultDiskType() {
        return diskType(defaultDiskType);
    }

    @Override
    public DiskType defaultRootDiskType(String flavor) {
        return diskType(gcpDiskUtil.isHyperdiskBalancedSupportedForInstanceType(flavor) ? defaultRootHyperDiskType : defaultRootDiskType);
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
        SSD("pd-ssd", "Solid-state persistent disks (SSD)", VolumeParameterType.SSD),
        BALANCED("pd-balanced", "Balanced persistent disks (BALANCED)", VolumeParameterType.MAGNETIC),
        EXTREME("pd-extreme", "Extreme persistent disks (EXTREME)", VolumeParameterType.SSD),
        HDD("pd-standard", "Standard persistent disks (HDD)", VolumeParameterType.MAGNETIC),
        LOCAL_SSD("local-ssd", "Local scratch disk (SSD)", VolumeParameterType.LOCAL_SSD),
        HYPERDISK_EXTREME("hyperdisk-extreme", "Hyperdisk extreme disks (HYPERDISK_EXTREME)", VolumeParameterType.HYPERDISK_EXTREME),
        HYPERDISK_BALANCED("hyperdisk-balanced", "Hyperdisk balanced disk (HYPERDISK_BALANCED)", VolumeParameterType.HYPERDISK_BALANCED),
        HYPERDISK_THROUGHPUT("hyperdisk-throughput", "Hyperdisk throughput disk (HYPERDISK_THROUGHPUT)", VolumeParameterType.HYPERDISK_THROUGHPUT);

        private final String value;

        private final String displayName;

        private final VolumeParameterType volumeParameterType;

        GcpDiskType(String value, String displayName, VolumeParameterType volumeParameterType) {
            this.value = value;
            this.displayName = displayName;
            this.volumeParameterType = volumeParameterType;
        }

        public static GcpDiskType findByValue(String value) {
            return Arrays.stream(GcpDiskType.values())
                    .filter(diskType -> diskType.value.equals(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No GCP disk type found for value: " + value));
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

        public VolumeParameterType getVolumeParameterType() {
            return volumeParameterType;
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
