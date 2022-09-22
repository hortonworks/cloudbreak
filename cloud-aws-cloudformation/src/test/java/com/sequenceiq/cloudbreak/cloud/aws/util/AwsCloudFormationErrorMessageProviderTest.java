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

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsEncodedAuthorizationFailureMessageDecoder;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;

import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackEventsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackEvent;
import software.amazon.awssdk.services.cloudformation.model.StackResource;

@ExtendWith(MockitoExtension.class)
class AwsCloudFormationErrorMessageProviderTest {

    private static final String REGION = "us-west-1";

    private static final String STACK_NAME = "cf-stack";

    @Mock
    private AwsCloudFormationClient awsClient;

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
        when(cfRetryClient.describeStacks(any())).thenReturn(DescribeStacksResponse.builder().stacks(
                Stack.builder().stackStatusReason("The following resource(s) failed to create: [EIPmaster01, ClusterNodeSecurityGroupmaster0].")
                        .build()).build());
        when(cfRetryClient.describeStackResources(any())).thenReturn(DescribeStackResourcesResponse.builder().stackResources(
                StackResource.builder().logicalResourceId("ClusterNodeSecurityGroupmaster0")
                        .resourceStatus(ResourceStatus.CREATE_FAILED.toString())
                        .resourceStatusReason("Resource creation cancelled").build(),
                StackResource.builder().logicalResourceId("EIPmaster01")
                        .resourceStatus(ResourceStatus.CREATE_FAILED.toString())
                        .resourceStatusReason("The maximum number of addresses has been reached. (Service: AmazonEC2; Status Code: 400; Error Code: " +
                                "AddressLimitExceeded; Request ID: ee8b7a70-a1bf-4b67-b9da-f6f6d258bfd4; Proxy: null)").build(),
                StackResource.builder().logicalResourceId("HealthyResource")
                        .resourceStatus(ResourceStatus.CREATE_COMPLETE.toString())
                        .resourceStatusReason("Created").build()
        ).build());
        when(cfRetryClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(DescribeStackEventsResponse.builder().build());

        String result = underTest.getErrorReason(credentialView, REGION, STACK_NAME, ResourceStatus.CREATE_FAILED);

        assertEquals("The following resource(s) failed to create: [EIPmaster01, ClusterNodeSecurityGroupmaster0]. " +
                "ClusterNodeSecurityGroupmaster0: Resource creation cancelled, " +
                "EIPmaster01: The maximum number of addresses has been reached. (Service: AmazonEC2; Status Code: 400; Error Code: AddressLimitExceeded; " +
                "Request ID: ee8b7a70-a1bf-4b67-b9da-f6f6d258bfd4; Proxy: null)", result);
    }

    @Test
    void shouldDecodeAuthorizationError() {
        when(cfRetryClient.describeStacks(any())).thenReturn(DescribeStacksResponse.builder().stacks(
                Stack.builder().stackStatusReason("The following resource(s) failed to create: [ClusterNodeSecurityGroupmaster0].").build()).build());
        String resourceStatusReason = "Resource creation cancelled";
        when(cfRetryClient.describeStackResources(any())).thenReturn(DescribeStackResourcesResponse.builder().stackResources(
                StackResource.builder().logicalResourceId("ClusterNodeSecurityGroupmaster0")
                        .resourceStatus(ResourceStatus.CREATE_FAILED.toString())
                        .resourceStatusReason(resourceStatusReason).build()).build());
        when(awsEncodedAuthorizationFailureMessageDecoder.decodeAuthorizationFailureMessageIfNeeded(any(), eq(REGION), anyString()))
                .thenReturn("Decoded auth error");
        when(cfRetryClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(DescribeStackEventsResponse.builder().build());

        String result = underTest.getErrorReason(credentialView, REGION, STACK_NAME, ResourceStatus.CREATE_FAILED);

        assertEquals("The following resource(s) failed to create: [ClusterNodeSecurityGroupmaster0]. " +
                "ClusterNodeSecurityGroupmaster0: Decoded auth error", result);
        verify(awsEncodedAuthorizationFailureMessageDecoder).decodeAuthorizationFailureMessageIfNeeded(credentialView, REGION, resourceStatusReason);
    }

    @Test
    void testWhenMessageExtractedFromCfEvent() {

        StackEvent event = StackEvent.builder().resourceStatus(ResourceStatus.CREATE_FAILED).resourceStatusReason("Error").build();

        DescribeStackEventsResponse response = DescribeStackEventsResponse.builder().stackEvents(event).build();
        when(cfRetryClient.describeStacks(any())).thenReturn(DescribeStacksResponse.builder().build());
        when(cfRetryClient.describeStackResources(any())).thenReturn(DescribeStackResourcesResponse.builder().build());
        when(cfRetryClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(response);

        String actual = underTest.getErrorReason(credentialView, REGION, STACK_NAME, ResourceStatus.CREATE_FAILED);

        assertEquals(actual, event.resourceStatusReason());
    }

    @Test
    void testWhenMessageExtractedFromStackStatusAndCfEvent() {

        StackEvent event = StackEvent.builder().resourceStatus(ResourceStatus.CREATE_FAILED).resourceStatusReason("Error").build();
        Stack stack = Stack.builder().stackStatusReason("Stack error").build();

        DescribeStackEventsResponse response = DescribeStackEventsResponse.builder().stackEvents(event).build();
        when(cfRetryClient.describeStacks(any())).thenReturn(DescribeStacksResponse.builder().stacks(stack).build());
        when(cfRetryClient.describeStackResources(any())).thenReturn(DescribeStackResourcesResponse.builder().build());
        when(cfRetryClient.describeStackEvents(any(DescribeStackEventsRequest.class))).thenReturn(response);

        String actual = underTest.getErrorReason(credentialView, REGION, STACK_NAME, ResourceStatus.CREATE_FAILED);

        assertEquals(actual, stack.stackStatusReason() + " " + event.resourceStatusReason());
    }

}
