package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.credential.model.parameters.mock.MockCredentialV1Parameters;
import com.sequenceiq.environment.credential.attributes.mock.MockCredentialAttributes;

@Component
public class MockCredentialV1ParametersToMockCredentialAttributesConverter {

    public MockCredentialAttributes convert(MockCredentialV1Parameters source) {
        MockCredentialAttributes response = new MockCredentialAttributes();
        response.setMockEndpoint(source.getMockEndpoint());
        return response;
    }

    public MockCredentialV1Parameters convert(MockCredentialAttributes source) {
        MockCredentialV1Parameters response = new MockCredentialV1Parameters();
        response.setMockEndpoint(source.getMockEndpoint());
        return response;
    }
}
