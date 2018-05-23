package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.domain.Constraint;

@Component
public class ConstraintRequestToConstraintConverter extends AbstractConversionServiceAwareConverter<ConstraintJson, Constraint> {

    @Override
    public Constraint convert(ConstraintJson source) {
        return new Constraint();
    }
}
