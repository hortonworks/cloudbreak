package com.sequenceiq.cloudbreak.cloud.aws.view;

import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView.VPC_CIDR;
import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView.VPC_CIDRS;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.ENDPOINT_GATEWAY_SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.INTERNET_GATEWAY_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.SUBNET_ID;
import static com.sequenceiq.cloudbreak.common.network.NetworkConstants.VPC_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.cloud.model.Network;

public class AwsNetworkViewTest {

    private static final String SUBNET_1 = "subnet-123";

    private static final String SUBNET_2 = "subnet-456";

    private static final String SUBNET_3 = "subnet-789";

    private static final List<String> SUBNET_LIST = List.of(SUBNET_1, SUBNET_2, SUBNET_3);

    private static final String MULTI_SUBNET_STRING = String.join(",", SUBNET_LIST);

    @Mock
    private Network network;

    private AwsNetworkView underTest;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        underTest = new AwsNetworkView(network);
    }

    @Test
    public void testVpc() {
        when(network.getStringParameter(VPC_ID)).thenReturn("vpc-123");
        assertTrue(underTest.isExistingVPC());
        assertEquals("vpc-123", underTest.getExistingVpc());
    }

    @Test
    public void testNoVpc() {
        when(network.getStringParameter(VPC_ID)).thenReturn(null);
        assertFalse(underTest.isExistingVPC());
        assertNull(underTest.getExistingVpc());
    }

    @Test
    public void testVpcCidr() {
        when(network.getStringParameter(VPC_CIDR)).thenReturn("0.1.2.3/24");
        assertEquals("0.1.2.3/24", underTest.getExistingVpcCidr());
    }

    @Test
    public void testNoVpcCidr() {
        when(network.getStringParameter(VPC_ID)).thenReturn(null);
        assertNull(underTest.getExistingVpcCidr());
    }

    @Test
    public void testIgw() {
        when(network.getStringParameter(INTERNET_GATEWAY_ID)).thenReturn("igw-123");
        assertTrue(underTest.isExistingIGW());
        assertEquals("igw-123", underTest.getExistingIgw());
    }

    @Test
    public void testNoIgw() {
        when(network.getStringParameter(INTERNET_GATEWAY_ID)).thenReturn(null);
        assertFalse(underTest.isExistingIGW());
        assertNull(underTest.getExistingIgw());
    }

    @Test
    public void testSingleSubnet() {
        when(network.getStringParameter(SUBNET_ID)).thenReturn("subnet-123");
        assertTrue(underTest.isExistingSubnet());
        assertEquals("subnet-123", underTest.getExistingSubnet());
        assertFalse(underTest.isSubnetList());
        assertEquals(List.of("subnet-123"), underTest.getSubnetList());
    }

    @Test
    public void testMultipleSubnet() {
        when(network.getStringParameter(SUBNET_ID)).thenReturn("subnet-123,subnet-456,subnet-789");
        assertTrue(underTest.isExistingSubnet());
        assertEquals("subnet-123,subnet-456,subnet-789", underTest.getExistingSubnet());
        assertTrue(underTest.isSubnetList());
        assertEquals(List.of("subnet-123", "subnet-456", "subnet-789"), underTest.getSubnetList());
    }

    @Test
    public void testNoSubnet() {
        when(network.getStringParameter(SUBNET_ID)).thenReturn(null);
        assertFalse(underTest.isExistingSubnet());
        assertNull(underTest.getExistingSubnet());
        assertFalse(underTest.isSubnetList());
        assertEquals(List.of(), underTest.getSubnetList());
    }

    @Test
    public void testMultipleSubnetCidr() {
        when(network.getParameter(VPC_CIDRS, List.class)).thenReturn(List.of("1.1.1.1", "2.2.2.2"));
        assertTrue(underTest.getExistingVpcCidrs().containsAll(List.of("1.1.1.1", "2.2.2.2")));
    }

    @Test
    public void testMultipleSubnetCidrNull() {
        when(network.getParameter(VPC_CIDRS, List.class)).thenReturn(null);
        when(network.getStringParameter(VPC_CIDR)).thenReturn("1.1.1.1");
        assertTrue(underTest.getExistingVpcCidrs().contains("1.1.1.1"));
    }

    @Test
    public void testMultipleSubnetCidrEmpty() {
        when(network.getParameter(VPC_CIDRS, List.class)).thenReturn(List.of());
        when(network.getStringParameter(VPC_CIDR)).thenReturn("1.1.1.1");
        assertTrue(underTest.getExistingVpcCidrs().contains("1.1.1.1"));
    }

    @Test
    public void testSingleEndpointGatewaySubnet() {
        when(network.getStringParameter(ENDPOINT_GATEWAY_SUBNET_ID)).thenReturn(SUBNET_1);
        assertTrue(underTest.containsEndpointGatewaySubnet());
        assertFalse(underTest.isEndpointGatewaySubnetList());
        assertEquals(List.of(SUBNET_1), underTest.getEndpointGatewaySubnetList());
    }

    @Test
    public void testMultipleEndpointGatewaySubnets() {
        when(network.getStringParameter(ENDPOINT_GATEWAY_SUBNET_ID)).thenReturn(MULTI_SUBNET_STRING);
        assertTrue(underTest.containsEndpointGatewaySubnet());
        assertTrue(underTest.isEndpointGatewaySubnetList());
        assertEquals(SUBNET_LIST, underTest.getEndpointGatewaySubnetList());
    }

}
