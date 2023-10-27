package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsStackRequestHelper;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsElasticIpService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsModelService;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsNetworkService;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;
import com.sequenceiq.common.api.type.AdjustmentType;

import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.waiters.DefaultCloudFormationWaiter;

@ExtendWith(MockitoExtension.class)
class AwsLaunchServiceTest {

    @InjectMocks
    private AwsLaunchService underTest;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private AwsNetworkService awsNetworkService;

    @Mock
    private CloudFormationTemplateBuilder cloudFormationTemplateBuilder;

    @Mock
    private AwsStackRequestHelper awsStackRequestHelper;

    @Mock
    private AwsComputeResourceService awsComputeResourceService;

    @Mock
    private AwsResourceConnector awsResourceConnector;

    @Mock
    private AwsAutoScalingService awsAutoScalingService;

    @Mock
    private AwsElasticIpService awsElasticIpService;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AwsCloudWatchService awsCloudWatchService;

    @Mock
    private AwsModelService awsModelService;

    @Mock
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    @Test
    void testLaunchAndWaiterShouldReturnSuccessfullyEvenIfStackGetsModifiedAfterCreation() throws Exception {
        AuthenticatedContext authenticatedContext = mock(AuthenticatedContext.class);
        CloudStack cloudStack = mock(CloudStack.class);
        when(cloudStack.getNetwork()).thenReturn(new Network(new Subnet("cidr")));
        ResourceNotifier resourceNotifier = mock(ResourceNotifier.class);
        when(awsClient.existingKeyPairNameSpecified(any())).thenReturn(Boolean.TRUE);
        when(authenticatedContext.getCloudContext())
                .thenReturn(CloudContext.Builder.builder()
                        .withLocation(Location.location(Region.region("region"), AvailabilityZone.availabilityZone("AZ")))
                        .build());
        AmazonCloudFormationClient amazonCloudFormationClient = mock(AmazonCloudFormationClient.class);
        CloudFormationClient cloudFormationClient = mock(CloudFormationClient.class);
        when(amazonCloudFormationClient.waiters()).thenReturn(DefaultCloudFormationWaiter.builder().client(cloudFormationClient).build());
        when(awsClient.createCloudFormationClient(any(), anyString()))
                .thenReturn(amazonCloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(DescribeStacksResponse.builder()
                .stacks(Stack.builder().stackStatus(StackStatus.UPDATE_COMPLETE).build())
                .build());
        when(cfStackUtil.getOutputs(any(), any())).thenReturn(Map.of("CreatedVpc", "vpc", "CreatedSubnet", "subnet"));
        underTest.launch(authenticatedContext, cloudStack, resourceNotifier, new AdjustmentTypeWithThreshold(AdjustmentType.EXACT, 1L));
        verify(awsResourceConnector, times(1)).check(eq(authenticatedContext), anyList());
    }
}