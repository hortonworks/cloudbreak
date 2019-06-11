package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;
import static java.util.Arrays.asList;

import java.util.List;

import com.amazonaws.services.cloudformation.model.StackStatus;

public class AwsResourceConstants {

    static final List<String> SUSPENDED_PROCESSES = asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification",
            "ScheduledActions", "AddToLoadBalancer", "RemoveFromLoadBalancerLowPriority");

    static final List<StackStatus> ERROR_STATUSES = asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE, DELETE_FAILED);

    private AwsResourceConstants() {
    }
}
