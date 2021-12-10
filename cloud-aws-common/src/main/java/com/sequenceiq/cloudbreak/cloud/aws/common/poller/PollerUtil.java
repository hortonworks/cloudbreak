package com.sequenceiq.cloudbreak.cloud.aws.common.poller;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.Polling.PollingOptions;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsInstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class PollerUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(PollerUtil.class);

    @Value("${cb.vm.status.polling.interval:5}")
    private Integer pollingInterval;

    @Value("${cb.vm.status.polling.attempt:200}")
    private Integer pollingAttempt;

    @Inject
    private AwsInstanceConnector awsInstanceConnector;

    public List<CloudVmInstanceStatus> waitFor(AuthenticatedContext authenticatedContext, List<CloudInstance> instances,
            Set<InstanceStatus> completedStatuses, String waitStatus) {
        PollingOptions pollingOptions = constructDefaultPollingOptions();
        return waitForInternal(pollingOptions, authenticatedContext, instances, completedStatuses, waitStatus);
    }

    public List<CloudVmInstanceStatus> timeBoundWaitFor(long timeBoundInMs, AuthenticatedContext authenticatedContext, List<CloudInstance> instances,
            Set<InstanceStatus> completedStatuses, String waitStatus) {
        PollingOptions pollingOptions = constructTimeBoundPollingOptions(timeBoundInMs);
        return waitForInternal(pollingOptions, authenticatedContext, instances, completedStatuses, waitStatus);
    }

    private List<CloudVmInstanceStatus> waitForInternal(PollingOptions pollingOptions,
            AuthenticatedContext authenticatedContext, List<CloudInstance> instances,
            Set<InstanceStatus> completedStatuses, String waitStatus) {
        long start = System.currentTimeMillis();
        try {
            return pollingOptions.run(process(authenticatedContext, instances, completedStatuses));
        } catch (PollerStoppedException e) {
            List<CloudVmInstanceStatus> currentInstances = awsInstanceConnector.check(authenticatedContext, instances);
            long duration = System.currentTimeMillis() - start;
            if (e.getCause() == null) {
                String message = String.format("%s operation cannot be finished in time. Duration: %s. Instances: %s",
                        getOperation(waitStatus), duration, currentInstances);
                LOGGER.error(message);
                throw new PollerStoppedException(message);
            }
            LOGGER.error("{} operation cannot be finished on {}. Duration: {}", getOperation(waitStatus), currentInstances, duration, e);
            throw e;
        }
    }

    private PollingOptions constructDefaultPollingOptions() {
        return Polling.stopAfterAttempt(pollingAttempt)
                .waitPeriodly(pollingInterval, TimeUnit.SECONDS)
                .stopIfException(true);
    }

    private PollingOptions constructTimeBoundPollingOptions(long timeBoundInMs) {
        return Polling.stopAfterDelay(timeBoundInMs, TimeUnit.MILLISECONDS)
                .waitPeriodly(pollingInterval, TimeUnit.SECONDS)
                .stopIfException(true);
    }

    private String getOperation(String waitStatus) {
        String operation = "unknown";
        if ("stopped".equalsIgnoreCase(waitStatus)) {
            operation = "Stop";
        } else if ("running".equalsIgnoreCase(waitStatus)) {
            operation = "Start";
        }
        return operation;
    }

    private CancellableAttemptMaker<List<CloudVmInstanceStatus>> process(AuthenticatedContext authenticatedContext, List<CloudInstance> instances,
            Set<InstanceStatus> completedStatuses) {
        return new CancellableAttemptMaker<>(authenticatedContext.getCloudContext().getId()) {
            @Override
            public AttemptResult<List<CloudVmInstanceStatus>> doProcess() {
                List<CloudVmInstanceStatus> instanceStatuses = awsInstanceConnector.check(authenticatedContext, instances);
                if (completed(instanceStatuses, completedStatuses)) {
                    return AttemptResults.finishWith(instanceStatuses);
                }
                return AttemptResults.justContinue();
            }
        };
    }

    private boolean completed(List<CloudVmInstanceStatus> instanceStatuses, Set<InstanceStatus> completedStatuses) {
        for (CloudVmInstanceStatus status : instanceStatuses) {
            if (status.getStatus().isTransient() || (!completedStatuses.isEmpty() && !completedStatuses.contains(status.getStatus()))) {
                return false;
            }
        }
        return true;
    }
}
