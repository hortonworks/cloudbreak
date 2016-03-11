package com.sequenceiq.cloudbreak.cloud.event.platform;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.cloud.model.StackParamValidation;

public class GetStackParamValidationResult extends CloudPlatformResult<CloudPlatformRequest> {
    private List<StackParamValidation> stackParamValidations;

    public GetStackParamValidationResult(CloudPlatformRequest<?> request, List<StackParamValidation> stackParamValidations) {
        super(request);
        this.stackParamValidations = stackParamValidations;
    }

    public GetStackParamValidationResult(String statusReason, Exception errorDetails, CloudPlatformRequest<?> request) {
        super(statusReason, errorDetails, request);
    }

    public List<StackParamValidation> getStackParamValidations() {
        return stackParamValidations;
    }
}
