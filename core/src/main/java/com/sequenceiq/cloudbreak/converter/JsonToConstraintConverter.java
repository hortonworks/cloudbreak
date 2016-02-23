package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Constraint;
import com.sequenceiq.cloudbreak.api.model.ConstraintJson;

@Component
public class JsonToConstraintConverter extends AbstractConversionServiceAwareConverter<ConstraintJson, Constraint> {

    @Override
    public Constraint convert(ConstraintJson source) {
        Constraint constraint = new Constraint();
        constraint.setHostCount(source.getHostCount());
        return constraint;
    }
}
