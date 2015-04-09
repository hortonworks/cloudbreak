package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.FailurePolicyJson;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

@Component
public class FailurePolicyToJsonConverter extends AbstractConversionServiceAwareConverter<FailurePolicy, FailurePolicyJson> {
    @Override
    public FailurePolicyJson convert(FailurePolicy entity) {
        FailurePolicyJson json = new FailurePolicyJson();
        json.setAdjustmentType(entity.getAdjustmentType());
        json.setId(entity.getId());
        json.setThreshold(entity.getThreshold() == null ? 0 : entity.getThreshold());
        return json;
    }
}
