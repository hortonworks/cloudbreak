package com.sequenceiq.cloudbreak.cloud.aws.client;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.Retry.ActionFailedException;
import com.sequenceiq.cloudbreak.service.RetryService;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.autoscaling.AutoScalingClient;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesRequest;
import software.amazon.awssdk.services.autoscaling.model.DetachInstancesRequest;
import software.amazon.awssdk.services.autoscaling.model.ResumeProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.SuspendProcessesRequest;
import software.amazon.awssdk.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import software.amazon.awssdk.services.autoscaling.model.UpdateAutoScalingGroupRequest;

class AmazonAutoScalingClientTest {

    private AutoScalingClient autoScalingClient;

    private AmazonAutoScalingClient underTest;

    @BeforeEach
    void setUp() {
        autoScalingClient = mock(AutoScalingClient.class);
        Retry retry = new RetryService();
        underTest = new AmazonAutoScalingClient(autoScalingClient, retry);
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "ResourceInUse", "ServiceLinkedRoleFailure"})
    void testSuspendProcessesRetriableErrorCodes(String errorCode) {
        SuspendProcessesRequest suspendProcessesRequest = mock(SuspendProcessesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(autoScalingClient.suspendProcesses(suspendProcessesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.suspendProcesses(suspendProcessesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "ResourceInUse", "ServiceLinkedRoleFailure"})
    void testResumeProcessesRetriableErrorCodes(String errorCode) {
        ResumeProcessesRequest resumeProcessesRequest = mock(ResumeProcessesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(autoScalingClient.resumeProcesses(resumeProcessesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.resumeProcesses(resumeProcessesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "ResourceInUse", "ServiceLinkedRoleFailure"})
    void testDescribeAutoScalingGroupsRetriableErrorCodes(String errorCode) {
        DescribeAutoScalingGroupsRequest describeAutoScalingGroupsRequest = mock(DescribeAutoScalingGroupsRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(autoScalingClient.describeAutoScalingGroups(describeAutoScalingGroupsRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeAutoScalingGroups(describeAutoScalingGroupsRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "ResourceInUse", "ServiceLinkedRoleFailure"})
    void testUpdateAutoScalingGroupRetriableErrorCodes(String errorCode) {
        UpdateAutoScalingGroupRequest updateAutoScalingGroupRequest = mock(UpdateAutoScalingGroupRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(autoScalingClient.updateAutoScalingGroup(updateAutoScalingGroupRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.updateAutoScalingGroup(updateAutoScalingGroupRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "ResourceInUse", "ServiceLinkedRoleFailure"})
    void testDetachInstancesRetriableErrorCodes(String errorCode) {
        DetachInstancesRequest detachInstancesRequest = mock(DetachInstancesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(autoScalingClient.detachInstances(detachInstancesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.detachInstances(detachInstancesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "ResourceInUse", "ServiceLinkedRoleFailure"})
    void testDescribeAutoScalingInstancesRetriableErrorCodes(String errorCode) {
        DescribeAutoScalingInstancesRequest describeAutoScalingInstancesRequest = mock(DescribeAutoScalingInstancesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(autoScalingClient.describeAutoScalingInstances(describeAutoScalingInstancesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () ->
                underTest.describeAutoScalingInstances(describeAutoScalingInstancesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "ResourceInUse", "ServiceLinkedRoleFailure"})
    void testDescribeScalingActivitiesRetriableErrorCodes(String errorCode) {
        DescribeScalingActivitiesRequest describeScalingActivitiesRequest = mock(DescribeScalingActivitiesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(autoScalingClient.describeScalingActivities(describeScalingActivitiesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeScalingActivities(describeScalingActivitiesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable", "ResourceInUse", "ServiceLinkedRoleFailure"})
    void testTerminateInstanceRetriableErrorCodes(String errorCode) {
        TerminateInstanceInAutoScalingGroupRequest terminateInstanceInAutoScalingGroupRequest = mock(TerminateInstanceInAutoScalingGroupRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(autoScalingClient.terminateInstanceInAutoScalingGroup(terminateInstanceInAutoScalingGroupRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.terminateInstance(terminateInstanceInAutoScalingGroupRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }
}