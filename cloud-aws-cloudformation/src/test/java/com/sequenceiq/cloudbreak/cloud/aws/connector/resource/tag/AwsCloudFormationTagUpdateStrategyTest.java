package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.CommonAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonCloudWatchClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsPageCollector;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsCloudWatchService;
import com.sequenceiq.cloudbreak.cloud.aws.util.AwsCloudFormationErrorMessageProvider;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesRequest;
import software.amazon.awssdk.services.cloudformation.model.ListStackResourcesResponse;
import software.amazon.awssdk.services.cloudformation.model.ResourceStatus;
import software.amazon.awssdk.services.cloudformation.model.Stack;
import software.amazon.awssdk.services.cloudformation.model.StackResourceSummary;
import software.amazon.awssdk.services.cloudformation.model.StackStatus;
import software.amazon.awssdk.services.cloudformation.model.Tag;
import software.amazon.awssdk.services.cloudformation.model.UpdateStackRequest;
import software.amazon.awssdk.services.cloudformation.waiters.CloudFormationWaiter;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.UntagResourceRequest;
import software.amazon.awssdk.services.ec2.model.CreateTagsRequest;
import software.amazon.awssdk.services.ec2.model.DeleteTagsRequest;

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

    private static final String ALARM_ARN = "arn:aws:cloudwatch:us-east-1:123:alarm:alarm-i1";

    private static final String NEXT_TOKEN = "nextToken";

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
    private AmazonCloudWatchClient amazonCloudWatchClient;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private AwsCloudWatchService awsCloudWatchService;

    @Mock
    private AwsCloudFormationErrorMessageProvider awsCloudFormationErrorMessageProvider;

    @Mock
    private CloudFormationWaiter cloudFormationWaiter;

    @Spy
    private AwsPageCollector awsPageCollector;

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
    void testDeleteTagsCloudFormationStack() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        Stack stack = Stack.builder().tags(List.of(
                Tag.builder().key("existingKey").value("existingValue").build(),
                Tag.builder().key("survivorKey").value("survivorValue").build()
        )).build();

        DescribeStacksResponse describeStacksResponse = DescribeStacksResponse.builder().stacks(stack).build();

        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, Map.of("survivorKey", "survivorValue")))
                .thenReturn(List.of(Tag.builder().key("survivorKey").value("survivorValue").build()));
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(ListStackResourcesResponse.builder().stackResourceSummaries(List.of()).build());
        stubStackUpdateWaiter();

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        verify(cloudFormationClient).updateStack(argThat(req ->
                req.stackName().equals(STACK_NAME)
                        && req.usePreviousTemplate()
                        && req.tags().size() == 1
                        && req.tags().get(0).key().equals("survivorKey")
                        && req.tags().get(0).value().equals("survivorValue")
        ));
    }

    @Test
    void testDeleteTagsSkipWhenKeyNotPresent() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        Stack stack = Stack.builder().tags(List.of(Tag.builder()
                .key("otherKey")
                .value("otherValue")
                .build())).build();

        DescribeStacksResponse describeStacksResponse = DescribeStacksResponse.builder().stacks(stack).build();

        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(ListStackResourcesResponse.builder().stackResourceSummaries(List.of()).build());

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        verify(cloudFormationClient, times(0)).updateStack(any(UpdateStackRequest.class));
    }

    @Test
    void testUpdateTagsSkipWhenStackDoesNotExist() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(AwsServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorMessage(STACK_NAME + " does not exist")
                                .build())
                        .build());

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(cloudFormationClient, times(0)).updateStack(any(UpdateStackRequest.class));
    }

    @Test
    void testDeleteTagsSkipWhenStackDoesNotExist() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenThrow(AwsServiceException.builder()
                        .awsErrorDetails(AwsErrorDetails.builder()
                                .errorMessage(STACK_NAME + " does not exist")
                                .build())
                        .build());

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("existingKey"));

        verify(cloudFormationClient, times(0)).updateStack(any(UpdateStackRequest.class));
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
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(ListStackResourcesResponse.builder().stackResourceSummaries(List.of()).build());
        stubStackUpdateWaiter();

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(cloudFormationClient).updateStack(argThat(req ->
                req.stackName().equals(STACK_NAME)
                        && req.usePreviousTemplate()
                        && req.tags().equals(expectedTags)
        ));
        verify(cloudFormationWaiter).waitUntilStackUpdateComplete(argThat((DescribeStacksRequest req) -> STACK_NAME.equals(req.stackName())),
                any(WaiterOverrideConfiguration.class));
    }

    @Test
    void testUpdateTagsTreatsNoUpdatesValidationErrorAsSuccess() {
        CloudResource cloudResource = cloudFormationStackResource();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder().stacks(stackWithExistingTags()).build());
        when(cloudFormationClient.updateStack(any(UpdateStackRequest.class))).thenThrow(AwsServiceException.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorCode("ValidationError")
                        .errorMessage("No updates are to be performed.")
                        .build())
                .build());
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(ListStackResourcesResponse.builder().stackResourceSummaries(List.of()).build());

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(cloudFormationClient, never()).waiters();
    }

    @Test
    void testUpdateTagsFailsWhenStackUpdateDoesNotComplete() {
        CloudResource cloudResource = cloudFormationStackResource();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder().stacks(stackWithExistingTags()).build());
        stubStackUpdateWaiter();
        when(cloudFormationWaiter.waitUntilStackUpdateComplete(any(DescribeStacksRequest.class),
                any(WaiterOverrideConfiguration.class)))
                .thenThrow(new IllegalStateException("UPDATE_ROLLBACK_COMPLETE"));
        when(awsCloudFormationErrorMessageProvider.getErrorReason(authenticatedContext, STACK_NAME, ResourceStatus.UPDATE_FAILED))
                .thenReturn("Tag policy violation");

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS));

        assertThat(exception.getMessage())
                .contains("CloudFormation stack stackName tag update failed.")
                .contains("UPDATE_ROLLBACK_COMPLETE")
                .contains("Tag policy violation");
        verify(cloudFormationClient, never()).listStackResources(any(ListStackResourcesRequest.class));
    }

    @Test
    void testUpdateTagsFailsWhenStackOperationIsInProgress() {
        CloudResource cloudResource = cloudFormationStackResource();
        Stack stack = stackWithExistingTags().toBuilder().stackStatus(StackStatus.UPDATE_IN_PROGRESS).build();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(DescribeStacksResponse.builder().stacks(stack).build());

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS));

        assertThat(exception.getMessage()).contains("UPDATE_IN_PROGRESS");
        verify(cloudFormationClient, never()).updateStack(any(UpdateStackRequest.class));
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

        ListStackResourcesResponse stackResourcesResponse = ListStackResourcesResponse.builder()
                .stackResourceSummaries(
                        launchTemplateResource(LAUNCH_TEMPLATE_ID_1),
                        launchTemplateResource(LAUNCH_TEMPLATE_ID_2)
                )
                .build();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(stackResourcesResponse);

        List<Tag> cfTags = toCloudFormationTags(EXPECTED_TAGS);
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, EXPECTED_TAGS)).thenReturn(cfTags);

        List<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = toEc2Tags(USER_DEFINED_TAGS);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(awsTaggingService.prepareEc2Tags(USER_DEFINED_TAGS)).thenReturn(ec2Tags);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);
        stubStackUpdateWaiter();

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(argThat((CreateTagsRequest req) ->
                req.resources().containsAll(List.of(LAUNCH_TEMPLATE_ID_1, LAUNCH_TEMPLATE_ID_2))
                        && req.tags().equals(ec2Tags)
        ));
    }

    @Test
    void testUpdateTagsFollowsEveryStackResourcePage() {
        CloudResource cloudResource = cloudFormationStackResource();

        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder().stacks(stackWithExistingTags()).build());
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(ListStackResourcesResponse.builder()
                                .stackResourceSummaries(launchTemplateResource(LAUNCH_TEMPLATE_ID_1))
                                .nextToken(NEXT_TOKEN)
                                .build(),
                        ListStackResourcesResponse.builder()
                                .stackResourceSummaries(launchTemplateResource(LAUNCH_TEMPLATE_ID_2))
                                .build());
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, EXPECTED_TAGS)).thenReturn(toCloudFormationTags(EXPECTED_TAGS));

        List<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = toEc2Tags(USER_DEFINED_TAGS);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(awsTaggingService.prepareEc2Tags(USER_DEFINED_TAGS)).thenReturn(ec2Tags);
        stubStackUpdateWaiter();

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        ArgumentCaptor<ListStackResourcesRequest> requestCaptor = ArgumentCaptor.forClass(ListStackResourcesRequest.class);
        verify(cloudFormationClient, times(2)).listStackResources(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).extracting(ListStackResourcesRequest::nextToken).containsExactly(null, NEXT_TOKEN);
        verify(ec2Client).createTags(argThat((CreateTagsRequest req) ->
                req.resources().containsAll(List.of(LAUNCH_TEMPLATE_ID_1, LAUNCH_TEMPLATE_ID_2)) && req.tags().equals(ec2Tags)));
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

        ListStackResourcesResponse stackResourcesResponse = ListStackResourcesResponse.builder()
                .stackResourceSummaries(
                        autoScalingGroupResource(ASG_NAME_1),
                        autoScalingGroupResource(ASG_NAME_2)
                )
                .build();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
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
        List<software.amazon.awssdk.services.cloudwatch.model.Tag> cloudWatchTags = toCloudWatchTags(USER_DEFINED_TAGS);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudWatchClient);
        when(awsTaggingService.prepareEc2Tags(USER_DEFINED_TAGS)).thenReturn(ec2Tags);
        when(awsTaggingService.prepareCloudWatchTags(USER_DEFINED_TAGS)).thenReturn(cloudWatchTags);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);
        stubStackUpdateWaiter();

        when(awsCloudWatchService.getSystemFailureAlarmsForInstances(amazonCloudWatchClient, List.of(INSTANCE_ID_1, INSTANCE_ID_2, INSTANCE_ID_3)))
                .thenReturn(List.of(MetricAlarm.builder().alarmArn(ALARM_ARN).build()));

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(ec2Client).createTags(argThat((CreateTagsRequest req) ->
                req.resources().containsAll(List.of(INSTANCE_ID_1, INSTANCE_ID_2, INSTANCE_ID_3))
                        && req.tags().equals(ec2Tags)
        ));

        verify(amazonCloudWatchClient).tagResource(argThat(req ->
                req.resourceARN().equals(ALARM_ARN) && req.tags().equals(cloudWatchTags)));
    }

    @Test
    void testDeleteTagsRemovesTagsFromInstancesAndCloudWatchAlarms() {
        CloudResource cloudResource = CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();

        Stack stack = Stack.builder().tags(List.of(Tag.builder()
                .key("otherKey")
                .value("otherValue")
                .build())).build();

        DescribeStacksResponse describeStacksResponse = DescribeStacksResponse.builder().stacks(stack).build();

        ListStackResourcesResponse stackResourcesResponse = ListStackResourcesResponse.builder()
                .stackResourceSummaries(
                        autoScalingGroupResource(ASG_NAME_1),
                        autoScalingGroupResource(ASG_NAME_2)
                )
                .build();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(describeStacksResponse);
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(stackResourcesResponse);

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

        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudWatchClient);

        when(awsCloudWatchService.getSystemFailureAlarmsForInstances(amazonCloudWatchClient, List.of(INSTANCE_ID_1, INSTANCE_ID_2, INSTANCE_ID_3)))
                .thenReturn(List.of(MetricAlarm.builder().alarmArn(ALARM_ARN).build()));

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("custom"));

        verify(ec2Client).deleteTags(argThat((DeleteTagsRequest req) ->
                req.resources().containsAll(List.of(INSTANCE_ID_1, INSTANCE_ID_2, INSTANCE_ID_3))
                        && req.tags().size() == 1
                        && req.tags().get(0).key().equals("custom")
        ));

        verify(amazonCloudWatchClient).untagResource(argThat((UntagResourceRequest req) ->
                req.resourceARN().equals(ALARM_ARN) && req.tagKeys().equals(List.of("custom"))));
        verify(cloudFormationClient, times(1)).listStackResources(any(ListStackResourcesRequest.class));
    }

    @Test
    void testDeleteTagsChunksInstanceTagDeletion() {
        CloudResource cloudResource = cloudFormationStackResource();
        List<software.amazon.awssdk.services.autoscaling.model.Instance> instances = IntStream.rangeClosed(1, 1001)
                .mapToObj(i -> software.amazon.awssdk.services.autoscaling.model.Instance.builder().instanceId("i-" + i).build())
                .toList();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder().stacks(stackWithExistingTags()).build());
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(ListStackResourcesResponse.builder()
                        .stackResourceSummaries(autoScalingGroupResource(ASG_NAME_1))
                        .build());
        when(awsCloudFormationClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(DescribeAutoScalingGroupsResponse.builder()
                        .autoScalingGroups(AutoScalingGroup.builder().autoScalingGroupName(ASG_NAME_1).instances(instances).build())
                        .build());
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudWatchClient);

        underTest.deleteTags(authenticatedContext, cloudResource, Set.of("custom"));

        ArgumentCaptor<DeleteTagsRequest> captor = ArgumentCaptor.forClass(DeleteTagsRequest.class);
        verify(ec2Client, times(2)).deleteTags(captor.capture());
        assertThat(captor.getAllValues().get(0).resources()).hasSize(1000);
        assertThat(captor.getAllValues().get(1).resources()).containsExactly("i-1001");
    }

    @Test
    void testUpdateTagsReconcilesChildrenWhenStackTagsAlreadyUpToDate() {
        CloudResource cloudResource = cloudFormationStackResource();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder().stacks(stackWithExistingTags()).build());
        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(ListStackResourcesResponse.builder()
                        .stackResourceSummaries(
                                launchTemplateResource(LAUNCH_TEMPLATE_ID_1),
                                autoScalingGroupResource(ASG_NAME_1))
                        .build());
        when(awsCloudFormationClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(DescribeAutoScalingGroupsResponse.builder()
                        .autoScalingGroups(AutoScalingGroup.builder()
                                .autoScalingGroupName(ASG_NAME_1)
                                .instances(software.amazon.awssdk.services.autoscaling.model.Instance.builder().instanceId(INSTANCE_ID_1).build())
                                .build())
                        .build());
        List<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = toEc2Tags(EXISTING_TAGS);
        List<software.amazon.awssdk.services.cloudwatch.model.Tag> cloudWatchTags = toCloudWatchTags(EXISTING_TAGS);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudWatchClient);
        when(awsTaggingService.prepareEc2Tags(EXISTING_TAGS)).thenReturn(ec2Tags);
        when(awsTaggingService.prepareCloudWatchTags(EXISTING_TAGS)).thenReturn(cloudWatchTags);
        when(awsCloudWatchService.getSystemFailureAlarmsForInstances(amazonCloudWatchClient, List.of(INSTANCE_ID_1)))
                .thenReturn(List.of(MetricAlarm.builder().alarmArn(ALARM_ARN).build()));

        underTest.updateTags(authenticatedContext, cloudResource, EXISTING_TAGS);

        verify(cloudFormationClient, never()).updateStack(any(UpdateStackRequest.class));
        verify(ec2Client).createTags(argThat((CreateTagsRequest req) -> req.resources().equals(List.of(LAUNCH_TEMPLATE_ID_1)) && req.tags().equals(ec2Tags)));
        verify(ec2Client).createTags(argThat((CreateTagsRequest req) -> req.resources().equals(List.of(INSTANCE_ID_1)) && req.tags().equals(ec2Tags)));
        verify(amazonCloudWatchClient).tagResource(argThat(req -> req.resourceARN().equals(ALARM_ARN) && req.tags().equals(cloudWatchTags)));
        verify(cloudFormationClient, times(1)).listStackResources(any(ListStackResourcesRequest.class));
    }

    @Test
    void testUpdateTagsReconcilesChildrenOnRetryAfterStackUpdateWaitFailed() {
        CloudResource cloudResource = cloudFormationStackResource();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        // AWS applied the tags, then the wait failed, so the retry sees a stack that is already up to date
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class)))
                .thenReturn(DescribeStacksResponse.builder().stacks(stackWithExistingTags()).build(),
                        DescribeStacksResponse.builder().stacks(stackWithTags(EXPECTED_TAGS)).build());
        when(awsTaggingService.prepareCloudformationTags(authenticatedContext, EXPECTED_TAGS)).thenReturn(toCloudFormationTags(EXPECTED_TAGS));
        stubStackUpdateWaiter();
        when(cloudFormationWaiter.waitUntilStackUpdateComplete(any(DescribeStacksRequest.class), any(WaiterOverrideConfiguration.class)))
                .thenThrow(new IllegalStateException("UPDATE_ROLLBACK_COMPLETE"));
        when(awsCloudFormationErrorMessageProvider.getErrorReason(authenticatedContext, STACK_NAME, ResourceStatus.UPDATE_FAILED))
                .thenReturn("Tag policy violation");

        assertThrows(CloudConnectorException.class, () -> underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS));

        when(cloudFormationClient.listStackResources(any(ListStackResourcesRequest.class)))
                .thenReturn(ListStackResourcesResponse.builder()
                        .stackResourceSummaries(
                                launchTemplateResource(LAUNCH_TEMPLATE_ID_1),
                                autoScalingGroupResource(ASG_NAME_1))
                        .build());
        when(awsCloudFormationClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(autoScalingClient);
        when(autoScalingClient.describeAutoScalingGroups(any(DescribeAutoScalingGroupsRequest.class)))
                .thenReturn(DescribeAutoScalingGroupsResponse.builder()
                        .autoScalingGroups(AutoScalingGroup.builder()
                                .autoScalingGroupName(ASG_NAME_1)
                                .instances(software.amazon.awssdk.services.autoscaling.model.Instance.builder().instanceId(INSTANCE_ID_1).build())
                                .build())
                        .build());
        List<software.amazon.awssdk.services.ec2.model.Tag> ec2Tags = toEc2Tags(USER_DEFINED_TAGS);
        List<software.amazon.awssdk.services.cloudwatch.model.Tag> cloudWatchTags = toCloudWatchTags(USER_DEFINED_TAGS);
        when(commonAwsClient.createEc2Client(authenticatedContext)).thenReturn(ec2Client);
        when(commonAwsClient.createCloudWatchClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudWatchClient);
        when(awsTaggingService.prepareEc2Tags(USER_DEFINED_TAGS)).thenReturn(ec2Tags);
        when(awsTaggingService.prepareCloudWatchTags(USER_DEFINED_TAGS)).thenReturn(cloudWatchTags);
        when(awsCloudWatchService.getSystemFailureAlarmsForInstances(amazonCloudWatchClient, List.of(INSTANCE_ID_1)))
                .thenReturn(List.of(MetricAlarm.builder().alarmArn(ALARM_ARN).build()));

        underTest.updateTags(authenticatedContext, cloudResource, USER_DEFINED_TAGS);

        verify(cloudFormationClient, times(1)).updateStack(any(UpdateStackRequest.class));
        verify(ec2Client).createTags(argThat((CreateTagsRequest req) -> req.resources().equals(List.of(LAUNCH_TEMPLATE_ID_1)) && req.tags().equals(ec2Tags)));
        verify(ec2Client).createTags(argThat((CreateTagsRequest req) -> req.resources().equals(List.of(INSTANCE_ID_1)) && req.tags().equals(ec2Tags)));
        verify(amazonCloudWatchClient).tagResource(argThat(req -> req.resourceARN().equals(ALARM_ARN) && req.tags().equals(cloudWatchTags)));
    }

    @Test
    void testDeleteTagsFailsWhenStackOperationIsInProgressEvenIfKeyIsAbsent() {
        CloudResource cloudResource = cloudFormationStackResource();
        Stack stack = stackWithExistingTags().toBuilder().stackStatus(StackStatus.UPDATE_IN_PROGRESS).build();
        when(awsCloudFormationClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(cloudFormationClient);
        when(cloudFormationClient.describeStacks(any(DescribeStacksRequest.class))).thenReturn(DescribeStacksResponse.builder().stacks(stack).build());

        CloudConnectorException exception = assertThrows(CloudConnectorException.class,
                () -> underTest.deleteTags(authenticatedContext, cloudResource, Set.of("custom")));

        assertThat(exception.getMessage()).contains("UPDATE_IN_PROGRESS");
        verify(cloudFormationClient, never()).updateStack(any(UpdateStackRequest.class));
        verify(cloudFormationClient, never()).listStackResources(any(ListStackResourcesRequest.class));
    }

    private void stubStackUpdateWaiter() {
        when(cloudContext.getId()).thenReturn(1L);
        when(cloudFormationClient.waiters()).thenReturn(cloudFormationWaiter);
    }

    private CloudResource cloudFormationStackResource() {
        return CloudResource.builder()
                .withName(STACK_NAME)
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();
    }

    private StackResourceSummary launchTemplateResource(String launchTemplateId) {
        return StackResourceSummary.builder().resourceType("AWS::EC2::LaunchTemplate").physicalResourceId(launchTemplateId).build();
    }

    private StackResourceSummary autoScalingGroupResource(String autoScalingGroupName) {
        return StackResourceSummary.builder().resourceType("AWS::AutoScaling::AutoScalingGroup").physicalResourceId(autoScalingGroupName).build();
    }

    private Stack stackWithTags(Map<String, String> tags) {
        return Stack.builder().tags(toCloudFormationTags(tags)).build();
    }

    private Stack stackWithExistingTags() {
        return stackWithTags(EXISTING_TAGS);
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

    private List<software.amazon.awssdk.services.cloudwatch.model.Tag> toCloudWatchTags(Map<String, String> tags) {
        return tags.entrySet().stream()
                .map(e -> software.amazon.awssdk.services.cloudwatch.model.Tag.builder().key(e.getKey()).value(e.getValue()).build())
                .toList();
    }
}