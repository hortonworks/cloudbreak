package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.cloudbreak.validation.VolumeParameterConstants;

@Component
public class InstanceTemplateValidator implements Validator<Template> {

    static final String INVALID_NUMBER_OF_VOLUMES_MESSAGE_PATTERN = "Maximum '%d' volumes could be configured for an instance. "
            + "Please modify the instance template with name: '%s', it has '%d' volumes configured!";

    @Override
    public ValidationResult validate(Template subject) {
        ValidationResultBuilder resultBuilder = ValidationResult.builder();
        if (Objects.isNull(subject)) {
            resultBuilder.error("Template cannot be null in the instance group request.");
        } else {
            resultBuilder.ifError(() -> subject.getRootVolumeSize() != null
                            && subject.getRootVolumeSize() < 1,
                    "Root volume size cannot be smaller than 1 gigabyte.");
        }
        validateNumberOfVolumes(subject, resultBuilder);
        return resultBuilder.build();
    }

    private void validateNumberOfVolumes(Template subject, ValidationResultBuilder resultBuilder) {
        if (CollectionUtils.isNotEmpty(subject.getVolumeTemplates())) {
            long numberOfVolumesPerInstanceTemplate = subject.getVolumeTemplates()
                    .stream()
                    .collect(Collectors.summarizingInt(VolumeTemplate::getVolumeCount))
                    .getSum();
            if (numberOfVolumesPerInstanceTemplate > VolumeParameterConstants.MAXIMUM_NUMBER_OF_VOLUMES) {
                String errorMsg = String.format(INVALID_NUMBER_OF_VOLUMES_MESSAGE_PATTERN,
                        VolumeParameterConstants.MAXIMUM_NUMBER_OF_VOLUMES,
                        subject.getName(),
                        numberOfVolumesPerInstanceTemplate);
                resultBuilder.error(errorMsg);
            }
        }
    }
}
