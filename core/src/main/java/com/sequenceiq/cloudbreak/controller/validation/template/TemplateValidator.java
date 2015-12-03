package com.sequenceiq.cloudbreak.controller.validation.template;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import java.util.Collection;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.aws.AwsPlatformParameters;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterService;

public class TemplateValidator implements ConstraintValidator<ValidTemplate, TemplateRequest> {
    private static final String ENCRYPTED_ATTRIBUTE_KEY = "encrypted";
    private static final String SPOT_PRICE_ATTRIBUTE_KEY = "spotPrice";

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
        boolean valid = false;
        Platform platform = Platform.platform(value.getCloudPlatform().name());
        VmType vmType = getAndValidateVmType(value, context, platform);
        if (vmType != null) {
            valid = validateVolume(value, context, vmType, platform);
        }
        return valid;
    }

    private VmType getAndValidateVmType(TemplateRequest value, ConstraintValidatorContext context, Platform platform) {
        VmType result = null;
        Map<Platform, Collection<VmType>> virtualMachines = cloudParameterService.getVmtypes().getVirtualMachines();
        VmType tempVmType = VmType.vmType(value.getInstanceType());
        if (virtualMachines.containsKey(platform) && virtualMachines.get(platform).contains(tempVmType)) {
            //get VmType with metadata
            for (VmType vmType : virtualMachines.get(platform)) {
                if (vmType.equals(tempVmType)) {
                    result = vmType;
                }
            }
        } else {
            String message = String.format("The '%s' instance type isn't supported by '%s' platform", tempVmType.value(), platform.value());
            addParameterConstraintViolation(context, "instanceType", message);
        }
        return result;
    }

    private boolean validateVolume(TemplateRequest value, ConstraintValidatorContext context, VmType vmType, Platform platform) {
        boolean valid = validateVolumeType(context, value, platform);
        if (valid) {
            if (isAwsTemplate(value) && isEphemeralVolume(value)) {
                valid = validateEphemeralVolumeParams(context, value, vmType);
            } else {
                valid = isValidVolumeSpecification(context, value);
            }
        }
        return valid;
    }

    private boolean validateVolumeType(ConstraintValidatorContext context, TemplateRequest value, Platform platform) {
        DiskType diskType = DiskType.diskType(value.getVolumeType());
        Map<Platform, Collection<DiskType>> diskTypes = cloudParameterService.getDiskTypes().getDiskTypes();
        boolean valid = diskTypes.containsKey(platform) && diskTypes.get(platform).contains(diskType);
        if (!valid) {
            String message = String.format("The '%s' platform does not support '%s' volume type", platform.value(), diskType.value());
            addParameterConstraintViolation(context, "volumeType", message);
        }
        return valid;
    }

    private boolean isAwsTemplate(TemplateRequest value) {
        return CloudPlatform.AWS.equals(value.getCloudPlatform());
    }

    private boolean isEphemeralVolume(TemplateRequest value) {
        return AwsPlatformParameters.AWS_EPHEMERAL_DISK_TYPE.equalsIgnoreCase(value.getVolumeType());
    }

    private boolean validateEphemeralVolumeParams(ConstraintValidatorContext context, TemplateRequest value, VmType vmType) {
        boolean valid = true;
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
