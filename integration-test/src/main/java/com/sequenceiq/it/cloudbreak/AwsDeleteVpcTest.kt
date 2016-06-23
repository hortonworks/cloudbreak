package com.sequenceiq.it.cloudbreak

import org.testng.annotations.AfterSuite
import org.testng.annotations.Optional
import org.testng.annotations.Parameters

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.DeleteStackRequest

class AwsDeleteVpcTest : AbstractCloudbreakIntegrationTest() {

    @AfterSuite
    @Parameters("regionName", "vpcStackName")
    fun deleteNetwork(regionName: String, @Optional("it-vpc-stack") vpcStackName: String) {
        val client = AmazonCloudFormationClient()
        client.setRegion(RegionUtils.getRegion(regionName))
        client.deleteStack(DeleteStackRequest().withStackName(vpcStackName))
    }
}