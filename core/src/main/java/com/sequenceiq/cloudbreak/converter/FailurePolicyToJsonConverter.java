package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;

@Component
public class FailurePolicyToJsonConverter extends AbstractConversionServiceAwareConverter<FailurePolicy, FailurePolicyResponse> {
    @Override
    public FailurePolicyResponse convert(FailurePolicy entity) {
        FailurePolicyResponse json = new FailurePolicyResponse();
        json.setAdjustmentType(entity.getAdjustmentType());
        json.setId(entity.getId());
        json.setThreshold(entity.getThreshold() == null ? 0 : entity.getThreshold());
        return json;
    }
}
