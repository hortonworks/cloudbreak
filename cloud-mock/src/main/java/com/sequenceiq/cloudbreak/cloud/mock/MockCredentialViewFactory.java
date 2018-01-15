package com.sequenceiq.cloudbreak.cloud.mock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

@Component
public class MockCredentialViewFactory {
    @Value("${mock.spi.endpoint:https://localhost:9443}")
    private String mockServerAddress;

    public MockCredentialView createCredetialView(CloudCredential cloudCredential) {
        String endpoint = cloudCredential.getParameter(MockConstants.MOCK_ENDPOINT_PARAMETER, String.class);
        return new MockCredentialView(StringUtils.isEmpty(endpoint) ? mockServerAddress : endpoint);
    }
}
