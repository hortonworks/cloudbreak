package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.FailurePolicyJson;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

@Component
public class JsonToFailurePolicyConverter extends AbstractConversionServiceAwareConverter<FailurePolicyJson, FailurePolicy> {
    @Override
    public FailurePolicy convert(FailurePolicyJson json) {
        FailurePolicy stackFailurePolicy = new FailurePolicy();
        stackFailurePolicy.setAdjustmentType(json.getAdjustmentType());
        stackFailurePolicy.setId(json.getId());
        stackFailurePolicy.setThreshold(json.getThreshold() == null ? 0 : json.getThreshold());
        return stackFailurePolicy;
    }
}
