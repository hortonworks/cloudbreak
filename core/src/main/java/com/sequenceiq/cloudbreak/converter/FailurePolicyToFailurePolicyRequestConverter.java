package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyRequest;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

@Component
public class FailurePolicyToFailurePolicyRequestConverter extends AbstractConversionServiceAwareConverter<FailurePolicy, FailurePolicyRequest> {

    @Override
    public FailurePolicyRequest convert(FailurePolicy json) {
        FailurePolicyRequest stackFailurePolicy = new FailurePolicyRequest();
        stackFailurePolicy.setAdjustmentType(json.getAdjustmentType());
        stackFailurePolicy.setThreshold(json.getThreshold());
        return stackFailurePolicy;
    }
}
