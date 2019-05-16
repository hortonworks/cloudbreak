package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.controller.validation.LocationService;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
public class TemplateValidator {

    @Inject
    private CloudParameterService cloudParameterService;

    @Inject
    private LocationService locationService;

    private final Supplier<Map<Platform, Map<String, VolumeParameterType>>> diskMappings =
            Suppliers.memoize(() -> cloudParameterService.getDiskTypes().getDiskMappings());

    private final Supplier<Map<Platform, PlatformParameters>> platformParameters =
            Suppliers.memoize(() -> cloudParameterService.getPlatformParameters());

    public void validateTemplateRequest(Credential credential, Template value, String region, String availabilityZone, String variant) {
        CloudVmTypes cloudVmTypes = cloudParameterService.getVmTypesV2(credential, region, variant, new HashMap<>());

        if (StringUtils.isEmpty(value.getInstanceType())) {
            validateCustomInstanceType(value);
        } else {
            VmType vmType = null;
            Platform platform = Platform.platform(value.cloudPlatform());
            Map<String, Set<VmType>> machines = cloudVmTypes.getCloudVmResponses();
            String locationString = locationService.location(region, availabilityZone);
            if (machines.containsKey(locationString) && !machines.get(locationString).isEmpty()) {
                for (VmType type : machines.get(locationString)) {
                    if (type.value().equals(value.getInstanceType())) {
                        vmType = type;
                        break;
                    }
                }
                if (vmType == null) {
                    throw new BadRequestException(
                            String.format("The '%s' instance type isn't supported by '%s' platform", value.getInstanceType(), platform.value()));
                }
            }

            validateVolumeTemplates(value, vmType, platform);
            validateMaximumVolumeSize(value, vmType);
        }
    }

    private void validateVolumeTemplates(Template value, VmType vmType, Platform platform) {
        for (VolumeTemplate volumeTemplate : value.getVolumeTemplates()) {
            VolumeParameterType volumeParameterType = null;
            Map<Platform, Map<String, VolumeParameterType>> disks = diskMappings.get();
            if (disks.containsKey(platform) && !disks.get(platform).isEmpty()) {
                Map<String, VolumeParameterType> map = disks.get(platform);
                volumeParameterType = map.get(volumeTemplate.getVolumeType());
                if (volumeParameterType == null) {
                    throw new BadRequestException(
                            String.format("The '%s' volume type isn't supported by '%s' platform", volumeTemplate.getVolumeType(), platform.value()));
                }
            }

            validateVolume(volumeTemplate, vmType, platform, volumeParameterType);
        }
    }

    private void validateCustomInstanceType(Template template) {
        Map<String, Object> params = template.getAttributes().getMap();
        Platform platform = Platform.platform(template.cloudPlatform());
        PlatformParameters pps = platformParameters.get().get(platform);
        if (pps != null) {
            Boolean customInstanceType = pps.specialParameters().getSpecialParameters().get(PlatformParametersConsts.CUSTOM_INSTANCETYPE);
            if (BooleanUtils.isTrue(customInstanceType)) {
                if (params.get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS) == null
                        || params.get(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY) == null) {
                    throw new BadRequestException(String.format("Missing 'cpus' or 'memory' param for custom instancetype on %s platform",
                            template.cloudPlatform()));
                }
            } else {
                throw new BadRequestException(String.format("Custom instancetype is not supported on %s platform", template.cloudPlatform()));
            }
        }
    }

    private void validateVolume(VolumeTemplate value, VmType vmType, Platform platform, VolumeParameterType volumeParameterType) {
        validateVolumeType(value, platform);
        validateVolumeCount(value, vmType, volumeParameterType);
        validateVolumeSize(value, vmType, volumeParameterType);
    }

    private void validateMaximumVolumeSize(Template value, VmType vmType) {
        if (vmType != null) {
            Object maxSize = vmType.getMetaDataValue(VmTypeMeta.MAXIMUM_PERSISTENT_DISKS_SIZE_GB);
            if (maxSize != null) {
                int fullSize = value.getVolumeTemplates().stream().mapToInt(volume -> volume.getVolumeSize() * volume.getVolumeCount()).sum();
                if (Integer.parseInt(maxSize.toString()) < fullSize) {
                    throw new BadRequestException(
                            String.format("The %s platform does not support %s Gb full volume size. The maximum size of disks could be %s Gb.",
                                    value.cloudPlatform(), fullSize, maxSize));
                }
            }
        }
    }

    private void validateVolumeType(VolumeTemplate value, Platform platform) {
        DiskType diskType = DiskType.diskType(value.getVolumeType());
        Map<Platform, Collection<DiskType>> diskTypes = cloudParameterService.getDiskTypes().getDiskTypes();
        if (diskTypes.containsKey(platform) && !diskTypes.get(platform).isEmpty()) {
            if (!diskTypes.get(platform).contains(diskType)) {
                throw new BadRequestException(String.format("The '%s' platform does not support '%s' volume type", platform.value(), diskType.value()));
            }
        }
    }

    private void validateVolumeCount(VolumeTemplate value, VmType vmType, VolumeParameterType volumeParameterType) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount())) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                if (value.getVolumeCount() > config.maximumNumber()) {
                    throw new BadRequestException(String.format("Max allowed volume count for '%s': %s", vmType.value(), config.maximumNumber()));
                } else if (value.getVolumeCount() < config.minimumNumber()) {
                    throw new BadRequestException(String.format("Min allowed volume count for '%s': %s", vmType.value(), config.minimumNumber()));
                }
            } else {
                throw new BadRequestException(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
        }
    }

    private void validateVolumeSize(VolumeTemplate value, VmType vmType, VolumeParameterType volumeParameterType) {
        if (vmType != null && needToCheckVolume(volumeParameterType, value.getVolumeCount())) {
            VolumeParameterConfig config = vmType.getVolumeParameterbyVolumeParameterType(volumeParameterType);
            if (config != null) {
                if (value.getVolumeSize() > config.maximumSize()) {
                    throw new BadRequestException(String.format("Max allowed volume size for '%s': %s", vmType.value(), config.maximumSize()));
                } else if (value.getVolumeSize() < config.minimumSize()) {
                    throw new BadRequestException(String.format("Min allowed volume size for '%s': %s", vmType.value(), config.minimumSize()));
                }
            } else {
                throw new BadRequestException(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
        }
    }

    private boolean needToCheckVolume(VolumeParameterType volumeParameterType, Object value) {
        return volumeParameterType != VolumeParameterType.EPHEMERAL || value != null;
    }
}
