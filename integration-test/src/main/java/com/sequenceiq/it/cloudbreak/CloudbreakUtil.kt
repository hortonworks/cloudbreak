package com.sequenceiq.it.cloudbreak

import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.testng.Assert

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.api.endpoint.StackEndpoint
import com.sequenceiq.cloudbreak.api.model.InstanceGroupJson
import com.sequenceiq.cloudbreak.api.model.StackResponse
import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.client.CloudbreakClient

import groovyx.net.http.HttpResponseException

object CloudbreakUtil {
    private val LOGGER = LoggerFactory.getLogger(CloudbreakUtil::class.java)
    private val MAX_RETRY = 360
    private val POLLING_INTERVAL = 10000

    fun checkResponse(operation: String, response: Response) {
        if (Response.Status.Family.SUCCESSFUL != response.statusInfo.family) {
            val errormsg = "Error happened during " + operation + " rest operation: status: " + response.status + ", error: "
            +response.readEntity<String>(String::class.java)
            LOGGER.error(errormsg)
            throw RuntimeException(errormsg)
        }
    }

    @Throws(Exception::class)
    fun waitAndCheckStackStatus(cloudbreakClient: CloudbreakClient, stackId: String, desiredStatus: String) {
        waitAndCheckStatus(cloudbreakClient, stackId, desiredStatus, "status")
    }

    @Throws(Exception::class)
    fun waitAndCheckClusterStatus(cloudbreakClient: CloudbreakClient, stackId: String, desiredStatus: String) {
        waitAndCheckStatus(cloudbreakClient, stackId, desiredStatus, "clusterStatus")
    }

    @Throws(Exception::class)
    fun waitForStackStatus(cloudbreakClient: CloudbreakClient, stackId: String, desiredStatus: String): WaitResult {
        return waitForStatus(cloudbreakClient, stackId, desiredStatus, "status")
    }

    @Throws(Exception::class)
    fun waitForClusterStatus(cloudbreakClient: CloudbreakClient, stackId: String, desiredStatus: String): WaitResult {
        return waitForStatus(cloudbreakClient, stackId, desiredStatus, "clusterStatus")
    }

    fun checkClusterFailed(stackEndpoint: StackEndpoint, stackId: String, failMessage: String) {
        val stackResponse = stackEndpoint[java.lang.Long.valueOf(stackId)]
        Assert.assertEquals(stackResponse.cluster!!.status, "CREATE_FAILED")
        Assert.assertTrue(stackResponse.cluster!!.statusReason!!.contains(failMessage))
    }

    @Throws(Exception::class)
    fun checkClusterAvailability(stackEndpoint: StackEndpoint, port: String, stackId: String, ambariUser: String, ambariPassowrd: String,
                                 checkAmbari: Boolean) {
        val stackResponse = stackEndpoint[java.lang.Long.valueOf(stackId)]

        Assert.assertEquals(stackResponse.cluster!!.status, "AVAILABLE", "The cluster hasn't been started!")
        Assert.assertEquals(stackResponse.status, Status.AVAILABLE, "The stack hasn't been started!")

        val ambariIp = stackResponse.cluster!!.ambariServerIp
        Assert.assertNotNull(ambariIp, "The Ambari IP is not available!")

        if (checkAmbari) {
            val ambariClient = AmbariClient(ambariIp, port, ambariUser, ambariPassowrd)
            Assert.assertEquals(ambariClient.healthCheck(), "RUNNING", "The Ambari server is not running!")
            Assert.assertEquals(ambariClient.clusterHosts.size, getNodeCount(stackResponse) - 1,
                    "The number of cluster nodes in the stack differs from the number of nodes registered in ambari")
        }
    }

    @Throws(Exception::class)
    fun checkClusterStopped(stackEndpoint: StackEndpoint, port: String, stackId: String, ambariUser: String, ambariPassword: String) {
        val stackResponse = stackEndpoint[java.lang.Long.valueOf(stackId)]

        Assert.assertEquals(stackResponse.cluster!!.status, "STOPPED", "The cluster is not stopped!")
        Assert.assertEquals(stackResponse.status, Status.STOPPED, "The stack is not stopped!")

        val ambariIp = stackResponse.cluster!!.ambariServerIp
        val ambariClient = AmbariClient(ambariIp, port, ambariUser, ambariPassword)
        Assert.assertFalse(isAmbariRunning(ambariClient), "The Ambari server is running in stopped state!")
    }

    fun isAmbariRunning(ambariClient: AmbariClient): Boolean {
        try {
            val ambariHealth = ambariClient.healthCheck()
            if ("RUNNING" == ambariHealth) {
                return true
            }
            return false
        } catch (e: Exception) {
            return false
        }

    }

    @Throws(Exception::class)
    private fun waitAndCheckStatus(cloudbreakClient: CloudbreakClient, stackId: String, desiredStatus: String, statusPath: String) {
        for (i in 0..2) {
            val waitResult = waitForStatus(cloudbreakClient, stackId, desiredStatus, statusPath)
            if (waitResult == WaitResult.FAILED) {
                Assert.fail("The stack has failed")
            }
            if (waitResult == WaitResult.TIMEOUT) {
                Assert.fail("Timeout happened")
            }
        }
    }

    @Throws(Exception::class)
    private fun waitForStatus(cloudbreakClient: CloudbreakClient, stackId: String, desiredStatus: String, statusPath: String): WaitResult {
        var waitResult = WaitResult.SUCCESSFUL
        var stackStatus: String? = null
        var retryCount = 0
        do {
            LOGGER.info("Waiting for stack status {}, stack id: {}, current status {} ...", desiredStatus, stackId, stackStatus)
            sleep()
            val stackEndpoint = cloudbreakClient.stackEndpoint()
            try {
                val statusResult = stackEndpoint.status(java.lang.Long.valueOf(stackId))
                stackStatus = statusResult[statusPath] as String
            } catch (exception: Exception) {
                if (exception is HttpResponseException && exception.statusCode == HttpStatus.NOT_FOUND.value()) {
                    stackStatus = "DELETE_COMPLETED"
                } else {
                    continue
                }
            }

            retryCount++
        } while (desiredStatus != stackStatus && !stackStatus!!.contains("FAILED") && "DELETE_COMPLETED" != stackStatus && retryCount < MAX_RETRY)
        LOGGER.info("Status {} for {} is in desired status {}", statusPath, stackId, stackStatus)
        if (stackStatus.contains("FAILED") || "DELETE_COMPLETED" != desiredStatus && "DELETE_COMPLETED" == stackStatus) {
            waitResult = WaitResult.FAILED
        }
        if (retryCount == MAX_RETRY) {
            waitResult = WaitResult.TIMEOUT
        }
        return waitResult
    }

    private fun sleep() {
        try {
            Thread.sleep(POLLING_INTERVAL.toLong())
        } catch (e: InterruptedException) {
            LOGGER.warn("Ex during wait: {}", e)
        }

    }

    private fun getNodeCount(stackResponse: StackResponse): Int {
        val instanceGroups = stackResponse.instanceGroups
        var nodeCount = 0
        for (instanceGroup in instanceGroups) {
            nodeCount += instanceGroup.nodeCount
        }
        return nodeCount
    }
}
