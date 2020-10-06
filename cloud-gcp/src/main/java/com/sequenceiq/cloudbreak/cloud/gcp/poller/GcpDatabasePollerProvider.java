package com.sequenceiq.cloudbreak.cloud.gcp.poller;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.compute.DatabaseServerCheckerService;

@Component
public class GcpDatabasePollerProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcpDatabasePollerProvider.class);

    private final DatabaseServerCheckerService checker;

    public GcpDatabasePollerProvider(DatabaseServerCheckerService checker) {
        this.checker = checker;
    }

    public AttemptMaker<Void> launchDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        return () -> fetchOperationResults(ac, resources, ResourceStatus.CREATED);
    }

    public AttemptMaker<Void> insertUserPoller(AuthenticatedContext ac, List<CloudResource> resources) {
        return () -> fetchOperationResults(ac, resources, ResourceStatus.CREATED);
    }

    public AttemptMaker<Void> terminateDatabasePoller(AuthenticatedContext ac, List<CloudResource> resources) {
        return () -> fetchOperationResults(ac, resources, ResourceStatus.DELETED);
    }

    private AttemptResult<Void> fetchOperationResults(AuthenticatedContext ac, List<CloudResource> resources, ResourceStatus expectedStatus) {
        List<CloudResourceStatus> cloudResourceStatuses = checker.check(ac, resources, expectedStatus);
        if (checkAllResourceIsInTheRightState(cloudResourceStatuses, expectedStatus)) {
            return AttemptResults.finishWith(null);
        } else if (hasFailedState(cloudResourceStatuses)) {
            return AttemptResults.breakFor(getFailedStatus(cloudResourceStatuses));
        }
        return AttemptResults.justContinue();
    }

    private boolean checkAllResourceIsInTheRightState(List<CloudResourceStatus> cloudResourceStatuses, ResourceStatus expectedStatus) {
        List<CloudResourceStatus> collect = cloudResourceStatuses.stream()
                .filter(e -> e.getStatus().equals(expectedStatus))
                .collect(Collectors.toList());
        return collect.size() == cloudResourceStatuses.size();
    }

    private List<CloudResourceStatus> getFailedStates(List<CloudResourceStatus> cloudResourceStatuses) {
        return cloudResourceStatuses.stream()
                .filter(e -> e.getStatus().equals(ResourceStatus.FAILED))
                .collect(Collectors.toList());
    }

    private boolean hasFailedState(List<CloudResourceStatus> cloudResourceStatuses) {
        return !getFailedStates(cloudResourceStatuses).isEmpty();
    }

    private String getFailedStatus(List<CloudResourceStatus> cloudResourceStatuses) {
        return getFailedStates(cloudResourceStatuses).get(0).getStatusReason();
    }
}
