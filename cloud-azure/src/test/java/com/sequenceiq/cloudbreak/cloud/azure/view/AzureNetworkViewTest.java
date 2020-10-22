package com.sequenceiq.cloudbreak.cloud.azure.view;

import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView.ENDPOINT_TYPE;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView.SUBNETS;
import static com.sequenceiq.cloudbreak.cloud.azure.view.AzureNetworkView.SUBNET_FOR_PRIVATE_ENDPOINT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.common.model.PrivateEndpointType;

public class AzureNetworkViewTest {

    @Mock
    private Network network;

    private AzureNetworkView underTest;

    @Before
    public void setUp() {
        initMocks(this);

        underTest = new AzureNetworkView(network);
    }

    @Test
    public void testSubnets() {
        when(network.getStringParameter(SUBNETS)).thenReturn("subnet-a,subnet-b");
        assertThat(underTest.getSubnets()).isEqualTo("subnet-a,subnet-b");
    }

    @Test
    public void testGetSubnetIdForPrivateEndpoint() {
        when(network.getStringParameter(SUBNET_FOR_PRIVATE_ENDPOINT)).thenReturn("subnet-a");
        assertThat(underTest.getSubnetIdForPrivateEndpoint()).isEqualTo("subnet-a");
    }

    @Test
    public void testGetEndpointType() {
        when(network.getStringParameter(ENDPOINT_TYPE)).thenReturn("USE_PRIVATE_ENDPOINT");
        assertThat(underTest.getEndpointType()).isEqualTo(PrivateEndpointType.USE_PRIVATE_ENDPOINT);
    }
}
