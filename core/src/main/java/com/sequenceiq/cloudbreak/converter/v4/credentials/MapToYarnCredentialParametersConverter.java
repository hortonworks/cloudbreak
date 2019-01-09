package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.yarn.YarnCredentialV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToYarnCredentialParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, YarnCredentialV4Parameters> {

    @Override
    public YarnCredentialV4Parameters convert(Map<String, Object> source) {
        YarnCredentialV4Parameters parameters = new YarnCredentialV4Parameters();
        parameters.setAmbariUser((String) source.get("cumulusAmbariUser"));
        parameters.setEndpoint((String) source.get("yarnEndpoint"));
        return parameters;
    }

}
