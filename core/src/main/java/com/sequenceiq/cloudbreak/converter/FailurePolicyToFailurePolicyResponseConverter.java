package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.FailurePolicyResponse;
import com.sequenceiq.cloudbreak.domain.FailurePolicy;
import org.springframework.stereotype.Component;

@Component
public class FailurePolicyToFailurePolicyResponseConverter extends AbstractConversionServiceAwareConverter<FailurePolicy, FailurePolicyResponse> {
    @Override
    public FailurePolicyResponse convert(FailurePolicy entity) {
        FailurePolicyResponse json = new FailurePolicyResponse();
        json.setAdjustmentType(entity.getAdjustmentType());
        json.setId(entity.getId());
        json.setThreshold(entity.getThreshold() == null ? 0 : entity.getThreshold());
        return json;
    }
}
