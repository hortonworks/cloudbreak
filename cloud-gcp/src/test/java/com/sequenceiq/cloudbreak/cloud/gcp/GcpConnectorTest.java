package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.GCP_PLATFORM;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.GCP_VARIANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CommonSecretEncryptionValidator;
import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.gcp.setup.GcpProvisionSetup;

@ExtendWith(MockitoExtension.class)
class GcpConnectorTest {

    @InjectMocks
    private GcpConnector underTest;

    @Mock
    private GcpAuthenticator authenticator;

    @Mock
    private GcpProvisionSetup provisionSetup;

    @Mock
    private GcpTagValidator gcpTagValidator;

    @Mock
    private CommonSecretEncryptionValidator commonSecretEncryptionValidator;

    @Mock
    private GcpInstanceConnector instanceConnector;

    @Mock
    private GcpResourceConnector resourceConnector;

    @Mock
    private GcpPlatformResources gcpPlatformResources;

    @Mock
    private GcpCredentialConnector gcpCredentialConnector;

    @Mock
    private GcpPlatformParameters gcpPlatformParameters;

    @Mock
    private GcpMetadataCollector metadataCollector;

    @Mock
    private GcpNetworkConnector gcpNetworkConnector;

    @Mock
    private GcpConstants gcpConstants;

    @Mock
    private GcpIdentityService gcpIdentityService;

    @Mock
    private GcpObjectStorageConnector gcpObjectStorageConnector;

    @Mock
    private GcpAvailabilityZoneConnector gcpAvailabilityZoneConnector;

    @Test
    void testAuthentication() {
        assertEquals(authenticator, underTest.authentication());
    }

    @Test
    void testValidatorsImage() {
        assertThat(underTest.validators(ValidatorType.IMAGE)).containsExactly();
    }

    @Test
    void testValidatorsAll() {
        assertThat(underTest.validators(ValidatorType.ALL)).containsExactly(gcpTagValidator, commonSecretEncryptionValidator);
    }

    @Test
    void testProvisionSetup() {
        assertEquals(provisionSetup, underTest.setup());
    }

    @Test
    void testPlatformParameters() {
        assertEquals(gcpPlatformParameters, underTest.parameters());
    }

    @Test
    void testCloudConstant() {
        assertEquals(gcpConstants, underTest.cloudConstant());
    }

    @Test
    void testCredentials() {
        assertEquals(gcpCredentialConnector, underTest.credentials());
    }

    @Test
    void testVariant() {
        assertEquals(GCP_VARIANT, underTest.variant());
    }

    @Test
    void testPlatform() {
        assertEquals(GCP_PLATFORM, underTest.platform());
    }

    @Test
    void testEncryptionResources() {
        assertEquals(null, underTest.encryptionResources());
    }

    @Test
    void testIdentityService() {
        assertEquals(gcpIdentityService, underTest.identityService());
    }

    @Test
    void testInstances() {
        assertEquals(instanceConnector, underTest.instances());
    }

    @Test
    void testMetadata() {
        assertEquals(metadataCollector, underTest.metadata());
    }

    @Test
    void testNetworkConnector() {
        assertEquals(gcpNetworkConnector, underTest.networkConnector());
    }

    @Test
    void testResources() {
        assertEquals(resourceConnector, underTest.resources());
    }

    @Test
    void testObjectStorage() {
        assertEquals(gcpObjectStorageConnector, underTest.objectStorage());
    }

    @Test
    void testPlatformResources() {
        assertEquals(gcpPlatformResources, underTest.platformResources());
    }

    @Test
    void testAvailabilityZoneConnector() {
        assertEquals(gcpAvailabilityZoneConnector, underTest.availabilityZoneConnector());
    }

}
