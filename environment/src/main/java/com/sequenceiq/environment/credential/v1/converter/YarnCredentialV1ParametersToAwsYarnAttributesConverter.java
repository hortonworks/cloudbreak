package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.credential.attributes.yarn.YarnCredentialAttributes;

@Component
class YarnCredentialV1ParametersToAwsYarnAttributesConverter {

    public YarnCredentialAttributes convert(YarnParameters source) {
        YarnCredentialAttributes response = new YarnCredentialAttributes();
        response.setAmbariUser(source.getAmbariUser());
        response.setEndpoint(source.getEndpoint());
        return response;
    }

    public YarnParameters convert(YarnCredentialAttributes source) {
        YarnParameters response = new YarnParameters();
        response.setAmbariUser(source.getAmbariUser());
        response.setEndpoint(source.getEndpoint());
        return response;
    }
}
