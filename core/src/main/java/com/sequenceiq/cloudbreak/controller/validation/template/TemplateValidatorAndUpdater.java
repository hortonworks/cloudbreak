package com.sequenceiq.cloudbreak.controller.validation.template;

import static com.sequenceiq.cloudbreak.constant.AwsPlatformResourcesFilterConstants.ARCHITECTURE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmTypes;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.generic.StringType;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.controller.validation.template.azure.HostEncryptionProvider;
import com.sequenceiq.cloudbreak.controller.validation.template.azure.ResourceDiskPropertyCalculator;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.common.api.type.InstanceGroupName;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.AwsDiskType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Component
public class TemplateValidatorAndUpdater {

    public static final String GROUP_NAME_ID_BROKER = "idbroker";

    public static final String ROLE_IMPALAD = "IMPALAD";

    private static final Set<String> SDX_COMPUTE_INSTANCES = Set.of(InstanceGroupName.HMS_SCALE_OUT.getName(), InstanceGroupName.RAZ_SCALE_OUT.getName(),
            InstanceGroupName.ATLAS_SCALE_OUT.getName());

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateValidatorAndUpdater.class);

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private EmptyVolumeSetFilter emptyVolumeSetFilter;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Inject
    private LocationService locationService;

    @Inject
    private CredentialToExtendedCloudCredentialConverter extendedCloudCredentialConverter;

    @Inject
    private ResourceDiskPropertyCalculator resourceDiskPropertyCalculator;

    @Inject
    private HostEncryptionProvider hostEncryptionProvider;

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

    public void validate(DetailedEnvironmentResponse environment, Credential credential, InstanceGroup instanceGroup, Stack stack,
            CdpResourceType stackType, ValidationResult.ValidationResultBuilder validationBuilder) {
        Template template = instanceGroup.getTemplate();
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential),
                stack.getRegion(),
                stack.getPlatformVariant(),
                stackType,
                Map.of(ARCHITECTURE, stack.getArchitectureName()));

        if (isEmpty(template.getInstanceType())) {
            validateCustomInstanceType(template, validationBuilder);
        } else {
            VmType vmType = null;
            Platform platform = Platform.platform(template.getCloudPlatform());
            Map<String, Set<VmType>> machines = cloudVmTypes.getCloudVmResponses();
            String locationString = locationService.location(stack.getRegion(), stack.getAvailabilityZone());
            if (machines.containsKey(locationString) && !machines.get(locationString).isEmpty()) {
                for (VmType type : machines.get(locationString)) {
                    if (type.value().equals(template.getInstanceType())) {
                        vmType = type;
                        break;
                    }
                }
                if (vmType == null) {
                    LOGGER.info("Instance type not found {} at location {}. Available instances: {}", template.getInstanceType(), locationString,
                            machines.get(locationString).stream().map(StringType::value).toList());
                    validationBuilder.error(getInvalidVmTypeErrorMessage(template.getInstanceType(), platform.value(), stack.getRegion()));
                } else {
                    validateArchitecture(vmType, stack, validationBuilder);
                }
            }
            template = emptyVolumeSetFilter.filterOutVolumeSetsWhichAreEmpty(instanceGroup.getTemplate());
            template = resourceDiskPropertyCalculator.updateWithResourceDiskAttached(credential, template, vmType);
            template = hostEncryptionProvider.updateWithHostEncryption(environment, credential, template, vmType);
            validateVolumeTemplates(template, vmType, platform, validationBuilder, instanceGroup, stack);
            validateMaximumVolumeSize(template, vmType, validationBuilder);
        }
    }

    public void validateGroupForVerticalScale(Credential credential, InstanceGroup instanceGroup, Stack stack,
        CdpResourceType stackType, ValidationResult.ValidationResultBuilder validationBuilder) {
        Template template = instanceGroup.getTemplate();
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(
                extendedCloudCredentialConverter.convert(credential),
                stack.getRegion(),
                stack.getPlatformVariant(),
                stackType,
                Map.of(ARCHITECTURE, stack.getArchitectureName()));
        VmType vmType = null;
        Platform platform = Platform.platform(template.getCloudPlatform());
        Map<String, Set<VmType>> machines = cloudVmTypes.getCloudVmResponses();
        String locationString = locationService.location(stack.getRegion(), stack.getAvailabilityZone());
        if (machines.containsKey(locationString) && !machines.get(locationString).isEmpty()) {
            for (VmType type : machines.get(locationString)) {
                if (type.value().equals(template.getInstanceType())) {
                    vmType = type;
                    break;
                }
            }
        }
        validateVolumeTemplates(template, vmType, platform, validationBuilder, instanceGroup, stack);
        validateMaximumVolumeSize(template, vmType, validationBuilder);
    }

    private void validateArchitecture(VmType vmType, Stack stack, ValidationResult.ValidationResultBuilder validationBuilder) {
        Architecture vmArchitecture = vmType.getMetaData().getArchitecture();
        Architecture stackArchitecture = stack.getArchitecture();
        if (vmArchitecture != stackArchitecture) {
            validationBuilder.error(
                    String.format("The '%s' instance type's architecture '%s' is not matching requested architecture '%s'",
                            vmType.value(), vmArchitecture.getName(), stackArchitecture.getName()));
        }
    }

    private void validateVolumeTemplates(Template value, VmType vmType, Platform platform,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
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

            validateVolume(volumeTemplate, vmType, platform, volumeParameterType, validationBuilder, instanceGroup, stack);
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

    private void validateVolume(VolumeTemplate value, VmType vmType, Platform platform, VolumeParameterType volumeParameterType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        validateVolumeType(value, platform, validationBuilder);
        validateVolumeCount(value, vmType, volumeParameterType, validationBuilder, instanceGroup, stack);
        validateVolumeSize(value, vmType, volumeParameterType, validationBuilder, instanceGroup, stack);
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

    private boolean isIDBrokerInstanceGroup(InstanceGroup instanceGroup, Stack stack) {
        return GROUP_NAME_ID_BROKER.equalsIgnoreCase(instanceGroup.getGroupName()) && datalakeOrTemplate(stack.getType());
    }

    private boolean isCoordinatorAndExecutorInstanceGroup(String bluePrintText) {
        CmTemplateProcessor processor = cmTemplateProcessorFactory.get(bluePrintText);
        Set<ServiceComponent> service = new HashSet<>();
        if (processor != null) {
            service = processor.getAllComponents().stream()
                    .filter(serviceComponent -> ROLE_IMPALAD.equals(serviceComponent.getComponent())).collect(Collectors.toSet());
        }
        if (!service.isEmpty()) {
            return true;
        }
        return false;
    }

    private void validateVolumeCount(VolumeTemplate value, VmType vmType, VolumeParameterType volumeParameterType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount()) && volumeParameterType != null) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                validateVolumeCountInParameterConfig(config, value, vmType, validationBuilder, instanceGroup, stack);
            } else {
                validationBuilder.error(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
        }
    }

    private void validateVolumeCountInParameterConfig(VolumeParameterConfig config, VolumeTemplate value, VmType vmType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        // IDBroker does not use data volume, so its volume count should be zero
        // To be backward-compatible, we only check max limit and allow the min limit to be zero for IDBroker
        if (!config.possibleNumberValues().isEmpty()) {
            if (!config.possibleNumberValues().contains(value.getVolumeCount())) {
                validationBuilder.error(String.format("Allowed volume count(s) for '%s': %s", vmType.value(), config.possibleNumberValues()));
            }
        } else if (value.getVolumeCount() > config.maximumNumber()) {
            validationBuilder.error(String.format("Max allowed volume count for '%s': %s", vmType.value(), config.maximumNumber()));
        } else if (shouldValidateBasedOnGroupName(instanceGroup, stack) &&
                value.getVolumeCount() < config.minimumNumber()) {
            validationBuilder.error(String.format("Min allowed volume count for '%s': %s", vmType.value(), config.minimumNumber()));
        }
    }

    private boolean isSdxComputeInstanceHostGroup(InstanceGroup instanceGroup, StackType stackType) {
        return SDX_COMPUTE_INSTANCES.contains(instanceGroup.getGroupName().toLowerCase(Locale.ROOT)) &&
                datalakeOrTemplate(stackType);
    }

    private static boolean datalakeOrTemplate(StackType stackType) {
        return StackType.DATALAKE.equals(stackType) || StackType.TEMPLATE.equals(stackType);
    }

    private void validateVolumeSize(VolumeTemplate value, VmType vmType, VolumeParameterType volumeParameterType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount()) && volumeParameterType != null) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                validateVolumeSizeInParameterConfig(config, value, vmType, validationBuilder, instanceGroup, stack);
            } else {
                validationBuilder.error(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
        }
    }

    private void validateVolumeSizeInParameterConfig(VolumeParameterConfig config, VolumeTemplate value, VmType vmType,
            ValidationResult.ValidationResultBuilder validationBuilder, InstanceGroup instanceGroup, Stack stack) {
        // IDBroker does not use data volume, so its volume size should be zero
        // To be backward-compatible, we only check max limit and allow the min limit to be zero for IDBroker
        if (!config.possibleSizeValues().isEmpty()) {
            if (!config.possibleSizeValues().contains(value.getVolumeSize())) {
                validationBuilder.error(String.format("Allowed volume size(s) for '%s': %s in case of %s volumes and the current size is %s.",
                        vmType.value(),
                        config.possibleSizeValues(),
                        value.getVolumeType(),
                        value.getVolumeSize()));
            }
        } else if (value.getVolumeSize() > config.maximumSize()) {
            validationBuilder.error(String.format("Max allowed volume size for '%s': %s GB in case of %s volumes and the current size is %s GB.",
                    vmType.value(),
                    config.maximumSize(),
                    value.getVolumeType(),
                    value.getVolumeSize()));
        } else if (shouldValidateBasedOnGroupName(instanceGroup, stack)
                && value.getVolumeSize() < config.minimumSize()) {
            validationBuilder.error(String.format("Min allowed volume size for '%s': %s GB in case of %s volumes and the current size is %s GB.",
                    vmType.value(),
                    config.minimumSize(),
                    value.getVolumeType(),
                    value.getVolumeSize()));
        }
    }

    private boolean shouldValidateBasedOnGroupName(InstanceGroup instanceGroup, Stack stack) {
        return !(isIDBrokerInstanceGroup(instanceGroup, stack)
                || isSdxComputeInstanceHostGroup(instanceGroup, stack.getType())
                || isCoordinatorAndExecutorInstanceGroup(stack.getBlueprint().getBlueprintJsonText()));
    }

    private boolean needToCheckVolume(VolumeParameterType volumeParameterType, Object value) {
        return volumeParameterType != null && volumeParameterType != VolumeParameterType.EPHEMERAL || value != null;
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
