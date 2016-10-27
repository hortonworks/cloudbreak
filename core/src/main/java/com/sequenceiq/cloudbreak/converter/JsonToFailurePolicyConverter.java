package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

@Component
public class JsonToFailurePolicyConverter extends AbstractConversionServiceAwareConverter<FailurePolicyRequest, FailurePolicy> {
    @Override
    public FailurePolicy convert(FailurePolicyRequest json) {
        FailurePolicy stackFailurePolicy = new FailurePolicy();
        stackFailurePolicy.setAdjustmentType(json.getAdjustmentType());
        stackFailurePolicy.setThreshold(json.getThreshold() == null ? 0 : json.getThreshold());
        return stackFailurePolicy;
    }
}
