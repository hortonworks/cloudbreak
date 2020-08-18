package com.sequenceiq.environment.credential.v1.converter.yarn;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.credential.attributes.yarn.YarnCredentialAttributes;

@Component
public class YarnCredentialV1ParametersToAwsYarnAttributesConverter {

    public YarnCredentialAttributes convert(YarnParameters source) {
        YarnCredentialAttributes response = new YarnCredentialAttributes();
        response.setEndpoint(source.getEndpoint());
        return response;
    }

    public YarnParameters convert(YarnCredentialAttributes source) {
        YarnParameters response = new YarnParameters();
        response.setEndpoint(source.getEndpoint());
        return response;
    }
}
