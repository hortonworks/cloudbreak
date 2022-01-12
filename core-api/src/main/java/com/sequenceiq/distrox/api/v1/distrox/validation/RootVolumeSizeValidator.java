package com.sequenceiq.distrox.api.v1.distrox.validation;

import java.util.Optional;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.validation.volume.RootVolumeSizeProvider;

@Component
public class RootVolumeSizeValidator implements ConstraintValidator<ValidRootVolumeSize, InstanceGroupV1Request> {

    @Inject
    private Optional<RootVolumeSizeProvider> rootVolumeSizeProvider;

    @Override
    public boolean isValid(InstanceGroupV1Request value, ConstraintValidatorContext context) {
        if (rootVolumeSizeProvider.isEmpty()) {
            throw new IllegalArgumentException("Please add an implementation to the " + RootVolumeSizeProvider.class.getName());
        }
        String cloudPlatform = "unknown";
        InstanceTemplateV1Request template = value.getTemplate();
        if (template == null || template.getRootVolume() == null || template.getRootVolume().getSize() == null) {
            return true;
        }
        if (template.getCloudPlatform() != null) {
            cloudPlatform = template.getCloudPlatform().name();
        }
        context.disableDefaultConstraintViolation();
        int rootVolumeSize = rootVolumeSizeProvider.get().getForPlatform(cloudPlatform);
        if (template.getRootVolume().getSize() < rootVolumeSize) {
            context
                    .buildConstraintViolationWithTemplate(StringUtils.capitalize(value.getName()) + " root volume (" + template.getRootVolume().getSize()
                            + "GB) couldn't be less than " + rootVolumeSize + "GB")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }

    public void setRootVolumeSizeProvider(Optional<RootVolumeSizeProvider> rootVolumeSizeProvider) {
        this.rootVolumeSizeProvider = rootVolumeSizeProvider;
    }
}
