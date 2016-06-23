package com.sequenceiq.cloudbreak.cloud.mock

import javax.inject.Inject

import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.Authenticator
import com.sequenceiq.cloudbreak.cloud.CloudConnector
import com.sequenceiq.cloudbreak.cloud.CredentialConnector
import com.sequenceiq.cloudbreak.cloud.InstanceConnector
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.PlatformParameters
import com.sequenceiq.cloudbreak.cloud.ResourceConnector
import com.sequenceiq.cloudbreak.cloud.Setup
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.Variant

@Service
class MockConnector : CloudConnector {

    @Inject
    private val mockAuthenticator: MockAuthenticator? = null
    @Inject
    private val mockSetup: MockSetup? = null
    @Inject
    private val mockCredentialConnector: MockCredentialConnector? = null
    @Inject
    private val mockResourceConnector: MockResourceConnector? = null
    @Inject
    private val mockInstanceConnector: MockInstanceConnector? = null
    @Inject
    private val mockMetadataCollector: MockMetadataCollector? = null
    @Inject
    private val mockPlatformParameters: MockPlatformParameters? = null

    override fun authentication(): Authenticator {
        return mockAuthenticator
    }

    override fun setup(): Setup {
        return mockSetup
    }

    override fun credentials(): CredentialConnector {
        return mockCredentialConnector
    }

    override fun resources(): ResourceConnector {
        return mockResourceConnector
    }

    override fun instances(): InstanceConnector {
        return mockInstanceConnector
    }

    override fun metadata(): MetadataCollector {
        return mockMetadataCollector
    }

    override fun parameters(): PlatformParameters {
        return mockPlatformParameters
    }

    override fun platform(): Platform {
        return MockConstants.MOCK_PLATFORM
    }

    override fun variant(): Variant {
        return MockConstants.MOCK_VARIANT
    }
}
