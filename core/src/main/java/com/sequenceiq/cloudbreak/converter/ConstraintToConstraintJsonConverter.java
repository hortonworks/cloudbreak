package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ConstraintJson;
import com.sequenceiq.cloudbreak.domain.Constraint;

@Component
public class ConstraintToConstraintJsonConverter extends AbstractConversionServiceAwareConverter<Constraint, ConstraintJson> {

    @Override
    public ConstraintJson convert(Constraint source) {
        ConstraintJson constraintJson = new ConstraintJson();
        if (source.getInstanceGroup() != null) {
            constraintJson.setInstanceGroupName(source.getInstanceGroup().getGroupName());
        }
        constraintJson.setHostCount(source.getHostCount());
        return constraintJson;
    }
}
