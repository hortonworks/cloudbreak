package com.sequenceiq.cloudbreak.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PollingService<T> {

    /**
     * Executes a [StatusCheckerTask] until it signals success, or the
     * maximum attempts are reached. A [StatusCheckerTask] has no
     * restrictions about what kind of tasks it should do, it just needs to
     * return if the task succeeded or not. If maxAttempts is lower than 0,
     * there will be no timeout.

     * @param interval    sleeps this many milliseconds between status checking attempts
     * *
     * @param maxAttempts signals how many times will the status check be executed before timeout
     */
    fun pollWithTimeout(statusCheckerTask: StatusCheckerTask<T>, t: T, interval: Int, maxAttempts: Int, maxFailure: Int): PollingResult {
        var success = false
        var timeout = false
        var attempts = 0
        var failures = 0
        var actual: Exception? = null
        var exit = statusCheckerTask.exitPolling(t)
        while (!success && !timeout && !exit) {
            LOGGER.info("Polling attempt {}.", attempts)
            try {
                success = statusCheckerTask.checkStatus(t)
            } catch (ex: Exception) {
                LOGGER.warn("Exception occurred in the polling: {}", ex.message, ex)
                failures++
                actual = ex
            }

            if (failures >= maxFailure) {
                LOGGER.info("Polling failure reached the limit which was {}, poller will drop the last exception.", maxFailure)
                statusCheckerTask.handleException(actual)
                return PollingResult.FAILURE
            }
            if (success) {
                LOGGER.info(statusCheckerTask.successMessage(t))
                return PollingResult.SUCCESS
            }
            sleep(interval)
            attempts++
            if (maxAttempts > 0) {
                timeout = attempts >= maxAttempts
            }
            exit = statusCheckerTask.exitPolling(t)
        }
        if (timeout) {
            LOGGER.info("Poller timeout.")
            statusCheckerTask.handleTimeout(t)
            return PollingResult.TIMEOUT
        } else if (exit) {
            LOGGER.info("Poller exiting.")
            return PollingResult.EXIT
        }
        return PollingResult.SUCCESS
    }

    fun pollWithTimeoutSingleFailure(statusCheckerTask: StatusCheckerTask<T>, t: T, interval: Int, maxAttempts: Int): PollingResult {
        return pollWithTimeout(statusCheckerTask, t, interval, maxAttempts, 1)
    }

    private fun sleep(duration: Int) {
        try {
            Thread.sleep(duration.toLong())
        } catch (e: InterruptedException) {
            LOGGER.info("Interrupted exception occurred during polling.", e)
            Thread.currentThread().interrupt()
        }

    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(PollingService<Any>::class.java)
    }
}