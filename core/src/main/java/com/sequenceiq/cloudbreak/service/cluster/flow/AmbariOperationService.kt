package com.sequenceiq.cloudbreak.service.cluster.flow

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.PollingResult
import com.sequenceiq.cloudbreak.service.PollingService
import com.sequenceiq.cloudbreak.service.StatusCheckerTask

@Service
class AmbariOperationService {

    @Inject
    private val ambariOperationsStatusCheckerTask: AmbariOperationsStatusCheckerTask? = null
    @Inject
    private val ambariOperationsStartCheckerTask: AmbariOperationsStartCheckerTask? = null
    @Inject
    private val requestCheckerTask: AmbariOperationsRequestCheckerTask? = null
    @Inject
    private val operationsPollingService: PollingService<AmbariOperations>? = null

    fun waitForOperations(stack: Stack, ambariClient: AmbariClient,
                          operationRequests: Map<String, Int>, ambariOperationType: AmbariOperationType): PollingResult {
        MDCBuilder.buildMdcContext(stack)
        LOGGER.info("Waiting for Ambari operations to finish. [Operation requests: {}]", operationRequests)
        return waitForOperations(stack, ambariClient, ambariOperationsStatusCheckerTask, operationRequests, ambariOperationType)
    }

    fun waitForOperationsToStart(stack: Stack, ambariClient: AmbariClient,
                                 operationRequests: Map<String, Int>, ambariOperationType: AmbariOperationType): PollingResult {
        MDCBuilder.buildMdcContext(stack)
        LOGGER.info("Waiting for Ambari operations to start. [Operation requests: {}]", operationRequests)
        return waitForOperations(stack, ambariClient, ambariOperationsStartCheckerTask, operationRequests, ambariOperationType)
    }

    fun waitForOperations(stack: Stack, ambariClient: AmbariClient, requestContext: String,
                          desiredOperationStatus: String, ambariOperationType: AmbariOperationType): PollingResult {
        MDCBuilder.buildMdcContext(stack)
        LOGGER.info("Waiting for Ambari operation with context {} to reach status: {}", requestContext, desiredOperationStatus)
        return operationsPollingService!!.pollWithTimeout(requestCheckerTask, AmbariOperations(stack, ambariClient, requestContext,
                desiredOperationStatus, ambariOperationType), AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_AMBARI_OPS, MAX_FAILURE_COUNT)
    }

    fun waitForOperations(stack: Stack, ambariClient: AmbariClient, task: StatusCheckerTask<AmbariOperations>,
                          operationRequests: Map<String, Int>, ambariOperationType: AmbariOperationType): PollingResult {
        return operationsPollingService!!.pollWithTimeout(task, AmbariOperations(stack, ambariClient, operationRequests, ambariOperationType),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_AMBARI_OPS, MAX_FAILURE_COUNT)
    }

    companion object {
        val MAX_ATTEMPTS_FOR_AMBARI_OPS = -1
        val AMBARI_POLLING_INTERVAL = 5000
        val MAX_ATTEMPTS_FOR_HOSTS = 240
        val MAX_ATTEMPTS_FOR_AMBARI_SERVER_STARTUP = 120
        val MAX_FAILURE_COUNT = 5
        private val LOGGER = LoggerFactory.getLogger(AmbariOperationService::class.java)
    }
}
