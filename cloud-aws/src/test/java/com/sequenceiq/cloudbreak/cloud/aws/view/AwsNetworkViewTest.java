package com.sequenceiq.cloudbreak.cloud.aws.view;

import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView.IGW;
import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView.SUBNET_ID;
import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView.VPC_CIDR;
import static com.sequenceiq.cloudbreak.cloud.aws.view.AwsNetworkView.VPC_ID;
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
        when(network.getStringParameter(IGW)).thenReturn("igw-123");
        assertTrue(underTest.isExistingIGW());
        assertEquals("igw-123", underTest.getExistingIgw());
    }

    @Test
    public void testNoIgw() {
        when(network.getStringParameter(IGW)).thenReturn(null);
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

}
