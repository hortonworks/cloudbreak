package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import org.springframework.stereotype.Component;

@Component
public class FailurePolicyRequestToFailurePolicyConverter extends AbstractConversionServiceAwareConverter<FailurePolicyRequest, FailurePolicy> {

    @Override
    public FailurePolicy convert(FailurePolicyRequest json) {
        FailurePolicy stackFailurePolicy = new FailurePolicy();
        stackFailurePolicy.setAdjustmentType(json.getAdjustmentType());
        stackFailurePolicy.setThreshold(json.getThreshold() == null ? Long.valueOf(0L) : json.getThreshold());
        return stackFailurePolicy;
    }
}
