package com.sequenceiq.cloudbreak.converter.v4.credentials;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.mock.MockCredentialV4Parameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class MapToMockCredentialV4ParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, MockCredentialV4Parameters> {

    @Override
    public MockCredentialV4Parameters convert(Map<String, Object> source) {
        MockCredentialV4Parameters parameters = new MockCredentialV4Parameters();
        parameters.setMockEndpoint((String) source.get("mockEndpoint"));
        return parameters;
    }

}
