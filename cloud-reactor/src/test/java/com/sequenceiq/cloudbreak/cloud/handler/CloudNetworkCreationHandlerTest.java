package com.sequenceiq.cloudbreak.cloud.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.event.platform.CloudNetworkCreationRequest;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedCloudNetwork;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.NetworkCreationRequest;

import reactor.bus.Event;

@RunWith(MockitoJUnitRunner.class)
public class CloudNetworkCreationHandlerTest {

    private static final String VPC_ID = "vpc-id";

    private static final String CLOUD_PLATFORM = "AWS";

    private static final String ENV_NAME = "TEST_ENV";

    private static final String VARIANT = "v4";

    private static final String REGION = "US-WEST";

    private static final String NETWORK_CIDR = "0.0.0.0/16";

    private static final Set<String> SUBNET_CIDRS = Set.of("1.1.1.1/8", "2.2.2.2/8");

    @InjectMocks
    private CloudNetworkCreationHandler underTest;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Test
    public void testAcceptShouldCallTheCloudPlatformConnectorsWithTheProperParameters() {
        Event<CloudNetworkCreationRequest> createCloudNetworkRequest = createCloudNetworkRequestEvent();
        CloudNetworkCreationRequest request = createCloudNetworkRequest.getData();

        CloudConnector awsConnector = Mockito.mock(CloudConnector.class);
        NetworkConnector networkConnector = Mockito.mock(NetworkConnector.class);
        CreatedCloudNetwork createdCloudNetwork = createCreatedCloudNetwork();

        when(networkConnector.createNetworkWithSubnets(any(NetworkCreationRequest.class))).thenReturn(createdCloudNetwork);
        when(awsConnector.networkConnector()).thenReturn(networkConnector);
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(awsConnector);

        underTest.accept(createCloudNetworkRequest);

        verify(request).getResult();
        verify(networkConnector).createNetworkWithSubnets(any(NetworkCreationRequest.class));
        verify(awsConnector).networkConnector();
        verify(cloudPlatformConnectors).get(any(CloudPlatformVariant.class));
    }

    @Test
    public void testAcceptShouldThrowExceptionWhenCloudPlatformConnectorsThrowsAnException() {
        Event<CloudNetworkCreationRequest> createCloudNetworkRequest = createCloudNetworkRequestEvent();
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenThrow(new IllegalArgumentException());

        underTest.accept(createCloudNetworkRequest);

        verify(cloudPlatformConnectors).get(any(CloudPlatformVariant.class));
    }

    private CreatedCloudNetwork createCreatedCloudNetwork() {
        return new CreatedCloudNetwork(VPC_ID, Collections.emptySet(), Collections.emptyMap());
    }

    private Event<CloudNetworkCreationRequest> createCloudNetworkRequestEvent() {
        ExtendedCloudCredential extendedCloudCredential = createExtendedCloudCredential();
        CloudNetworkCreationRequest request = new CloudNetworkCreationRequest(ENV_NAME, extendedCloudCredential, extendedCloudCredential, VARIANT,
                REGION, NETWORK_CIDR, SUBNET_CIDRS, false, false);
        return new Event<>(Mockito.spy(request));
    }

    private ExtendedCloudCredential createExtendedCloudCredential() {
        ExtendedCloudCredential extendedCloudCredential = mock(ExtendedCloudCredential.class);
        when(extendedCloudCredential.getCloudPlatform()).thenReturn(CLOUD_PLATFORM);
        return extendedCloudCredential;
    }
}