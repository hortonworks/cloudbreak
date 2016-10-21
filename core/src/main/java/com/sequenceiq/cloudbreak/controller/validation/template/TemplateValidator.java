package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.VmTypeMeta;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterConfig;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
public class TemplateValidator {

    @Inject
    private CloudParameterService cloudParameterService;

    private Supplier<Map<Platform, Collection<VmType>>> virtualMachines =
            Suppliers.memoize(() -> cloudParameterService.getVmtypes(true).getVirtualMachines());
    private Supplier<Map<Platform, Map<String, VolumeParameterType>>> diskMappings =
            Suppliers.memoize(() -> cloudParameterService.getDiskTypes().getDiskMappings());

    public void validateTemplateRequest(TemplateRequest value) {
        VmType vmType = null;
        VolumeParameterType volumeParameterType = null;
        Platform platform = Platform.platform(value.getCloudPlatform());
        Map<Platform, Collection<VmType>> machines = virtualMachines.get();
        if (machines.containsKey(platform) && !machines.get(platform).isEmpty()) {
            for (VmType type : machines.get(platform)) {
                if (type.value().equals(value.getInstanceType())) {
                    vmType = type;
                }
            }
            if (vmType == null) {
                throw new BadRequestException(
                        String.format("The '%s' instance type isn't supported by '%s' platform", value.getInstanceType(), platform.value()));
            }
        }
        Map<Platform, Map<String, VolumeParameterType>> disks = diskMappings.get();
        if (disks.containsKey(platform) && !disks.get(platform).isEmpty()) {
            Map<String, VolumeParameterType> map = disks.get(platform);
            volumeParameterType = map.get(value.getVolumeType());
            if (volumeParameterType == null) {
                throw new BadRequestException(
                        String.format("The '%s' volume type isn't supported by '%s' platform", value.getVolumeType(), platform.value()));
            }
        }

        validateVolume(value, vmType, platform, volumeParameterType);
    }

    private void validateVolume(TemplateRequest value, VmType vmType, Platform platform, VolumeParameterType volumeParameterType) {
        validateVolumeType(value, platform);
        validateVolumeCount(value, vmType, volumeParameterType);
        validateVolumeSize(value, vmType, volumeParameterType);
        validateMaximumVolumeSize(value, vmType);
    }

    private void validateMaximumVolumeSize(TemplateRequest value, VmType vmType) {
        if (vmType != null) {
            String maxSize = vmType.getMetaDataValue(VmTypeMeta.MAXIMUM_PERSISTENT_DISKS_SIZE_GB);
            if (maxSize != null) {
                int fullSize = value.getVolumeSize() * value.getVolumeCount();
                if (Integer.valueOf(maxSize) < fullSize) {
                    throw new BadRequestException(
                            String.format("The %s platform does not support %s Gb full volume size. The maximum size of disks could be %s Gb.",
                                    value.getCloudPlatform(), fullSize, maxSize));
                }
            }
        }
    }

    private void validateVolumeType(TemplateRequest value, Platform platform) {
        DiskType diskType = DiskType.diskType(value.getVolumeType());
        Map<Platform, Collection<DiskType>> diskTypes = cloudParameterService.getDiskTypes().getDiskTypes();
        if (diskTypes.containsKey(platform) && !diskTypes.get(platform).isEmpty()) {
            if (!diskTypes.get(platform).contains(diskType)) {
                throw new BadRequestException(String.format("The '%s' platform does not support '%s' volume type", platform.value(), diskType.value()));
            }
        }
    }

    private void validateVolumeCount(TemplateRequest value, VmType vmType, VolumeParameterType volumeParameterType) {
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

    private void validateVolumeSize(TemplateRequest value, VmType vmType, VolumeParameterType volumeParameterType) {
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
