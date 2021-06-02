package com.sequenceiq.cloudbreak.cloud.aws.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.cloudformation.model.DescribeStackEventsRequest;
import com.amazonaws.services.cloudformation.model.DescribeStackEventsResult;
import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackEvent;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.LegacyAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

@ExtendWith(MockitoExtension.class)
class AwsCloudFormationErrorMessageProviderTest {

    private static final String REGION = "us-west-1";

    private static final String STACK_NAME = "cf-stack";

    @Mock
    private LegacyAwsClient awsClient;

    @Mock
    private AmazonCloudFormationClient cfRetryClient;

    @Mock
    private AwsEncodedAuthorizationFailureMessageDecoder awsEncodedAuthorizationFailureMessageDecoder;

    @Mock
    private AwsCredentialView credentialView;

    @InjectMocks
    private AwsCloudFormationErrorMessageProvider underTest;

    @BeforeEach
    void setUp() {
        when(awsClient.createCloudFormationClient(credentialView, REGION)).thenReturn(cfRetryClient);
        lenient().when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(any(), eq(REGION), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(2));
    }

    @Test
    void shouldReportAddressLimitExceededOnCreate() {
        when(cfRetryClient.describeStacks(any())).thenReturn(new DescribeStacksResult().withStacks(
                new Stack().withStackStatusReason("The following resource(s) failed to create: [EIPmaster01, ClusterNodeSecurityGroupmaster0].")));
        when(cfRetryClient.describeStackResources(any())).thenReturn(new DescribeStackResourcesResult().withStackResources(
                new StackResource().withLogicalResourceId("ClusterNodeSecurityGroupmaster0")
                        .withResourceStatus(ResourceStatus.CREATE_FAILED.toString())
                        .withResourceStatusReason("Resource creation cancelled"),
                new StackResource().withLogicalResourceId("EIPmaster01")
                        .withResourceStatus(ResourceStatus.CREATE_FAILED.toString())
                        .withResourceStatusReason("The maximum number of addresses has been reached. (Service: AmazonEC2; Status Code: 400; Error Code: " +
                                "AddressLimitExceeded; Request ID: ee8b7a70-a1bf-4b67-b9da-f6f6d258bfd4; Proxy: null)"),
                new StackResource().withLogicalResourceId("HealthyResource")
                        .withResourceStatus(ResourceStatus.CREATE_COMPLETE.toString())
                        .withResourceStatusReason("Created")
        ));
        when(cfRetryClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(new DescribeStackEventsResult());

        String result = underTest.getErrorReason(credentialView, REGION, STACK_NAME, ResourceStatus.CREATE_FAILED);

        assertEquals("The following resource(s) failed to create: [EIPmaster01, ClusterNodeSecurityGroupmaster0]. " +
                "ClusterNodeSecurityGroupmaster0: Resource creation cancelled, " +
                "EIPmaster01: The maximum number of addresses has been reached. (Service: AmazonEC2; Status Code: 400; Error Code: AddressLimitExceeded; " +
                "Request ID: ee8b7a70-a1bf-4b67-b9da-f6f6d258bfd4; Proxy: null)", result);
    }

    @Test
    void shouldDecodeAuthorizationError() {
        when(cfRetryClient.describeStacks(any())).thenReturn(new DescribeStacksResult().withStacks(
                new Stack().withStackStatusReason("The following resource(s) failed to create: [ClusterNodeSecurityGroupmaster0].")));
        String resourceStatusReason = "Resource creation cancelled";
        when(cfRetryClient.describeStackResources(any())).thenReturn(new DescribeStackResourcesResult().withStackResources(
                new StackResource().withLogicalResourceId("ClusterNodeSecurityGroupmaster0")
                        .withResourceStatus(ResourceStatus.CREATE_FAILED.toString())
                        .withResourceStatusReason(resourceStatusReason)));
        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(any(), eq(REGION), anyString()))
                .thenReturn("Decoded auth error");
        when(cfRetryClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(new DescribeStackEventsResult());

        String result = underTest.getErrorReason(credentialView, REGION, STACK_NAME, ResourceStatus.CREATE_FAILED);

        assertEquals("The following resource(s) failed to create: [ClusterNodeSecurityGroupmaster0]. " +
                "ClusterNodeSecurityGroupmaster0: Decoded auth error", result);
        verify(awsEncodedAuthorizationFailureMessageDecoder).decodeAuthorizationFailureMessageIfNeeded(credentialView, REGION, resourceStatusReason);
    }

    @Test
    void testWhenMessageExtractedFromCfEvent() {

        StackEvent event = new StackEvent().withResourceStatus(ResourceStatus.CREATE_FAILED).withResourceStatusReason("Error");

        DescribeStackEventsResult result = new DescribeStackEventsResult().withStackEvents(event);
        when(cfRetryClient.describeStacks(any())).thenReturn(new DescribeStacksResult());
        when(cfRetryClient.describeStackResources(any())).thenReturn(new DescribeStackResourcesResult());
        when(cfRetryClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(result);

        String actual = underTest.getErrorReason(credentialView, REGION, STACK_NAME, ResourceStatus.CREATE_FAILED);

        assertEquals(actual, event.getResourceStatusReason());
    }

    @Test
    void testWhenMessageExtractedFromStackStatusAndCfEvent() {

        StackEvent event = new StackEvent().withResourceStatus(ResourceStatus.CREATE_FAILED).withResourceStatusReason("Error");
        Stack stack = new Stack().withStackStatusReason("Stack error");

        DescribeStackEventsResult result = new DescribeStackEventsResult().withStackEvents(event);
        when(cfRetryClient.describeStacks(any())).thenReturn(new DescribeStacksResult().withStacks(stack));
        when(cfRetryClient.describeStackResources(any())).thenReturn(new DescribeStackResourcesResult());
        when(cfRetryClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(result);

        String actual = underTest.getErrorReason(credentialView, REGION, STACK_NAME, ResourceStatus.CREATE_FAILED);

        assertEquals(actual, stack.getStackStatusReason() + " " + event.getResourceStatusReason());
    }

}
