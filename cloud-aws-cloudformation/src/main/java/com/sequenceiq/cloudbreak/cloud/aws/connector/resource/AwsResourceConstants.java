package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static java.util.Arrays.asList;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.DELETE_FAILED;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static software.amazon.awssdk.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;

import java.util.Collections;
import java.util.List;

import software.amazon.awssdk.services.cloudformation.model.StackStatus;

public class AwsResourceConstants {

    public static final List<StackStatus> ERROR_STATUSES =
            Collections.unmodifiableList(List.of(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE, DELETE_FAILED));

    static final List<String> SUSPENDED_PROCESSES = asList("Launch", "HealthCheck", "ReplaceUnhealthy", "AZRebalance", "AlarmNotification",
            "ScheduledActions", "AddToLoadBalancer", "RemoveFromLoadBalancerLowPriority");

    private AwsResourceConstants() { }
}
