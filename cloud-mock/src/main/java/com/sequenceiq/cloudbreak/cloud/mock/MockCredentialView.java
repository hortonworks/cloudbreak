package com.sequenceiq.cloudbreak.cloud.mock;

public class MockCredentialView {
    private final String mockServerAddress;

    public MockCredentialView(String mockServerAddress) {
        this.mockServerAddress = mockServerAddress;
    }

    public String getMockEndpoint() {
        return mockServerAddress;
    }
}
