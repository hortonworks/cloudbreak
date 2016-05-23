package com.sequenceiq.it.cloudbreak;

import org.testng.annotations.AfterSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.DeleteStackRequest;

public class AwsDeleteVpcTest extends AbstractCloudbreakIntegrationTest {

    @AfterSuite
    @Parameters({ "regionName", "vpcStackName" })
    public void deleteNetwork(String regionName, @Optional("it-vpc-stack") String vpcStackName) {
        AmazonCloudFormationClient client = new AmazonCloudFormationClient();
        client.setRegion(RegionUtils.getRegion(regionName));
        client.deleteStack(new DeleteStackRequest().withStackName(vpcStackName));
    }
}