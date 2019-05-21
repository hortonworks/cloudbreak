package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.cumulus.CumulusYarnParameters;
import com.sequenceiq.environment.credential.attributes.cumulus.CumulusYarnCredentialAttributes;

@Component
public class CumulusCredentialV1ParametersToCumulusCredentialAttributesConverter {

    public CumulusYarnCredentialAttributes convert(CumulusYarnParameters source) {
        if (source == null) {
            return null;
        }
        CumulusYarnCredentialAttributes response = new CumulusYarnCredentialAttributes();
        response.setAmbariPassword(source.getAmbariPassword());
        response.setAmbariUrl(source.getAmbariUrl());
        response.setAmbariUser(source.getAmbariUser());
        return response;
    }

    public CumulusYarnParameters convert(CumulusYarnCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        CumulusYarnParameters response = new CumulusYarnParameters();
        response.setAmbariPassword(source.getAmbariPassword());
        response.setAmbariUrl(source.getAmbariUrl());
        response.setAmbariUser(source.getAmbariUser());
        return response;
    }
}
