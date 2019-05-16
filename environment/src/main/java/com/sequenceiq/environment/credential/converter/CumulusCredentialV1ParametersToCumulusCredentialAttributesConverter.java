package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.credential.model.parameters.cumulus.CumulusYarnCredentialV1Parameters;
import com.sequenceiq.environment.credential.attributes.cumulus.CumulusYarnCredentialAttributes;

@Component
public class CumulusCredentialV1ParametersToCumulusCredentialAttributesConverter {

    public CumulusYarnCredentialAttributes convert(CumulusYarnCredentialV1Parameters source) {
        CumulusYarnCredentialAttributes response = new CumulusYarnCredentialAttributes();
        response.setAmbariPassword(source.getAmbariPassword());
        response.setAmbariUrl(source.getAmbariUrl());
        response.setAmbariUser(source.getAmbariUser());
        return response;
    }

    public CumulusYarnCredentialV1Parameters convert(CumulusYarnCredentialAttributes source) {
        CumulusYarnCredentialV1Parameters response = new CumulusYarnCredentialV1Parameters();
        response.setAmbariPassword(source.getAmbariPassword());
        response.setAmbariUrl(source.getAmbariUrl());
        response.setAmbariUser(source.getAmbariUser());
        return response;
    }
}
