package com.sequenceiq.cloudbreak.cloud.gcp;

import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.GCP_PLATFORM;
import static com.sequenceiq.cloudbreak.cloud.gcp.GcpConstants.GCP_VARIANT;

import java.util.Collections;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    public void testAuthentication() {
        Assert.assertEquals(authenticator, underTest.authentication());
    }

    @Test
    public void testValidators() {
        Assert.assertEquals(Collections.singletonList(gcpTagValidator), underTest.validators());
    }

    @Test
    public void testProvisionSetup() {
        Assert.assertEquals(provisionSetup, underTest.setup());
    }

    @Test
    public void testPlatformParameters() {
        Assert.assertEquals(gcpPlatformParameters, underTest.parameters());
    }

    @Test
    public void testCloudConstant() {
        Assert.assertEquals(gcpConstants, underTest.cloudConstant());
    }

    @Test
    public void testCredentials() {
        Assert.assertEquals(gcpCredentialConnector, underTest.credentials());
    }

    @Test
    public void testVariant() {
        Assert.assertEquals(GCP_VARIANT, underTest.variant());
    }

    @Test
    public void testPlatform() {
        Assert.assertEquals(GCP_PLATFORM, underTest.platform());
    }

    @Test
    public void testEncryptionResources() {
        Assert.assertEquals(null, underTest.encryptionResources());
    }

    @Test
    public void testIdentityService() {
        Assert.assertEquals(gcpIdentityService, underTest.identityService());
    }

    @Test
    public void testInstances() {
        Assert.assertEquals(instanceConnector, underTest.instances());
    }

    @Test
    public void testMetadata() {
        Assert.assertEquals(metadataCollector, underTest.metadata());
    }

    @Test
    public void testNetworkConnector() {
        Assert.assertEquals(gcpNetworkConnector, underTest.networkConnector());
    }

    @Test
    public void testResources() {
        Assert.assertEquals(resourceConnector, underTest.resources());
    }

    @Test
    public void testObjectStorage() {
        Assert.assertEquals(gcpObjectStorageConnector, underTest.objectStorage());
    }

    @Test
    public void testPlatformResources() {
        Assert.assertEquals(gcpPlatformResources, underTest.platformResources());
    }

}
