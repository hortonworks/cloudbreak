package com.sequenceiq.it.cloudbreak

import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Optional
import org.testng.annotations.Parameters
import org.testng.annotations.Test

import com.sequenceiq.cloudbreak.api.model.StatusRequest
import com.sequenceiq.cloudbreak.api.model.UpdateClusterJson
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson
import com.sequenceiq.it.IntegrationTestContext

class StatusUpdateTest : AbstractCloudbreakIntegrationTest() {

    @BeforeMethod
    fun setContextParameters() {
        val itContext = itContext
        Assert.assertNotNull(itContext.getContextParam(CloudbreakITContextConstants.STACK_ID), "Stack id is mandatory.")
    }

    @Test
    @Parameters("newStatus")
    @Throws(Exception::class)
    fun testStatusUpdate(@Optional(STOPPED) newStatus: String) {
        // GIVEN
        val itContext = itContext
        val stackId = itContext.getContextParam(CloudbreakITContextConstants.STACK_ID)
        val stackIntId = Integer.valueOf(stackId)
        // WHEN
        if (newStatus == STOPPED) {
            val updateClusterJson = UpdateClusterJson()
            updateClusterJson.status = StatusRequest.valueOf(newStatus)
            CloudbreakUtil.checkResponse("StopCluster", cloudbreakClient.clusterEndpoint().put(java.lang.Long.valueOf(stackIntId!!.toLong()), updateClusterJson))
            CloudbreakUtil.waitAndCheckClusterStatus(cloudbreakClient, stackId, STOPPED)
            val updateStackJson = UpdateStackJson()
            updateStackJson.status = StatusRequest.valueOf(newStatus)
            CloudbreakUtil.checkResponse("StopStack", cloudbreakClient.stackEndpoint().put(java.lang.Long.valueOf(stackIntId.toLong()), updateStackJson))
            CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, STOPPED)
        } else {
            val updateStackJson = UpdateStackJson()
            updateStackJson.status = StatusRequest.valueOf(newStatus)
            CloudbreakUtil.checkResponse("StartStack", cloudbreakClient.stackEndpoint().put(java.lang.Long.valueOf(stackIntId!!.toLong()), updateStackJson))
            CloudbreakUtil.waitAndCheckStackStatus(cloudbreakClient, stackId, "AVAILABLE")
            val updateClusterJson = UpdateClusterJson()
            updateClusterJson.status = StatusRequest.valueOf(newStatus)
            CloudbreakUtil.checkResponse("StartCluster", cloudbreakClient.clusterEndpoint().put(java.lang.Long.valueOf(stackIntId.toLong()), updateClusterJson))
            CloudbreakUtil.waitAndCheckClusterStatus(cloudbreakClient, stackId, "AVAILABLE")
        }
        // THEN
        if (newStatus == STARTED) {
            CloudbreakUtil.checkClusterAvailability(cloudbreakClient.stackEndpoint(), "8080", stackId,
                    itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                    itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID), true)
        } else if (newStatus == STOPPED) {
            CloudbreakUtil.checkClusterStopped(cloudbreakClient.stackEndpoint(), "8080", stackId,
                    itContext.getContextParam(CloudbreakITContextConstants.AMBARI_USER_ID),
                    itContext.getContextParam(CloudbreakITContextConstants.AMBARI_PASSWORD_ID))
        }
    }

    companion object {
        private val STOPPED = "STOPPED"
        private val STARTED = "STARTED"
    }
}
