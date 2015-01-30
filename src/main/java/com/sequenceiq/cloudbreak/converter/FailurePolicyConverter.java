package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.FailurePolicyJson;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

@Component
public class FailurePolicyConverter extends AbstractConverter<FailurePolicyJson, FailurePolicy> {
    @Override
    public FailurePolicyJson convert(FailurePolicy entity) {
        FailurePolicyJson json = new FailurePolicyJson();
        json.setAdjustmentType(entity.getAdjustmentType());
        json.setId(entity.getId());
        json.setThreshold(entity.getThreshold());
        return json;
    }

    @Override
    public FailurePolicy convert(FailurePolicyJson json) {
        FailurePolicy stackFailurePolicy = new FailurePolicy();
        stackFailurePolicy.setAdjustmentType(json.getAdjustmentType());
        stackFailurePolicy.setId(json.getId());
        stackFailurePolicy.setThreshold(json.getThreshold());
        return stackFailurePolicy;
    }
}
