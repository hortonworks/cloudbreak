package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

@Component
public class TemplateValidator {
    private static final int MAXCOUNT = 24;
    private static final int MINCOUNT = 1;
    // TODO needs a proper validation
    private static final int MAXSIZE = 2000;
    private static final int MINSIZE = 10;

    @Inject
    private CloudParameterService cloudParameterService;

    public void validateTemplateRequest(TemplateRequest value) {
        VmType vmType = null;
        Platform platform = Platform.platform(value.getCloudPlatform());
        Map<Platform, Collection<VmType>> virtualMachines = cloudParameterService.getVmtypes().getVirtualMachines();
        VmType tempVmType = VmType.vmType(value.getInstanceType());

        if (virtualMachines.containsKey(platform) && !virtualMachines.get(platform).isEmpty()) {
            if (virtualMachines.get(platform).contains(tempVmType)) {
                //get VmType with metadata
                for (VmType type : virtualMachines.get(platform)) {
                    if (type.equals(tempVmType)) {
                        vmType = type;
                    }
                }
            } else {
                throw new BadRequestException(String.format("The '%s' instance type isn't supported by '%s' platform", tempVmType.value(), platform.value()));
            }
        }

        validateVolume(value, vmType, platform);
    }

    private void validateVolume(TemplateRequest value, VmType vmType, Platform platform) {
        validateVolumeType(value, platform);
        if (isEphemeralVolume(value)) {
            validateEphemeralVolumeParams(value, vmType);
        } else {
            isValidVolumeSpecification(value);
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

    private boolean isEphemeralVolume(TemplateRequest value) {
        return "ephemeral".equalsIgnoreCase(value.getVolumeType());
    }

    private void validateEphemeralVolumeParams(TemplateRequest value, VmType vmType) {
        if (vmType != null) {
            int maxVolume = vmType.getMetaData().maxEphemeralVolumeCount();
            int desiredVolumeCount = value.getVolumeCount() == null ? 0 : value.getVolumeCount();
            if (maxVolume == 0) {
                throw new BadRequestException(String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value()));
            }
            if (desiredVolumeCount > maxVolume) {
                throw new BadRequestException(String.format("Max allowed ephemeral volume for '%s': %s", vmType.value(), maxVolume));
            }
            if (desiredVolumeCount < MINCOUNT) {
                throw new BadRequestException(String.format("Min volume count: " + MINCOUNT));
            }
        }
    }

    private void isValidVolumeSpecification(TemplateRequest value) {
        if (!isCountInRange(value.getVolumeCount())) {
            throw new BadRequestException(String.format("Volume count must be in range [%s-%s] ", MINCOUNT, MAXCOUNT));
        }
        if (!isSizeInRange(value.getVolumeSize())) {
            throw new BadRequestException(String.format("Volume size must be in range [%s-%s] ", MINSIZE, MAXSIZE));
        }
    }

    private boolean isCountInRange(Integer volumeCount) {
        return isInRange(MINCOUNT, MAXCOUNT, volumeCount);
    }

    private boolean isSizeInRange(Integer volumeSize) {
        return isInRange(MINSIZE, MAXSIZE, volumeSize);
    }

    private boolean isInRange(int min, int max, Integer value) {
        return value != null && value >= min && value <= max;
    }
}
