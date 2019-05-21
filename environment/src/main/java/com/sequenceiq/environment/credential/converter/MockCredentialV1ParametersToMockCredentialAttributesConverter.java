package com.sequenceiq.environment.credential.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.credential.attributes.mock.MockCredentialAttributes;

@Component
public class MockCredentialV1ParametersToMockCredentialAttributesConverter {

    public MockCredentialAttributes convert(MockParameters source) {
        if (source == null) {
            return null;
        }
        MockCredentialAttributes response = new MockCredentialAttributes();
        response.setMockEndpoint(source.getMockEndpoint());
        return response;
    }

    public MockParameters convert(MockCredentialAttributes source) {
        if (source == null) {
            return null;
        }
        MockParameters response = new MockParameters();
        response.setMockEndpoint(source.getMockEndpoint());
        return response;
    }
}
