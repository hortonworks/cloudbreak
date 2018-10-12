package com.sequenceiq.cloudbreak.cloud.event.validation;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public class ParametersValidationRequest extends CloudPlatformRequest<ParametersValidationResult> {

    private final Map<String, String> parameters;

    public ParametersValidationRequest(CloudCredential credential, CloudContext cloudContext, Map<String, String> parameters) {
        super(cloudContext, credential);
        this.parameters = parameters;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ParametersValidationRequest{");
        sb.append("parameters=").append(parameters);
        sb.append('}');
        return sb.toString();
    }
}
