package com.sequenceiq.cloudbreak.cloud.aws.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.ResourceStatus;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResource;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;

@ExtendWith(MockitoExtension.class)
class AwsCloudFormationErrorMessageProviderTest {

    private static final String STACK_NAME = "cf-stack";

    @Mock
    private AmazonCloudFormationRetryClient cfRetryClient;

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

        String result = AwsCloudFormationErrorMessageProvider.getErrorReason(cfRetryClient, STACK_NAME, ResourceStatus.CREATE_FAILED);

        Assert.assertEquals("The following resource(s) failed to create: [EIPmaster01, ClusterNodeSecurityGroupmaster0]. " +
                "ClusterNodeSecurityGroupmaster0: Resource creation cancelled, " +
                "EIPmaster01: The maximum number of addresses has been reached. (Service: AmazonEC2; Status Code: 400; Error Code: AddressLimitExceeded; " +
                "Request ID: ee8b7a70-a1bf-4b67-b9da-f6f6d258bfd4; Proxy: null)", result);

    }

}
