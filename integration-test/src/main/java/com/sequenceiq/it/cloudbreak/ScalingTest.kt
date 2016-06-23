package com.sequenceiq.it.cloudbreak

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.HostGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson
import com.sequenceiq.cloudbreak.client.CloudbreakClient
import com.sequenceiq.it.IntegrationTestContext

class ScalingTest : AbstractCloudbreakIntegrationTest() {

    @BeforeMethod
    fun setContextParameters() {
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.")
    }

    @Test
    @Parameters("instanceGroup", "scalingAdjustment")
    @Throws(Exception::class)
    fun testScaling(@Optional("slave_1") instanceGroup: String, @Optional("1") scalingAdjustment: Int) {
        // GIVEN
        val itContext = itContext
        val stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID)
        val stackIntId = Integer.valueOf(stackId)!!
        // WHEN
        if (scalingAdjustment < 0) {
            val updateClusterJson = UpdateClusterJson()
            val hostGroupAdjustmentJson = HostGroupAdjustmentJson()
            hostGroupAdjustmentJson.hostGroup = instanceGroup
            hostGroupAdjustmentJson.withStackUpdate = false
            hostGroupAdjustmentJson.scalingAdjustment = scalingAdjustment
            updateClusterJson.hostGroupAdjustment = hostGroupAdjustmentJson
            CloudbreakUtil.checkResponse("DownscaleCluster", cloudbreakClient.clusterEndpoint().put(java.lang.Long.valueOf(stackIntId.toLong()), updateClusterJson))
            CloudbreakUtil.waitAndCheckClusterStatus(cloudbreakClient, stackId, "AVAILABLE")

            val updateStackJson = UpdateStackJson()
            val instanceGroupAdjustmentJson = InstanceGroupAdjustmentJson()
            instanceGroupAdjustmentJson.instanceGroup = instanceGroup
            instanceGroupAdjustmentJson.scalingAdjustment = scalingAdjustment
            instanceGroupAdjustmentJson.withClusterEvent = false
            updateStackJson.instanceGroupAdjustment = instanceGroupAdjustmentJson
            CloudbreakUtil.checkResponse("DownscaleStack", cloudbreakClient.stackEndpoint().put(java.lang.Long.valueOf(stackIntId.toLong()), updateStackJson))
            CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, "AVAILABLE")
        } else {
            val updateStackJson = UpdateStackJson()
            val instanceGroupAdjustmentJson = InstanceGroupAdjustmentJson()
            instanceGroupAdjustmentJson.instanceGroup = instanceGroup
            instanceGroupAdjustmentJson.scalingAdjustment = scalingAdjustment
            instanceGroupAdjustmentJson.withClusterEvent = false
            updateStackJson.instanceGroupAdjustment = instanceGroupAdjustmentJson
            CloudbreakUtil.checkResponse("UpscaleStack", cloudbreakClient.stackEndpoint().put(java.lang.Long.valueOf(stackIntId.toLong()), updateStackJson))
            CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, "AVAILABLE")

            val updateClusterJson = UpdateClusterJson()
            val hostGroupAdjustmentJson = HostGroupAdjustmentJson()
            hostGroupAdjustmentJson.hostGroup = instanceGroup
            hostGroupAdjustmentJson.withStackUpdate = false
            hostGroupAdjustmentJson.scalingAdjustment = scalingAdjustment
            updateClusterJson.hostGroupAdjustment = hostGroupAdjustmentJson
            CloudbreakUtil.checkResponse("UpscaleCluster", cloudbreakClient.clusterEndpoint().put(java.lang.Long.valueOf(stackIntId.toLong()), updateClusterJson))
            CloudbreakUtil.waitAndCheckClusterStatus(cloudbreakClient, stackId, "AVAILABLE")
        }
        // THEN
        CloudbreakUtil.checkClusterAvailability(
                itContext.getContextParam<CloudbreakClient>(CloudbreakITContextConstants.CLOUDBREAK_CLIENT, CloudbreakClient::class.java).stackEndpoint(),
                "8080", stackId, itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), true)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ScalingTest::class.java)
    }
}
