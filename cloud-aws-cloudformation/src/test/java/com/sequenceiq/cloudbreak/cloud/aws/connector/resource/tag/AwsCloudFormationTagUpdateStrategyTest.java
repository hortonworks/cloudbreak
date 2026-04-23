package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResource;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;

@ExtendWith(MockitoExtension.class)
class AwsCloudFormationTagUpdateStrategyTest {
    private static final String STACK_NAME = "stackName";

    private static final String REGION_NAME = "regionName";

    private static final String LAUNCH_TEMPLATE_ID_1 = "lt-1";

    private static final String LAUNCH_TEMPLATE_ID_2 = "lt-2";

    private static final String ASG_NAME_1 = "asg-1";

    private static final String ASG_NAME_2 = "asg-2";

    private static final String INSTANCE_ID_1 = "i-1";

    private static final String INSTANCE_ID_2 = "i-2";

    private static final String INSTANCE_ID_3 = "i-3";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    private static final Map<String, String> EXISTING_TAGS = Map.of("existingKey", "existingValue");

    private static final Map<String, String> EXPECTED_TAGS = Map.of("existingKey", "existingValue", "custom", "value");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private Location location;

    @Mock
    private Region region;

    @Mock
    private AwsCloudFormationClient awsCloudFormationClient;

    @Mock
    private AmazonCloudFormationClient cloudFormationClient;

    @Mock
    private CommonAwsClient commonAwsClient;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private AmazonAutoScalingClient autoScalingClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @InjectMocks
    private AwsCloudFormationTagUpdateStrategy underTest;

    @BeforeEach
    void setUp() {
        when(authenticatedContext.getCloudContext()).thenReturn(cloudContext);
        when(authenticatedContext.getCloudCredential()).thenReturn(cloudCredential);
        when(cloudContext.getLocation()).thenReturn(location);
        when(location.getRegion()).thenReturn(region);
        when(region.getRegionName()).thenReturn(REGION_NAME);
    }

    @Test
    void testUpdateTagsCloudFormationStack() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        Stack stack = Stack.builder().tags(List.of(Tag.builder()
                .key("existingKey")
                .value("existingValue")
                .build())).build();

        List<Tag> expectedTags = toCloudFormationTags(EXPECTED_TAGS);

        DescribeStacksResponse describeStacksResponse = DescribeStacksResponse.builder().stacks(stack).build();

        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, EXPECTED_TAGS)).thenReturn(expectedTags);
        when(cloudFormationClient.describeStackResources(any(DescribeStackResourcesRequest.class)))
                .thenReturn(DescribeStackResourcesResponse.builder().stackResources(List.of()).build());


        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(cloudFormationClient).updateStack(argThat(req ->
                req.stackName().equals(STACK_NAME)
                        && req.usePreviousTemplate()
                        && req.tags().equals(expectedTags)
        ));
    }

    @Test
    void testUpdateTagsUpdatesLaunchTemplates() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        Stack stack = Stack.builder().tags(List.of(Tag.builder()
                .key("existingKey")
                .value("existingValue")
                .build())).build();

        List<Tag> expectedTags = toCloudFormationTags(EXPECTED_TAGS);

        DescribeStacksResponse describeStacksResponse = DescribeStacksResponse.builder().stacks(stack).build();

        DescribeStackResourcesResponse stackResourcesResponse = DescribeStackResourcesResponse.builder()
                .stackResources(
                        StackResource.builder().resourceType("AWS::EC2::LaunchTemplate").physicalResourceId(LAUNCH_TEMPLATE_ID_1).build(),
                        StackResource.builder().resourceType("AWS::EC2::LaunchTemplate").physicalResourceId(LAUNCH_TEMPLATE_ID_2).build()
                )
                .build();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStackResources(any(DescribeStackResourcesRequest.class)))
                .thenReturn(stackResourcesResponse);

        List<Tag> cfTags = toCloudFormationTags(EXPECTED_TAGS);
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, EXPECTED_TAGS)).thenReturn(cfTags);

        List<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = toEc2Tags(USER_DEFINED_TAGS);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(awsTaggingService.prepareEc2Tags(USER_DEFINED_TAGS)).thenReturn(ec2Tags);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(argThat((CreateTagsRequest req) ->
                req.resources().containsAll(List.of(LAUNCH_TEMPLATE_ID_1, LAUNCH_TEMPLATE_ID_2))
                        && req.tags().equals(ec2Tags)
        ));
    }

    @Test
    void testUpdateTagsUpdatesInstancesAcrossMultipleAutoScalingGroups() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        Stack stack = Stack.builder().tags(List.of(Tag.builder()
                .key("existingKey")
                .value("existingValue")
                .build())).build();

        DescribeStacksResponse describeStacksResponse = DescribeStacksResponse.builder().stacks(stack).build();

        DescribeStackResourcesResponse stackResourcesResponse = DescribeStackResourcesResponse.builder()
                .stackResources(
                        StackResource.builder().resourceType("AWS::AutoScaling::AutoScalingGroup").physicalResourceId(ASG_NAME_1).build(),
                        StackResource.builder().resourceType("AWS::AutoScaling::AutoScalingGroup").physicalResourceId(ASG_NAME_2).build()
                )
                .build();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStackResources(any(DescribeStackResourcesRequest.class)))
                .thenReturn(stackResourcesResponse);

        List<Tag> cfTags = toCloudFormationTags(EXPECTED_TAGS);
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, EXPECTED_TAGS)).thenReturn(cfTags);

        when(awsCloudFormationClient.createAutoScalingClient(any(AwsCredentialView.class), anyString()))
                .thenReturn(autoScalingClient);

        DescribeAutoScalingGroupsResponse asgResponse = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(
                        AutoScalingGroup.builder()
                                .autoScalingGroupName(ASG_NAME_1)
                                .instances(
                                        software.amazon.awssdk.services.autoscaling.model.Instance.builder()
                                                .instanceId(INSTANCE_ID_1).build(),
                                        software.amazon.awssdk.services.autoscaling.model.Instance.builder()
                                                .instanceId(INSTANCE_ID_2).build()
                                )
                                .build(),
                        AutoScalingGroup.builder()
                                .autoScalingGroupName(ASG_NAME_2)
                                .instances(
                                        software.amazon.awssdk.services.autoscaling.model.Instance.builder()
                                                .instanceId(INSTANCE_ID_3).build()
                                )
                                .build()
                )
                .build();
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(asgResponse);

        List<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = toEc2Tags(USER_DEFINED_TAGS);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(awsTaggingService.prepareEc2Tags(USER_DEFINED_TAGS)).thenReturn(ec2Tags);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(argThat((CreateTagsRequest req) ->
                req.resources().containsAll(List.of(INSTANCE_ID_1, INSTANCE_ID_2, INSTANCE_ID_3))
                        && req.tags().equals(ec2Tags)
        ));
    }

    @Test
    void testUpdateTagsSkipUpdateWhenTagsAlreadyUpToDate() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        Stack stack = Stack.builder().tags(List.of(Tag.builder()
                .key("existingKey")
                .value("existingValue")
                .build())).build();

        DescribeStacksResponse describeStacksResponse = DescribeStacksResponse.builder().stacks(stack).build();

        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_TAGS);

        verify(cloudFormationClient, times(0)).updateStack(any(UpdateStackRequest.class));
    }

    private List<Tag> toCloudFormationTags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> Tag.builder()
                        .key(e.getKey()).value(e.getValue()).build())
                .toList();
    }

    private List<software.amazon.awssdk.services.ec2.model.Tag> toEc2Tags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> software.amazon.awssdk.services.ec2.model.Tag.builder().key(e.getKey()).value(e.getValue()).build())
                .toList();
    }
}