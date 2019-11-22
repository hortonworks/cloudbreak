package com.sequenceiq.cloudbreak.cloud.aws.poller;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.dyngr.Polling;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.aws.AwsInstanceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

@Service
public class PollerUtil {

    @Value("${cb.vm.status.polling.interval:5}")
    private Integer pollingInterval;

    @Value("${cb.vm.status.polling.attempt:200}")
    private Integer pollingAttempt;

    @Inject
    private AwsInstanceConnector awsInstanceConnector;

    public List<CloudVmInstanceStatus> waitFor(AuthenticatedContext authenticatedContext, List<CloudInstance> instances,
            Set<InstanceStatus> completedStatuses) {
        return Polling.stopAfterAttempt(pollingAttempt)
                .waitPeriodly(pollingInterval, TimeUnit.SECONDS)
                .stopIfException(true)
                .run(process(authenticatedContext, instances, completedStatuses));
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
