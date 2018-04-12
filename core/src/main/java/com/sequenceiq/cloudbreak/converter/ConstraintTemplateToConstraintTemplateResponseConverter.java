package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse;
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate;
import org.springframework.stereotype.Component;

@Component
public class ConstraintTemplateToConstraintTemplateResponseConverter
        extends AbstractConversionServiceAwareConverter<ConstraintTemplate, ConstraintTemplateResponse> {

    @Override
    public ConstraintTemplateResponse convert(ConstraintTemplate source) {
        ConstraintTemplateResponse constraintTemplateResponse = new ConstraintTemplateResponse();
        constraintTemplateResponse.setId(source.getId());
        constraintTemplateResponse.setName(source.getName());
        constraintTemplateResponse.setPublicInAccount(source.isPublicInAccount());
        constraintTemplateResponse.setDescription(source.getDescription() == null ? "" : source.getDescription());
        constraintTemplateResponse.setCpu(source.getCpu());
        constraintTemplateResponse.setMemory(source.getMemory());
        constraintTemplateResponse.setDisk(source.getDisk());
        constraintTemplateResponse.setOrchestratorType(source.getOrchestratorType());

        return constraintTemplateResponse;
    }
}
