package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;
import com.sequenceiq.environment.credential.attributes.yarn.YarnCredentialAttributes;

@Component
public class YarnCredentialV1ParametersToAwsYarnAttributesConverter {

    public YarnCredentialAttributes convert(YarnParameters source) {
        if (source == null) {
            return null;
        }
        YarnCredentialAttributes response = new YarnCredentialAttributes();
        response.setAmbariUser(source.getAmbariUser());
        response.setEndpoint(source.getEndpoint());
        return response;
    }

    public YarnParameters convert(YarnCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        YarnParameters response = new YarnParameters();
        response.setAmbariUser(source.getAmbariUser());
        response.setEndpoint(source.getEndpoint());
        return response;
    }

}
