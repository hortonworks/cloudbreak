package com.sequenceiq.it.cloudbreak

import com.amazonaws.services.cloudformation.model.StackStatus.CREATE_COMPLETE
import com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED
import com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE
import com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED
import com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS

import java.io.IOException
import java.io.InputStream
import java.util.Arrays
import java.util.HashMap

import org.apache.commons.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient
import com.amazonaws.services.cloudformation.model.CreateStackRequest
import com.amazonaws.services.cloudformation.model.DescribeStacksRequest
import com.amazonaws.services.cloudformation.model.Output
import com.amazonaws.services.cloudformation.model.Parameter
import com.amazonaws.services.cloudformation.model.Stack
import com.amazonaws.services.cloudformation.model.StackStatus
import com.sequenceiq.cloudbreak.api.model.NetworkJson

class AwsCreateExistingVpcNetworkTest : AbstractCloudbreakIntegrationTest() {

    @Test
    @Parameters("networkName", "description", "publicInAccount", "regionName", "vpcStackName", "vpcName")
    fun createNetwork(networkName: String, @Optional("") description: String, @Optional("false") publicInAccount: Boolean,
                      regionName: String, @Optional("it-vpc-stack") vpcStackName: String, @Optional("it-vpc") vpcName: String) {
        val client = AmazonCloudFormationClient()
        client.setRegion(RegionUtils.getRegion(regionName))

        val vpcId: String
        val subnetId: String

        try {
            javaClass.getResourceAsStream("/cloudformation/public_vpc.json").use({ vpcJsonInputStream ->
                val vpcCFTemplateString = IOUtils.toString(vpcJsonInputStream)
                val stackRequest = createStackRequest(vpcStackName, vpcName, vpcCFTemplateString)
                client.createStack(stackRequest)

                val outputForRequest = getOutputForRequest(vpcStackName, client)
                vpcId = outputForRequest[0].outputValue
                subnetId = outputForRequest[1].outputValue

                if (vpcId.isEmpty() || subnetId.isEmpty()) {
                    LOGGER.error("vpcId or subnetId is empty")
                    throw RuntimeException()
                }
            })
        } catch (e: IOException) {
            LOGGER.error("can't read vpc cloudformation template file")
            throw RuntimeException(e)
        }

        val networkJson = NetworkJson()
        networkJson.name = networkName
        networkJson.description = description
        val map = HashMap<String, Any>()
        map.put("vpcId", vpcId)
        map.put("subnetId", subnetId)
        networkJson.parameters = map
        networkJson.cloudPlatform = "AWS"
        networkJson.isPublicInAccount = publicInAccount
        val id = cloudbreakClient.networkEndpoint().postPrivate(networkJson).id!!.toString()
        itContext.putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true)
    }

    private fun getOutputForRequest(vpcStackName: String, client: AmazonCloudFormationClient): List<Output> {
        var tried = 0
        while (tried < MAX_TRY) {
            LOGGER.info("checking vpc stack creation result, tried: $tried/$MAX_TRY")
            val describeStacksRequest = DescribeStacksRequest()
            describeStacksRequest.withStackName(vpcStackName)
            val resultStack = client.describeStacks(describeStacksRequest).stacks[0]
            val stackStatus = StackStatus.valueOf(resultStack.stackStatus)
            if (FAILED_STATUSES.contains(stackStatus)) {
                LOGGER.error("stack creation failed: ", stackStatus)
                throw RuntimeException()
            } else if (CREATE_COMPLETE == stackStatus) {
                return resultStack.outputs
            }
            try {
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
                LOGGER.error("thread sleep interrupted", e)
            }

            tried++
        }
        throw RuntimeException("vpc creation timed out")
    }

    private fun createStackRequest(vpcStackName: String, vpcName: String, vpcCFTemplateString: String): CreateStackRequest {
        val createStackRequest = CreateStackRequest()
        val vpcNameParameter = Parameter()
        vpcNameParameter.parameterKey = "VpcName"
        vpcNameParameter.parameterValue = vpcName
        createStackRequest.withParameters(vpcNameParameter)
        createStackRequest.withStackName(vpcStackName)
        createStackRequest.withTemplateBody(vpcCFTemplateString)
        return createStackRequest
    }

    companion object {

        private val FAILED_STATUSES = Arrays.asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE)
        private val LOGGER = LoggerFactory.getLogger(AwsCreateExistingVpcNetworkTest::class.java)
        private val MAX_TRY = 30
    }
}
