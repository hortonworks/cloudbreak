package com.sequenceiq.cloudbreak.ambari.flow;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.ambari.AmbariOperationType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.cloudbreak.polling.StatusCheckerTask;

@Service
public class AmbariOperationService {
    public static final int MAX_ATTEMPTS_FOR_AMBARI_OPS = -1;

    public static final int AMBARI_POLLING_INTERVAL = 5000;

    public static final int MAX_ATTEMPTS_FOR_HOSTS = 240;

    public static final int MAX_ATTEMPTS_FOR_AMBARI_SERVER_STARTUP = 120;

    public static final int MAX_FAILURE_COUNT = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationService.class);

    @Inject
    private AmbariOperationsStatusCheckerTask ambariOperationsStatusCheckerTask;

    @Inject
    private AmbariOperationsStartCheckerTask ambariOperationsStartCheckerTask;

    @Inject
    private PollingService<AmbariOperations> operationsPollingService;

    public Pair<PollingResult, Exception> waitForOperations(Stack stack, AmbariClient ambariClient,
            Map<String, Integer> operationRequests, AmbariOperationType ambariOperationType) {
        LOGGER.debug("Waiting for Ambari operations to finish. [Operation requests: {}]", operationRequests);
        return waitForOperations(stack, ambariClient, ambariOperationsStatusCheckerTask, operationRequests, ambariOperationType);
    }

    public Pair<PollingResult, Exception> waitForOperationsToStart(Stack stack, AmbariClient ambariClient,
            Map<String, Integer> operationRequests, AmbariOperationType ambariOperationType) {
        LOGGER.debug("Waiting for Ambari operations to start. [Operation requests: {}]", operationRequests);
        return operationsPollingService.pollWithTimeout(ambariOperationsStartCheckerTask, new AmbariOperations(stack, ambariClient, operationRequests,
                        ambariOperationType), AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_AMBARI_OPS, 1);
    }

    public Pair<PollingResult, Exception> waitForOperations(Stack stack, AmbariClient ambariClient, StatusCheckerTask<AmbariOperations> task,
            Map<String, Integer> operationRequests, AmbariOperationType ambariOperationType) {
        return operationsPollingService.pollWithTimeout(task, new AmbariOperations(stack, ambariClient, operationRequests, ambariOperationType),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_AMBARI_OPS, MAX_FAILURE_COUNT);
    }
}
