package com.sequenceiq.environment.environment.service.stack;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptMaker;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.environment.environment.poller.StackPollerProvider;
import com.sequenceiq.environment.exception.DatahubOperationFailedException;

@Service
public class StackPollerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPollerService.class);

    private static final List<Status> SKIPPED_STATES = List.of(
        Status.CREATE_FAILED,
        Status.STOPPED,
        Status.STOP_IN_PROGRESS,
        Status.STOP_REQUESTED,
        Status.DELETE_IN_PROGRESS,
        Status.DELETE_COMPLETED,
        Status.DELETED_ON_PROVIDER_SIDE,
        Status.DELETE_FAILED,
        Status.PRE_DELETE_IN_PROGRESS,
        Status.START_FAILED,
        Status.EXTERNAL_DATABASE_CREATION_FAILED,
        Status.EXTERNAL_DATABASE_DELETION_FINISHED,
        Status.EXTERNAL_DATABASE_DELETION_FAILED,
        Status.EXTERNAL_DATABASE_START_FAILED,
        Status.EXTERNAL_DATABASE_START_FINISHED,
        Status.EXTERNAL_DATABASE_START_IN_PROGRESS,
        Status.EXTERNAL_DATABASE_STOP_FAILED,
        Status.EXTERNAL_DATABASE_STOP_FINISHED,
        Status.EXTERNAL_DATABASE_STOP_IN_PROGRESS
    );

    private final StackV4Endpoint stackV4Endpoint;

    private final StackPollerProvider stackPollerProvider;

    @Value("${env.stack.config.update.polling.maximum.seconds:7200}")
    private Integer maxTime;

    @Value("${env.stack.config.update.sleep.time.seconds:60}")
    private Integer sleepTime;

    public StackPollerService(
        StackV4Endpoint stackV4Endpoint,
        StackPollerProvider stackPollerProvider) {
        this.stackV4Endpoint = stackV4Endpoint;
        this.stackPollerProvider = stackPollerProvider;
    }

    public void updateStackConfigurations(Long envId, String envCrn, String flowId) {
        List<String> stackCrns = getUpdateableStacks(envCrn);
        startStackConfigUpdatePolling(stackCrns,
            stackPollerProvider.stackUpdateConfigPoller(stackCrns, envId, flowId));
    }

    private void startStackConfigUpdatePolling(List<String> stackCrns, AttemptMaker<Void> attemptMaker) {
        if (CollectionUtils.isNotEmpty(stackCrns)) {
            try {
                Polling.stopAfterDelay(maxTime, TimeUnit.SECONDS)
                        .stopIfException(true)
                        .waitPeriodly(sleepTime, TimeUnit.SECONDS)
                        .run(attemptMaker);
            } catch (PollerStoppedException e) {
                LOGGER.info("Datahub starting timed out");
                throw new DatahubOperationFailedException("Datahub starting timed out", e);
            }
        }
    }

    private List<String> getUpdateableStacks(String envCrn) {
        StackViewV4Responses stackViewV4Responses = stackV4Endpoint.list(0L, envCrn, false);
        return stackViewV4Responses.getResponses().stream().
            filter(stack -> !SKIPPED_STATES.contains(stack.getCluster().getStatus()))
            .map(stack -> stack.getCrn())
            .collect(Collectors.toList());
    }
}
