package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.GCP_PLATFORM;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.GCP_VARIANT;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.ValidatorType;
import com.sequenceiq.cloudbreak.cloud.gcp.setup.GcpProvisionSetup;

@ExtendWith(MockitoExtension.class)
public class GcpConnectorTest {

    @InjectMocks
    private GcpConnector underTest;

    @Mock
    private GcpAuthenticator authenticator;

    @Mock
    private GcpProvisionSetup provisionSetup;

    @Mock
    private GcpTagValidator gcpTagValidator;

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
    public void testAuthentication() {
        assertEquals(authenticator, underTest.authentication());
    }

    @Test
    public void testValidators() {
        assertEquals(Collections.singletonList(gcpTagValidator), underTest.validators(ValidatorType.ALL));
    }

    @Test
    public void testProvisionSetup() {
        assertEquals(provisionSetup, underTest.setup());
    }

    @Test
    public void testPlatformParameters() {
        assertEquals(gcpPlatformParameters, underTest.parameters());
    }

    @Test
    public void testCloudConstant() {
        assertEquals(gcpConstants, underTest.cloudConstant());
    }

    @Test
    public void testCredentials() {
        assertEquals(gcpCredentialConnector, underTest.credentials());
    }

    @Test
    public void testVariant() {
        assertEquals(GCP_VARIANT, underTest.variant());
    }

    @Test
    public void testPlatform() {
        assertEquals(GCP_PLATFORM, underTest.platform());
    }

    @Test
    public void testEncryptionResources() {
        assertEquals(null, underTest.encryptionResources());
    }

    @Test
    public void testIdentityService() {
        assertEquals(gcpIdentityService, underTest.identityService());
    }

    @Test
    public void testInstances() {
        assertEquals(instanceConnector, underTest.instances());
    }

    @Test
    public void testMetadata() {
        assertEquals(metadataCollector, underTest.metadata());
    }

    @Test
    public void testNetworkConnector() {
        assertEquals(gcpNetworkConnector, underTest.networkConnector());
    }

    @Test
    public void testResources() {
        assertEquals(resourceConnector, underTest.resources());
    }

    @Test
    public void testObjectStorage() {
        assertEquals(gcpObjectStorageConnector, underTest.objectStorage());
    }

    @Test
    public void testPlatformResources() {
        assertEquals(gcpPlatformResources, underTest.platformResources());
    }

    @Test
    public void testAvailabilityZoneConnector() {
        assertEquals(gcpAvailabilityZoneConnector, underTest.availabilityZoneConnector());
    }

}
