package com.sequenceiq.cloudbreak.service.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class NetworkValidationServiceTest {

    private DefaultNetworkRequiredService underTest = new DefaultNetworkRequiredService();

    @Test
    void testNetworkIfNullRequestThenShouldReturnTrue() {
        assertTrue(underTest.shouldAddNetwork(CloudPlatform.AWS.name(), null));
    }

    @Test
    void testNetworkIfAwsRequestEmptyThenShouldReturnTrue() {
        NetworkV4Request networkV4Request = new NetworkV4Request();
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        networkV4Request.setAws(awsNetworkV4Parameters);
        assertTrue(underTest.shouldAddNetwork(CloudPlatform.AWS.name(), networkV4Request));
    }

    @Test
    void testNetworkIfAwsRequestHasSubnetIdThenShouldReturnFalse() {
        NetworkV4Request networkV4Request = new NetworkV4Request();
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        awsNetworkV4Parameters.setSubnetId("subnet");
        networkV4Request.setAws(awsNetworkV4Parameters);
        assertFalse(underTest.shouldAddNetwork(CloudPlatform.AWS.name(), networkV4Request));
    }

    @Test
    void testNetworkIfAzureRequestEmptyThenShouldReturnTrue() {
        NetworkV4Request networkV4Request = new NetworkV4Request();
        AzureNetworkV4Parameters azureNetworkV4Parameters = new AzureNetworkV4Parameters();
        networkV4Request.setAzure(azureNetworkV4Parameters);
        assertTrue(underTest.shouldAddNetwork(CloudPlatform.AZURE.name(), networkV4Request));
    }

    @Test
    void testNetworkIfAzureRequestHasSubnetThenShouldReturnTrue() {
        NetworkV4Request networkV4Request = new NetworkV4Request();
        AzureNetworkV4Parameters azureNetworkV4Parameters = new AzureNetworkV4Parameters();
        azureNetworkV4Parameters.setSubnetId("subnet");
        networkV4Request.setAzure(azureNetworkV4Parameters);
        assertFalse(underTest.shouldAddNetwork(CloudPlatform.AZURE.name(), networkV4Request));
    }
}