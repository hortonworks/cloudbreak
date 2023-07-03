package com.sequenceiq.cloudbreak.controller.validation.template;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.workspace.model.User;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.api.type.InstanceGroupName;
import com.sequenceiq.common.model.AwsDiskType;

@Component
public class TemplateValidatorAndUpdater {

    public static final String GROUP_NAME_ID_BROKER = "idbroker";

    public static final String GROUP_NAME_COMPUTE = "compute";

    public static final String ROLE_IMPALAD = "IMPALAD";

    private static final Set<String> SDX_COMPUTE_INSTANCES = Set.of(InstanceGroupName.HMS_SCALE_OUT.name().toLowerCase(Locale.ROOT),
            InstanceGroupName.RAZ_SCALE_OUT.getName().toLowerCase(Locale.ROOT), InstanceGroupName.ATLAS_SCALE_OUT.getName().toLowerCase(Locale.ROOT));

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateValidatorAndUpdater.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private LocationService locationService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private ResourceDiskPropertyCalculator resourceDiskPropertyCalculator;

    @Value("${cb.doc.urls.supportedInstanceTypes:https://www.cloudera.com/products/pricing/cdp-public-cloud-service-rates.html}")
    private String supportedVmTypesDocPageLink;

    private final Supplier<Map<Platform, Map<String, VolumeParameterType>>> diskMappings =
            Suppliers.memoize(() -> cloudParameterService.getDiskTypes().getDiskMappings());

    private final Supplier<Map<Platform, PlatformParameters>> platformParameters =
            Suppliers.memoize(() -> cloudParameterService.getPlatformParameters());

    @PostConstruct
    private void postConstructChecks() {
        if (isEmpty(supportedVmTypesDocPageLink)) {
            LOGGER.warn("The documentation link for the supported instance types has not filled hence we're unable to provide it" +
                    " for the customer in the case of an invalid request!");
        }
    }

    public void validate(Credential credential, InstanceGroup instanceGroup, Stack stack,
            CdpResourceType stackType, Optional<User> user, ValidationResult.ValidationResultBuilder validationBuilder) {
        Template value = instanceGroup.getTemplate();
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential, user),
                stack.getRegion(),
                stack.getPlatformVariant(),
                stackType,
                new HashMap<>());

        if (isEmpty(value.getInstanceType())) {
            validateCustomInstanceType(value, validationBuilder);
        } else {
            VmType vmType = null;
            Platform platform = Platform.platform(value.getCloudPlatform());
            Map<String, Set<VmType>> machines = cloudVmTypes.getCloudVmResponses();
            String locationString = locationService.location(stack.getRegion(), stack.getAvailabilityZone());
            if (machines.containsKey(locationString) && !machines.get(locationString).isEmpty()) {
                for (VmType type : machines.get(locationString)) {
                    if (type.value().equals(value.getInstanceType())) {
                        vmType = type;
                        break;
                    }
                }
                if (vmType == null) {
                    validationBuilder.error(getInvalidVmTypeErrorMessage(value.getInstanceType(), platform.value(), stack.getRegion()));
                }
            }

            validateVolumeTemplates(value, vmType, platform, validationBuilder, instanceGroup, stack.getBlueprint().getBlueprintText());
            validateMaximumVolumeSize(value, vmType, validationBuilder);
            resourceDiskPropertyCalculator.updateWithResourceDiskAttached(credential, instanceGroup.getTemplate(), vmType);
        }
    }

    private Function<Map<String, Object>, Json> toJson() {
        return value -> {
            try {
                return new Json(value);
            } catch (IllegalArgumentException e) {
                LOGGER.info("Failed to parse template parameters as JSON.", e);
                throw new BadRequestException("Invalid template parameter format, valid JSON expected.");
            }
        };
    }

    private void validateVolumeTemplates(Template value, VmType vmType, Platform platform,
        ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, String bluePrintText) {
        for (VolumeTemplate volumeTemplate : value.getVolumeTemplates()) {
            VolumeParameterType volumeParameterType = null;
            Map<Platform, Map<String, VolumeParameterType>> disks = diskMappings.get();
            if (disks.containsKey(platform) && !disks.get(platform).isEmpty()) {
                Map<String, VolumeParameterType> map = disks.get(platform);
                volumeParameterType = map.get(volumeTemplate.getVolumeType());
                if (volumeParameterType == null) {
                    validationBuilder.error(
                            String.format("The '%s' volume type isn't supported by '%s' platform", volumeTemplate.getVolumeType(), platform.value()));
                }
            }

            validateVolume(volumeTemplate, vmType, platform, volumeParameterType, validationBuilder, instanceGroup, bluePrintText);
        }
    }

    private void validateCustomInstanceType(Template template, ValidationResult.ValidationResultBuilder validationBuilder) {
        Map<String, Object> params = template.getAttributes().getMap();
        Platform platform = Platform.platform(template.getCloudPlatform());
        PlatformParameters pps = platformParameters.get().get(platform);
        if (pps != null) {
            Boolean customInstanceType = pps.specialParameters().getSpecialParameters().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE);
            if (BooleanUtils.isTrue(customInstanceType)) {
                if (params.get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS) == null
                        || params.get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY) == null) {
                    validationBuilder.error(String.format("Missing 'cpus' or 'memory' param for custom instancetype on %s platform",
                            template.getCloudPlatform()));
                }
            } else {
                validationBuilder.error(String.format("Custom instancetype is not supported on %s platform", template.getCloudPlatform()));
            }
        }
    }

    private void validateVolume(VolumeTemplate value, VmType vmType, Platform platform,
        VolumeParameterType volumeParameterType, ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, String bluePrintText) {
        validateVolumeType(value, platform, validationBuilder);
        validateVolumeCount(value, vmType, volumeParameterType, validationBuilder, instanceGroup, bluePrintText);
        validateVolumeSize(value, vmType, volumeParameterType, validationBuilder, instanceGroup, bluePrintText);
    }

    private void validateMaximumVolumeSize(Template value, VmType vmType, ValidationResult.ValidationResultBuilder validationBuilder) {
        if (vmType != null) {
            Object maxSize = vmType.getMetaDataValue(VmTypeMeta.MAXIMUM_PERSISTENT_DISKS_SIZE_GB);
            if (maxSize != null) {
                int fullSize = value.getVolumeTemplates().stream()
                        .filter(volumeTemplate -> !AwsDiskType.Ephemeral.value().equals(volumeTemplate.getVolumeType()))
                        .mapToInt(volume -> volume.getVolumeSize() * volume.getVolumeCount()).sum();
                if (Integer.parseInt(maxSize.toString()) < fullSize) {
                    validationBuilder.error(
                            String.format("The %s platform does not support %s Gb full volume size. The maximum size of disks could be %s Gb.",
                                    value.getCloudPlatform(), fullSize, maxSize));
                }
            }
        }
    }

    private void validateVolumeType(VolumeTemplate value, Platform platform, ValidationResult.ValidationResultBuilder validationBuilder) {
        DiskType diskType = DiskType.diskType(value.getVolumeType());
        Map<Platform, Collection<DiskType>> diskTypes = cloudParameterService.getDiskTypes().getDiskTypes();
        if (diskTypes.containsKey(platform) && !diskTypes.get(platform).isEmpty()) {
            if (!diskTypes.get(platform).contains(diskType)) {
                validationBuilder.error(String.format("The '%s' platform does not support '%s' volume type", platform.value(), diskType.value()));
            }
        }
    }

    private boolean isIDBrokerInstanceGroup(InstanceGroup instanceGroup) {
        return GROUP_NAME_ID_BROKER.equalsIgnoreCase(instanceGroup.getGroupName());
    }

    private boolean isComputeInstanceGroup(InstanceGroup instanceGroup) {
        return GROUP_NAME_COMPUTE.equalsIgnoreCase(instanceGroup.getGroupName());
    }

    private boolean isCoordinatorAndExecutorInstanceGroup(String bluePrintText) {
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(bluePrintText);
        Set<ServiceComponent> service = processor.getAllComponents().stream()
                .filter(serviceComponent -> ROLE_IMPALAD.equals(serviceComponent.getComponent())).collect(Collectors.toSet());
        if (!service.isEmpty()) {
            return true;
        }
        return false;
    }

    private void validateVolumeCount(VolumeTemplate value, VmType vmType, VolumeParameterType volumeParameterType,
        ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, String bluePrintText) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount())) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                validateVolumeCountInParameterConfig(config, value, vmType, validationBuilder, instanceGroup, bluePrintText);
            } else {
                validationBuilder.error(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
        }
    }

    private void validateVolumeCountInParameterConfig(VolumeParameterConfig config, VolumeTemplate value, VmType vmType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, String bluePrintText) {
        // IDBroker does not use data volume, so its volume count should be zero
        // To be backward-compatible, we only check max limit and allow the min limit to be zero for IDBroker
        if (!config.possibleNumberValues().isEmpty()) {
            if (!config.possibleNumberValues().contains(value.getVolumeCount())) {
                validationBuilder.error(String.format("Allowed volume count(s) for '%s': %s", vmType.value(), config.possibleNumberValues()));
            }
        } else if (value.getVolumeCount() > config.maximumNumber()) {
            validationBuilder.error(String.format("Max allowed volume count for '%s': %s", vmType.value(), config.maximumNumber()));
        } else if (!(isIDBrokerInstanceGroup(instanceGroup) || isComputeInstanceGroup(instanceGroup)
                || isSdxComputeInstanceHostGroup(instanceGroup) || isCoordinatorAndExecutorInstanceGroup(bluePrintText)) &&
                value.getVolumeCount() < config.minimumNumber()) {
            validationBuilder.error(String.format("Min allowed volume count for '%s': %s", vmType.value(), config.minimumNumber()));
        }
    }

    private boolean isSdxComputeInstanceHostGroup(InstanceGroup instanceGroup) {
        return SDX_COMPUTE_INSTANCES.contains(instanceGroup.getGroupName().toLowerCase(Locale.ROOT));
    }

    private void validateVolumeSize(VolumeTemplate value, VmType vmType, VolumeParameterType volumeParameterType,
        ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, String bluePrintText) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount())) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                validateVolumeSizeInParameterConfig(config, value, vmType, validationBuilder, instanceGroup, bluePrintText);
            } else {
                validationBuilder.error(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
        }
    }

    private void validateVolumeSizeInParameterConfig(VolumeParameterConfig config, VolumeTemplate value, VmType vmType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, String bluePrintText) {
        // IDBroker does not use data volume, so its volume size should be zero
        // To be backward-compatible, we only check max limit and allow the min limit to be zero for IDBroker
        if (!config.possibleSizeValues().isEmpty()) {
            if (!config.possibleSizeValues().contains(value.getVolumeSize())) {
                validationBuilder.error(String.format("Allowed volume size(s) for '%s': %s", vmType.value(), config.possibleSizeValues()));
            }
        } else if (value.getVolumeSize() > config.maximumSize()) {
            validationBuilder.error(String.format("Max allowed volume size for '%s': %s", vmType.value(), config.maximumSize()));
        } else if (!(isIDBrokerInstanceGroup(instanceGroup) || isComputeInstanceGroup(instanceGroup)
                || isSdxComputeInstanceHostGroup(instanceGroup) || isCoordinatorAndExecutorInstanceGroup(bluePrintText)) &&
                value.getVolumeSize() < config.minimumSize()) {
            validationBuilder.error(String.format("Min allowed volume size for '%s': %s", vmType.value(), config.minimumSize()));
        }
    }

    private boolean needToCheckVolume(VolumeParameterType volumeParameterType, Object value) {
        return volumeParameterType != VolumeParameterType.EPHEMERAL || value != null;
    }

    private String getInvalidVmTypeErrorMessage(String instanceType, String platform, String region) {
        String baseMsg = "Our platform currently not supporting the '%s' instance type for '%s' in %s.";
        if (isEmpty(supportedVmTypesDocPageLink)) {
            return String.format(baseMsg, instanceType, platform, region);
        }
        return String.format("Our platform currently not supporting the '%s' instance type for '%s' in %s." +
                " You can find the supported types here: %s", instanceType, platform, region, supportedVmTypesDocPageLink);
    }

}
