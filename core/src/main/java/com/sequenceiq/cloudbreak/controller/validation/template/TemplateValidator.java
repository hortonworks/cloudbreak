package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Collection;
import java.util.Map;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

public class TemplateValidator implements ConstraintValidator<ValidTemplate, TemplateRequest> {

    private int maxCount;
    private int minCount;
    private int maxSize;
    private int minSize;

    @Inject
    private CloudParameterService cloudParameterService;

    @Override
    public void initialize(ValidTemplate constraintAnnotation) {
        this.maxCount = constraintAnnotation.maxCount();
        this.minCount = constraintAnnotation.minCount();
        this.maxSize = constraintAnnotation.maxSize();
        this.minSize = constraintAnnotation.minSize();
    }

    @Override
    public boolean isValid(TemplateRequest value, ConstraintValidatorContext context) {
        boolean valid = true;
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
                valid = false;
                String message = String.format("The '%s' instance type isn't supported by '%s' platform", tempVmType.value(), platform.value());
                addParameterConstraintViolation(context, "instanceType", message);
            }
        }

        if (valid) {
            valid = validateVolume(value, context, vmType, platform);
        }
        return valid;
    }

    private boolean validateVolume(TemplateRequest value, ConstraintValidatorContext context, VmType vmType, Platform platform) {
        boolean valid = validateVolumeType(context, value, platform);
        if (valid) {
            if (isEphemeralVolume(value)) {
                valid = validateEphemeralVolumeParams(context, value, vmType);
            } else {
                valid = isValidVolumeSpecification(context, value);
            }
        }
        return valid;
    }

    private boolean validateVolumeType(ConstraintValidatorContext context, TemplateRequest value, Platform platform) {
        boolean valid = true;
        DiskType diskType = DiskType.diskType(value.getVolumeType());
        Map<Platform, Collection<DiskType>> diskTypes = cloudParameterService.getDiskTypes().getDiskTypes();
        if (diskTypes.containsKey(platform) && !diskTypes.get(platform).isEmpty()) {
            valid = diskTypes.get(platform).contains(diskType);
        }
        if (!valid) {
            String message = String.format("The '%s' platform does not support '%s' volume type", platform.value(), diskType.value());
            addParameterConstraintViolation(context, "volumeType", message);
        }
        return valid;
    }

    private boolean isEphemeralVolume(TemplateRequest value) {
        return "ephemeral".equalsIgnoreCase(value.getVolumeType());
    }

    private boolean validateEphemeralVolumeParams(ConstraintValidatorContext context, TemplateRequest value, VmType vmType) {
        boolean valid = true;
        if (vmType != null) {
            int maxVolume = vmType.getMetaData().maxEphemeralVolumeCount();
            int desiredVolumeCount = value.getVolumeCount() == null ? 0 : value.getVolumeCount();
            if (maxVolume == 0) {
                valid = false;
                String message = String.format("The '%s' instance type does not support 'Ephemeral' volume type", vmType.value());
                addParameterConstraintViolation(context, "volumeCount", message);
            }
            if (valid && desiredVolumeCount > maxVolume) {
                valid = false;
                addParameterConstraintViolation(context, "volumeCount", String.format("Max allowed ephemeral volume for '%s': %s", vmType.value(), maxVolume));
            }
            if (valid && desiredVolumeCount < minCount) {
                valid = false;
                addParameterConstraintViolation(context, "volumeCount", "Min volume count: " + minCount);
            }
        }
        return valid;
    }

    private boolean isValidVolumeSpecification(ConstraintValidatorContext context, TemplateRequest value) {
        boolean valid = true;
        if (!isCountInRange(value.getVolumeCount())) {
            addParameterConstraintViolation(context, "volumeCount", String.format("Volume count must be in range [%s-%s] ", minCount, maxCount));
            valid = false;
        }
        if (valid) {
            if (!isSizeInRange(value.getVolumeSize())) {
                addParameterConstraintViolation(context, "volumeSize", String.format("Volume size must be in range [%s-%s] ", minSize, maxSize));
                valid = false;
            }
        }
        return valid;
    }

    private boolean isCountInRange(Integer volumeCount) {
        return isInRange(minCount, maxCount, volumeCount);
    }

    private boolean isSizeInRange(Integer volumeSize) {
        return isInRange(minSize, maxSize, volumeSize);
    }

    private boolean isInRange(int min, int max, Integer value) {
        return value != null && value >= min && value <= max;
    }

    private void addParameterConstraintViolation(ConstraintValidatorContext context, String key, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(key)
                .addConstraintViolation();
    }
}
