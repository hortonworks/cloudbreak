package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.Map;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws.KeyBasedCredentialParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

public class MapToKeyBasedCredentialConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, KeyBasedCredentialParameters> {

    @Override
    public KeyBasedCredentialParameters convert(Map<String, Object> source) {
        KeyBasedCredentialParameters keyBasedCredentialParameters = new KeyBasedCredentialParameters();
        keyBasedCredentialParameters.setAccessKey((String) source.get("accessKey"));
        keyBasedCredentialParameters.setSecretKey((String) source.get("secretKey"));
        return keyBasedCredentialParameters;
    }
}
