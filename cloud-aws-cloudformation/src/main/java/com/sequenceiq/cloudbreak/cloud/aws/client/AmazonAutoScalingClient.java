package com.sequenceiq.cloudbreak.cloud.aws.client;


import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INTERNAL_FAILURE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.REQUEST_EXPIRED;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.RESOURCE_IN_USE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.SERVICE_LINKED_ROLE_FAILURE;
import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.SERVICE_UNAVAILABLE;

import java.util.Set;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonClient;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.CreateLaunchConfigurationResponse;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import software.amazon.awssdk.services.autoscaling.model.DeleteLaunchConfigurationResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeLaunchConfigurationsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesResponse;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesResponse;
import software.amazon.awssdk.services.autoscaling.model.SuspendProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.SuspendProcessesResponse;
import software.amazon.awssdk.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.TerminateInstanceInAutoScalingGroupResponse;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupResponse;
import software.amazon.awssdk.services.autoscaling.waiters.AutoScalingWaiter;

public class AmazonAutoScalingClient extends AmazonClient {

    private static final Set<String> RETRIABLE_ERRORS = Set.of(INTERNAL_FAILURE, REQUEST_EXPIRED, SERVICE_UNAVAILABLE, RESOURCE_IN_USE,
            SERVICE_LINKED_ROLE_FAILURE);

    private final AutoScalingClient client;

    private final Retry retry;

    public AmazonAutoScalingClient(AutoScalingClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public SuspendProcessesResponse suspendProcesses(SuspendProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.suspendProcesses(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public ResumeProcessesResponse resumeProcesses(ResumeProcessesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.resumeProcesses(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeAutoScalingGroupsResponse describeAutoScalingGroups(DescribeAutoScalingGroupsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeAutoScalingGroups(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public UpdateAutoScalingGroupResponse updateAutoScalingGroup(UpdateAutoScalingGroupRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.updateAutoScalingGroup(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DetachInstancesResponse detachInstances(DetachInstancesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.detachInstances(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeAutoScalingInstancesResponse describeAutoScalingInstances(DescribeAutoScalingInstancesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeAutoScalingInstances(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeScalingActivitiesResponse describeScalingActivities(DescribeScalingActivitiesRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.describeScalingActivities(request);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public TerminateInstanceInAutoScalingGroupResponse terminateInstance(TerminateInstanceInAutoScalingGroupRequest terminateInstanceInAutoScalingGroupRequest) {
        return retry.testWith2SecDelayMax15Times(() -> {
            try {
                return client.terminateInstanceInAutoScalingGroup(terminateInstanceInAutoScalingGroupRequest);
            } catch (AwsServiceException ex) {
                throw createActionFailedExceptionIfRetriableError(ex);
            }
        });
    }

    public DescribeLaunchConfigurationsResponse describeLaunchConfigurations(DescribeLaunchConfigurationsRequest launchConfigurationsRequest) {
        return client.describeLaunchConfigurations(launchConfigurationsRequest);
    }

    public DeleteLaunchConfigurationResponse deleteLaunchConfiguration(DeleteLaunchConfigurationRequest deleteLaunchConfigurationRequest) {
        return client.deleteLaunchConfiguration(deleteLaunchConfigurationRequest);
    }

    // FIXME return actual waiter instead
    public AutoScalingWaiter waiters() {
        return client.waiter();
    }

    public CreateLaunchConfigurationResponse createLaunchConfiguration(CreateLaunchConfigurationRequest createLaunchConfigurationRequest) {
        return client.createLaunchConfiguration(createLaunchConfigurationRequest);
    }

    private RuntimeException createActionFailedExceptionIfRetriableError(AwsServiceException ex) {
        if (ex.awsErrorDetails() != null) {
            String errorCode = ex.awsErrorDetails().errorCode();
            if (RETRIABLE_ERRORS.contains(errorCode)) {
                return new ActionFailedException(ex);
            }
        }
        return ex;
    }
}
