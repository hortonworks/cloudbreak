package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.domain.Constraint;
import org.springframework.stereotype.Component;

@Component
public class ConstraintRequestToConstraintConverter extends AbstractConversionServiceAwareConverter<ConstraintJson, Constraint> {

    @Override
    public Constraint convert(ConstraintJson source) {
        Constraint constraint = new Constraint();
        constraint.setHostCount(source.getHostCount());
        return constraint;
    }
}
