package com.sequenceiq.environment.credential.v1.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.cumulus.CumulusYarnParameters;
import com.sequenceiq.environment.credential.attributes.cumulus.CumulusYarnCredentialAttributes;

@Component
class CumulusCredentialV1ParametersToCumulusCredentialAttributesConverter {

    public CumulusYarnCredentialAttributes convert(CumulusYarnParameters source) {
        CumulusYarnCredentialAttributes response = new CumulusYarnCredentialAttributes();
        response.setAmbariPassword(source.getAmbariPassword());
        response.setAmbariUrl(source.getAmbariUrl());
        response.setAmbariUser(source.getAmbariUser());
        return response;
    }

    public CumulusYarnParameters convert(CumulusYarnCredentialAttributes source) {
        CumulusYarnParameters response = new CumulusYarnParameters();
        response.setAmbariPassword(source.getAmbariPassword());
        response.setAmbariUrl(source.getAmbariUrl());
        response.setAmbariUser(source.getAmbariUser());
        return response;
    }
}
