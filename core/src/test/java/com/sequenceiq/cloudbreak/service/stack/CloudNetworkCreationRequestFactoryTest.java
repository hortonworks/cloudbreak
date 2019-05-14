package com.sequenceiq.cloudbreak.service.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.base.EnvironmentNetworkAzureV4Params;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentNetworkV4Request;
import com.sequenceiq.cloudbreak.cloud.event.platform.CloudNetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Credential;

@RunWith(MockitoJUnitRunner.class)
public class CloudNetworkCreationRequestFactoryTest {

    private static final String ENV_NAME = "TEST_ENV";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String REGION = "US-WEST";

    private static final String NETWORK_CIDR = "0.0.0.0/16";

    private static final Set<String> SUBNET_CIDRS = Set.of("1.1.1.1/8", "2.2.2.2/8", "3.3.3.3/8");

    @InjectMocks
    private CloudNetworkCreationRequestFactory underTest;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Mock
    private SubnetCidrProvider subnetCidrProvider;

    @Test
    public void testCreateShouldCreateANewCreateCloudNetworkRequestInstance() {
        Credential credential = new Credential();
        ExtendedCloudCredential cloudCredential = mock(ExtendedCloudCredential.class);
        EnvironmentNetworkV4Request networkRequest = createNetworkRequest(createAzureParams());

        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(subnetCidrProvider.provide(NETWORK_CIDR)).thenReturn(SUBNET_CIDRS);

        CloudNetworkCreationRequest actual = underTest.create(ENV_NAME, credential, CLOUD_PLATFORM, REGION, networkRequest);

        assertEquals(ENV_NAME, actual.getEnvName());
        assertEquals(cloudCredential, actual.getCloudCredential());
        assertEquals(cloudCredential, actual.getExtendedCloudCredential());
        assertEquals(CLOUD_PLATFORM, actual.getVariant());
        assertEquals(REGION, actual.getRegion());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(SUBNET_CIDRS, actual.getSubnetCidrs());
        assertTrue(actual.isNoFirewallRules());
        assertTrue(actual.isNoPublicIp());
    }

    @Test
    public void testCreateShouldCreateANewCreateCloudNetworkRequestWhenTheAzureParamsAreNotPresent() {
        Credential credential = new Credential();
        ExtendedCloudCredential cloudCredential = mock(ExtendedCloudCredential.class);
        EnvironmentNetworkV4Request networkRequest = createNetworkRequest(null);

        when(credentialToExtendedCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        when(subnetCidrProvider.provide(NETWORK_CIDR)).thenReturn(SUBNET_CIDRS);

        CloudNetworkCreationRequest actual = underTest.create(ENV_NAME, credential, CLOUD_PLATFORM, REGION, networkRequest);

        assertEquals(ENV_NAME, actual.getEnvName());
        assertEquals(cloudCredential, actual.getCloudCredential());
        assertEquals(cloudCredential, actual.getExtendedCloudCredential());
        assertEquals(CLOUD_PLATFORM, actual.getVariant());
        assertEquals(REGION, actual.getRegion());
        assertEquals(NETWORK_CIDR, actual.getNetworkCidr());
        assertEquals(SUBNET_CIDRS, actual.getSubnetCidrs());
        assertNull(actual.isNoFirewallRules());
        assertNull(actual.isNoPublicIp());
    }

    private EnvironmentNetworkV4Request createNetworkRequest(EnvironmentNetworkAzureV4Params azureV4Params) {
        EnvironmentNetworkV4Request networkRequest = new EnvironmentNetworkV4Request();
        networkRequest.setNetworkCidr(NETWORK_CIDR);
        networkRequest.setAzure(azureV4Params);
        return networkRequest;
    }

    private EnvironmentNetworkAzureV4Params createAzureParams() {
        EnvironmentNetworkAzureV4Params azureV4Params = new EnvironmentNetworkAzureV4Params();
        azureV4Params.setNoFirewallRules(true);
        azureV4Params.setNoPublicIp(true);
        return azureV4Params;
    }
}