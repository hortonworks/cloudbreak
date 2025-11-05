package com.sequenceiq.datalake.service.sdx.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.network.NetworkV4Response;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class NetworkV4ResponseToNetworkV4RequestConverterTest {

    private NetworkV4ResponseToNetworkV4RequestConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new NetworkV4ResponseToNetworkV4RequestConverter();
    }

    @Test
    public void testAwsConvert() {
        NetworkV4Response networkV4Response = new NetworkV4Response();
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        awsNetworkV4Parameters.setSubnetId("subnet-123");
        awsNetworkV4Parameters.setInternetGatewayId("internet-gateway-123");
        awsNetworkV4Parameters.setVpcId("vpc-123");
        networkV4Response.setSubnetCIDR("192.168.1.0/24");
        networkV4Response.setCloudPlatform(CloudPlatform.AWS);
        networkV4Response.setAws(awsNetworkV4Parameters);

        NetworkV4Request networkV4Request = underTest.convert(networkV4Response);

        assertEquals(networkV4Response.getSubnetCIDR(), networkV4Request.getSubnetCIDR());
        assertEquals(networkV4Response.getCloudPlatform(), networkV4Request.getCloudPlatform());
        assertEquals(networkV4Response.getAws(), networkV4Request.getAws());
    }

    @Test
    public void testAzureConvert() {
        NetworkV4Response networkV4Response = new NetworkV4Response();
        AzureNetworkV4Parameters azureNetworkV4Parameters = new AzureNetworkV4Parameters();
        azureNetworkV4Parameters.setSubnetId("subnet-123");
        azureNetworkV4Parameters.setNetworkId("network-123");
        azureNetworkV4Parameters.setNoPublicIp(true);
        azureNetworkV4Parameters.setResourceGroupName("resource-group-123");
        azureNetworkV4Parameters.setNoOutboundLoadBalancer(true);
        networkV4Response.setAzure(azureNetworkV4Parameters);

        NetworkV4Request networkV4Request = underTest.convert(networkV4Response);

        assertEquals(networkV4Response.getAzure(), networkV4Request.getAzure());
    }

    @Test
    public void testGcpConvert() {
        NetworkV4Response networkV4Response = new NetworkV4Response();
        GcpNetworkV4Parameters gcpNetworkV4Parameters = new GcpNetworkV4Parameters();
        gcpNetworkV4Parameters.setSubnetId("subnet-123");
        gcpNetworkV4Parameters.setNetworkId("network-123");
        gcpNetworkV4Parameters.setNoPublicIp(true);
        gcpNetworkV4Parameters.setNoFirewallRules(true);
        gcpNetworkV4Parameters.setSharedProjectId("shared-project-123");
        networkV4Response.setGcp(gcpNetworkV4Parameters);

        NetworkV4Request networkV4Request = underTest.convert(networkV4Response);

        assertEquals(networkV4Response.getGcp(), networkV4Request.getGcp());
    }
}
