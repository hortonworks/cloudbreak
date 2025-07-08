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
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DeleteStackRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourceRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;

class AmazonCloudFormationClientTest {

    private CloudFormationClient cloudFormationClient;

    private AmazonCloudFormationClient underTest;

    @BeforeEach
    void setUp() {
        cloudFormationClient = mock(CloudFormationClient.class);
        Retry retry = new RetryService();
        underTest = new AmazonCloudFormationClient(cloudFormationClient, retry);
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable"})
    void testDescribeStacksRetriableErrorCodes(String errorCode) {
        DescribeStacksRequest describeStacksRequest = mock(DescribeStacksRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(cloudFormationClient.describeStacks(describeStacksRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeStacks(describeStacksRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable"})
    void testCreateStackRetriableErrorCodes(String errorCode) {
        CreateStackRequest createStackRequest = mock(CreateStackRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(cloudFormationClient.createStack(createStackRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.createStack(createStackRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable"})
    void testDeleteStackRetriableErrorCodes(String errorCode) {
        DeleteStackRequest deleteStackRequest = mock(DeleteStackRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(cloudFormationClient.deleteStack(deleteStackRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.deleteStack(deleteStackRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable"})
    void testDescribeStackResourceRetriableErrorCodes(String errorCode) {
        DescribeStackResourceRequest describeStackResourceRequest = mock(DescribeStackResourceRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(cloudFormationClient.describeStackResource(describeStackResourceRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeStackResource(describeStackResourceRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable"})
    void testDescribeStackResourcesRetriableErrorCodes(String errorCode) {
        DescribeStackResourcesRequest describeStackResourcesRequest = mock(DescribeStackResourcesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(cloudFormationClient.describeStackResources(describeStackResourcesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeStackResources(describeStackResourcesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable"})
    void testDescribeStackEventsRetriableErrorCodes(String errorCode) {
        DescribeStackEventsRequest describeStackEventsRequest = mock(DescribeStackEventsRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(cloudFormationClient.describeStackEvents(describeStackEventsRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.describeStackEvents(describeStackEventsRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable"})
    void testUpdateStackRetriableErrorCodes(String errorCode) {
        UpdateStackRequest updateStackRequest = mock(UpdateStackRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(cloudFormationClient.updateStack(updateStackRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.updateStack(updateStackRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }

    @ParameterizedTest(name = "When AWS execption code is {0} should throw ActionFailedException")
    @ValueSource(strings = {"InternalFailure", "RequestExpired", "ServiceUnavailable"})
    void testListStackResourcesRetriableErrorCodes(String errorCode) {
        ListStackResourcesRequest listStackResourcesRequest = mock(ListStackResourcesRequest.class);
        AwsServiceException exception = mock(AwsServiceException.class);
        AwsErrorDetails awsErrorDetails = mock(AwsErrorDetails.class);

        when(exception.awsErrorDetails()).thenReturn(awsErrorDetails);
        when(awsErrorDetails.errorCode()).thenReturn(errorCode);

        when(cloudFormationClient.listStackResources(listStackResourcesRequest)).thenThrow(exception);

        ActionFailedException result = assertThrows(ActionFailedException.class, () -> underTest.listStackResources(listStackResourcesRequest));
        assertInstanceOf(AwsServiceException.class, result.getCause());
    }
}