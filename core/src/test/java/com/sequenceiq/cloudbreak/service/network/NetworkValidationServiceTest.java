package com.sequenceiq.cloudbreak.service.network;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@RunWith(MockitoJUnitRunner.class)
public class NetworkValidationServiceTest {

    @InjectMocks
    private DefaultNetworkRequiredService underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testNetworkIfNullRequestThenShouldReturnTrue() {
        Assert.assertTrue(underTest.shouldAddNetwork(CloudPlatform.AWS.name(), null));
    }

    @Test
    public void testNetworkIfAwsRequestEmptyThenShouldReturnTrue() {
        NetworkV4Request networkV4Request = new NetworkV4Request();
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        networkV4Request.setAws(awsNetworkV4Parameters);
        Assert.assertTrue(underTest.shouldAddNetwork(CloudPlatform.AWS.name(), networkV4Request));
    }

    @Test
    public void testNetworkIfAwsRequestHasSubnetIdThenShouldReturnFalse() {
        NetworkV4Request networkV4Request = new NetworkV4Request();
        AwsNetworkV4Parameters awsNetworkV4Parameters = new AwsNetworkV4Parameters();
        //awsNetworkV4Parameters.setSubnetId("subnet");
        networkV4Request.setAws(awsNetworkV4Parameters);
        Assert.assertFalse(underTest.shouldAddNetwork(CloudPlatform.AWS.name(), networkV4Request));
    }

    @Test
    public void testNetworkIfAzureRequestEmptyThenShouldReturnTrue() {
        NetworkV4Request networkV4Request = new NetworkV4Request();
        AzureNetworkV4Parameters azureNetworkV4Parameters = new AzureNetworkV4Parameters();
        networkV4Request.setAzure(azureNetworkV4Parameters);
        Assert.assertTrue(underTest.shouldAddNetwork(CloudPlatform.AZURE.name(), networkV4Request));
    }

    @Test
    public void testNetworkIfAzureRequestHasSubnetThenShouldReturnTrue() {
        NetworkV4Request networkV4Request = new NetworkV4Request();
        AzureNetworkV4Parameters azureNetworkV4Parameters = new AzureNetworkV4Parameters();
        //azureNetworkV4Parameters.setSubnetId("subnet");
        networkV4Request.setAzure(azureNetworkV4Parameters);
        Assert.assertFalse(underTest.shouldAddNetwork(CloudPlatform.AZURE.name(), networkV4Request));
    }
}