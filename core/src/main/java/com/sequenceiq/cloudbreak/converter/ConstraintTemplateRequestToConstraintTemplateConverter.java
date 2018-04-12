package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest;
import com.sequenceiq.cloudbreak.api.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConstraintTemplateRequestToConstraintTemplateConverter
        extends AbstractConversionServiceAwareConverter<ConstraintTemplateRequest, ConstraintTemplate> {
    @Override
    public ConstraintTemplate convert(ConstraintTemplateRequest source) {
        ConstraintTemplate constraintTemplate = new ConstraintTemplate();
        constraintTemplate.setCpu(source.getCpu());
        constraintTemplate.setMemory(source.getMemory());
        constraintTemplate.setDisk(source.getDisk());
        constraintTemplate.setOrchestratorType(source.getOrchestratorType());
        constraintTemplate.setName(source.getName());
        constraintTemplate.setDescription(source.getDescription());
        constraintTemplate.setStatus(ResourceStatus.USER_MANAGED);
        return constraintTemplate;
    }

}
