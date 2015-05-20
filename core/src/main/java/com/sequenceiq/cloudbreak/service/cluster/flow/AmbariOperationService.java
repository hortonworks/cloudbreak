package com.sequenceiq.cloudbreak.service.cluster.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.PollingResult;
import com.sequenceiq.cloudbreak.service.PollingService;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

@Service
public class AmbariOperationService {

    public static final int MAX_ATTEMPTS_FOR_AMBARI_OPS = -1;
    public static final int AMBARI_POLLING_INTERVAL = 5000;
    public static final int MAX_ATTEMPTS_FOR_HOSTS = 240;
    public static final int MAX_FAILURE_COUNT = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariOperationService.class);

    @Autowired
    private AmbariOperationsStatusCheckerTask ambariOperationsStatusCheckerTask;
    @Autowired
    private PollingService<AmbariOperations> operationsPollingService;

    public PollingResult waitForAmbariOperations(Stack stack, AmbariClient ambariClient, Map<String, Integer> operationRequests) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.info("Waiting for Ambari operations to finish. [Operation requests: {}]", operationRequests);
        return waitForAmbariOperations(stack, ambariClient, ambariOperationsStatusCheckerTask, operationRequests);
    }

    public PollingResult waitForAmbariOperations(Stack stack, AmbariClient ambariClient, StatusCheckerTask task, Map<String, Integer> operationRequests) {
        return operationsPollingService.pollWithTimeout(task, new AmbariOperations(stack, ambariClient, operationRequests),
                AMBARI_POLLING_INTERVAL, MAX_ATTEMPTS_FOR_AMBARI_OPS, MAX_FAILURE_COUNT);
    }
}
