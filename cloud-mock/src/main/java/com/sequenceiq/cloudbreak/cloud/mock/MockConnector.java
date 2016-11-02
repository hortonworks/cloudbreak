package com.sequenceiq.cloudbreak.cloud.mock;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.CredentialConnector;
import com.sequenceiq.cloudbreak.cloud.InstanceConnector;
import com.sequenceiq.cloudbreak.cloud.MetadataCollector;
import com.sequenceiq.cloudbreak.cloud.PlatformParameters;
import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.Setup;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

@Service
public class MockConnector implements CloudConnector {

    @Inject
    private MockAuthenticator mockAuthenticator;

    @Inject
    private MockSetup mockSetup;

    @Inject
    private MockCredentialConnector mockCredentialConnector;

    @Inject
    private MockResourceConnector mockResourceConnector;

    @Inject
    private MockInstanceConnector mockInstanceConnector;

    @Inject
    private MockMetadataCollector mockMetadataCollector;

    @Inject
    private MockPlatformParameters mockPlatformParameters;

    @Override
    public Authenticator authentication() {
        return mockAuthenticator;
    }

    @Override
    public Setup setup() {
        return mockSetup;
    }

    @Override
    public CredentialConnector credentials() {
        return mockCredentialConnector;
    }

    @Override
    public ResourceConnector resources() {
        return mockResourceConnector;
    }

    @Override
    public InstanceConnector instances() {
        return mockInstanceConnector;
    }

    @Override
    public MetadataCollector metadata() {
        return mockMetadataCollector;
    }

    @Override
    public PlatformParameters parameters() {
        return mockPlatformParameters;
    }

    @Override
    public Platform platform() {
        return MockConstants.MOCK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return MockConstants.MOCK_VARIANT;
    }
}
