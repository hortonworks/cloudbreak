package com.sequenceiq.cloudbreak.cloud.azure.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.common.model.PrivateEndpointType;

public class AzureNetworkViewTest {

    private static final String SUBNETS = "subnets";

    private static final String SUBNET_FOR_PRIVATE_ENDPOINT = "subnetForPrivateEndpoint";

    private static final String ENDPOINT_TYPE = "endpointType";

    private static final String EXISTING_PRIVATE_DNS_ZONE_ID = "existingDatabasePrivateDnsZoneId";

    @Mock
    private Network network;

    private AzureNetworkView underTest;

    @BeforeEach
    public void setUp() {
        openMocks(this);
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

    @Test
    public void testGetPrivateDnsZoneId() {
        when(network.getStringParameter(EXISTING_PRIVATE_DNS_ZONE_ID)).thenReturn("privateDnsZoneId-a");
        assertThat(underTest.getExistingDatabasePrivateDnsZoneId()).isEqualTo("privateDnsZoneId-a");
    }
}
