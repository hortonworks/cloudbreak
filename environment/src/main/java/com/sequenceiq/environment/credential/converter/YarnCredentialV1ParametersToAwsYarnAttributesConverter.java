package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.credential.model.parameters.yarn.YarnCredentialV1Parameters;
import com.sequenceiq.environment.credential.attributes.yarn.YarnCredentialAttributes;

@Component
public class YarnCredentialV1ParametersToAwsYarnAttributesConverter {

    public YarnCredentialAttributes convert(YarnCredentialV1Parameters source) {
        YarnCredentialAttributes response = new YarnCredentialAttributes();
        response.setAmbariUser(source.getAmbariUser());
        response.setEndpoint(source.getEndpoint());
        return response;
    }

    public YarnCredentialV1Parameters convert(YarnCredentialAttributes source) {
        YarnCredentialV1Parameters response = new YarnCredentialV1Parameters();
        response.setAmbariUser(source.getAmbariUser());
        response.setEndpoint(source.getEndpoint());
        return response;
    }

}
