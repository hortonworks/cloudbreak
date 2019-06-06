package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;

public class GetStackParamValidationResult extends CloudPlatformResult {
    private List<StackParamValidation> stackParamValidations;

    public GetStackParamValidationResult(Long resourceId, List<StackParamValidation> stackParamValidations) {
        super(resourceId);
        this.stackParamValidations = stackParamValidations;
    }

    public GetStackParamValidationResult(String statusReason, Exception errorDetails, Long resourceId) {
        super(statusReason, errorDetails, resourceId);
    }

    public List<StackParamValidation> getStackParamValidations() {
        return stackParamValidations;
    }
}
