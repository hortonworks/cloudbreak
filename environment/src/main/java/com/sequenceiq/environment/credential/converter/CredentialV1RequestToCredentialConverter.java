package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.credential.model.request.CredentialV1Request;
import com.sequenceiq.environment.credential.Credential;

@Component
public class CredentialV1RequestToCredentialConverter extends AbstractConversionServiceAwareConverter<CredentialV1Request, Credential> {

    @Override
    public Credential convert(CredentialV1Request source) {
        Credential credential = new Credential();
        credential.setName(source.getName());
        credential.setDescription(source.getDescription());
        credential.setCloudPlatform(source.getCloudPlatform());
//      credential.setAttributes(new Json(credentialPropertyCollector.propertyMap(source)).getValue());
        if (source.getAws() != null) {
            credential.setGovCloud(source.getAws().getGovCloud());
        }
        return credential;
    }

}
