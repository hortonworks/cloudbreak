package com.sequenceiq.cloudbreak.controller.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.domain.AwsInstanceType;
import com.sequenceiq.cloudbreak.domain.AwsVolumeType;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

public class VolumeCountValidator implements ConstraintValidator<ValidVolume, TemplateJson> {

    private int maxCount;
    private int minCount;
    private int maxSize;
    private int minSize;

    @Override
    public void initialize(ValidVolume constraintAnnotation) {
        this.maxCount = constraintAnnotation.maxCount();
        this.minCount = constraintAnnotation.minCount();
        this.maxSize = constraintAnnotation.maxSize();
        this.minSize = constraintAnnotation.minSize();
    }

    @Override
    public boolean isValid(TemplateJson value, ConstraintValidatorContext context) {
        boolean valid;
        if (isAwsTemplate(value)) {
            if (isEphemeralVolume(value)) {
                valid = validateEphemeralParams(context, value);
            } else {
                valid = isVolumeTypeSpecified(context, value);
                if (valid) {
                    valid = isValidVolumeSpecification(context, value);
                }
            }
        } else {
            valid = isValidVolumeSpecification(context, value);
        }
        return valid;
    }

    private boolean isAwsTemplate(TemplateJson value) {
        return CloudPlatform.AWS.equals(value.getCloudPlatform());
    }

    private boolean isEphemeralVolume(TemplateJson value) {
        return AwsVolumeType.Ephemeral.name().equalsIgnoreCase((String) value.getParameters().get(AwsTemplateParam.VOLUME_TYPE.getName()));
    }

    private boolean validateEphemeralParams(ConstraintValidatorContext context, TemplateJson value) {
        boolean valid = true;
        String instanceType = (String) value.getParameters().get(AwsTemplateParam.INSTANCE_TYPE.getName());
        int maxVolume = AwsInstanceType.valueOf(instanceType).getEphemeralVolumes();
        int desiredVolumeCount = value.getVolumeCount() == null ? 0 : value.getVolumeCount();
        if (maxVolume == 0) {
            valid = false;
            addParameterConstraintViolation(context, "volumeCount", String.format("'%s' instance type does not support 'Ephemeral' volume type", instanceType));
        }
        if (valid && desiredVolumeCount > maxVolume) {
            valid = false;
            addParameterConstraintViolation(context, "volumeCount", String.format("Max allowed ephemeral volume for '%s': %s", instanceType, maxVolume));
        }
        if (valid && desiredVolumeCount < minCount) {
            valid = false;
            addParameterConstraintViolation(context, "volumeCount", "Min volume count: " + minCount);
        }
        return valid;
    }

    private boolean isValidVolumeSpecification(ConstraintValidatorContext context, TemplateJson value) {
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

    private boolean isVolumeTypeSpecified(ConstraintValidatorContext context, TemplateJson value) {
        boolean specified = value.getParameters().get(AwsTemplateParam.VOLUME_TYPE.getName()) != null;
        if (!specified) {
            addParameterConstraintViolation(context, "volumeType", "Volume type must be specified");
        }
        return specified;
    }

    private void addParameterConstraintViolation(ConstraintValidatorContext context, String key, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(key)
                .addConstraintViolation();
    }
}
