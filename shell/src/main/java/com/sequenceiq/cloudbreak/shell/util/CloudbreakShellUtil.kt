package com.sequenceiq.cloudbreak.shell.util

import javax.inject.Inject
import javax.ws.rs.core.Response

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.Status
import com.sequenceiq.cloudbreak.client.CloudbreakClient

@Component
class CloudbreakShellUtil {

    @Inject
    private val cloudbreakClient: CloudbreakClient? = null

    fun checkResponse(operation: String, response: Response) {
        if (Response.Status.Family.SUCCESSFUL != response.statusInfo.family) {
            val errormsg = "Error happened during " + operation + " rest operation: status: " + response.status + ", error: "
            +response.readEntity<String>(String::class.java)
            LOGGER.error(errormsg)
            throw RuntimeException(errormsg)
        }
    }

    @Throws(Exception::class)
    fun waitAndCheckStackStatus(stackId: Long?, desiredStatus: String): WaitResult {
        return waitAndCheckStatus(stackId, desiredStatus, "status")
    }

    @Throws(Exception::class)
    fun waitAndCheckClusterStatus(stackId: Long?, desiredStatus: String): WaitResult {
        return waitAndCheckStatus(stackId, desiredStatus, "clusterStatus")
    }

    @Throws(Exception::class)
    private fun waitAndCheckStatus(stackId: Long?, desiredStatus: String, statusPath: String): WaitResult {
        for (i in 0..MAX_ATTEMPT - 1) {
            val waitResult = waitForStatus(stackId, desiredStatus, statusPath)
            if (waitResult.waitResultStatus == WaitResultStatus.FAILED) {
                return waitResult
            }
        }
        return WaitResult(WaitResultStatus.SUCCESSFUL, "")
    }

    @Throws(Exception::class)
    private fun waitForStatus(stackId: Long?, desiredStatus: String, statusPath: String): WaitResult {
        var waitResult = WaitResult(WaitResultStatus.SUCCESSFUL, "")
        var status: String? = null
        var statusReason: String
        var retryCount = 0
        do {
            LOGGER.info("Waiting for status {}, stack id: {}, current status {} ...", desiredStatus, stackId, status)
            sleep()
            val statusResult = cloudbreakClient!!.stackEndpoint().status(stackId)
            if (statusResult == null || statusResult.isEmpty()) {
                return WaitResult(WaitResultStatus.FAILED, "Status result is empty.")
            }
            status = statusResult[statusPath] as String
            statusReason = statusResult[statusPath + "Reason"] as String
            retryCount++
        } while (desiredStatus != status && !status!!.contains("FAILED") && Status.DELETE_COMPLETED.name != status
                && retryCount < MAX_RETRY)
        LOGGER.info("Status {} for {} is in desired status {}", statusPath, stackId, status)
        if (status.contains("FAILED") || Status.DELETE_COMPLETED.name != desiredStatus && Status.DELETE_COMPLETED.name == status) {
            waitResult = WaitResult(WaitResultStatus.FAILED, statusReason)
        } else if (retryCount == MAX_RETRY) {
            waitResult = WaitResult(WaitResultStatus.FAILED, "Timeout while trying to fetch status.")
        }
        return waitResult
    }

    enum class WaitResultStatus {
        SUCCESSFUL,
        FAILED
    }

    inner class WaitResult(val waitResultStatus: WaitResultStatus, val reason: String)

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudbreakShellUtil::class.java)
        private val MAX_RETRY = 360
        private val POLLING_INTERVAL = 10000
        private val MAX_ATTEMPT = 3

        private fun sleep() {
            try {
                Thread.sleep(POLLING_INTERVAL.toLong())
            } catch (e: InterruptedException) {
                LOGGER.warn("Ex during wait: {}", e)
            }

        }
    }
}
