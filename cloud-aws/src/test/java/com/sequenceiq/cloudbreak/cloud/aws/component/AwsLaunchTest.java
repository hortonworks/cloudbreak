package com.sequenceiq.cloudbreak.cloud.aws.component;

import static com.sequenceiq.cloudbreak.cloud.aws.TestConstants.LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH;
import static com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone.availabilityZone;
import static com.sequenceiq.cloudbreak.cloud.model.Location.location;
import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.cloudbreak.common.type.CloudConstants.AWS;
import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.LifecycleState;
import com.amazonaws.services.cloudformation.model.DescribeStackResourceResult;
import com.amazonaws.services.cloudformation.model.DescribeStacksResult;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import com.amazonaws.services.cloudformation.model.StackResourceDetail;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateVolumeResult;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.DescribeVolumesRequest;
import com.amazonaws.services.ec2.model.DescribeVolumesResult;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.VolumeState;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.AdjustmentType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupType;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationRetryClient;
import com.sequenceiq.cloudbreak.cloud.aws.encryption.EncryptedImageCopyService;
import com.sequenceiq.cloudbreak.cloud.aws.connector.resource.AwsResourceConnector;
import com.sequenceiq.cloudbreak.cloud.aws.task.AwsCreateStackStatusCheckerTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.PortDefinition;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.cloud.model.SecurityRule;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.util.FreeMarkerTemplateUtils;

@MockBeans({
        @MockBean(AwsCreateStackStatusCheckerTask.class),
//        @MockBean(Aws)
})
public class AwsLaunchTest extends AwsComponentTest {

    private static final String LOGIN_USER_NAME = "loginusername";

    private static final String PUBLIC_KEY = "pubkey";

    private static final int ROOT_VOLUME_SIZE = 50;

    private static final String CORE_CUSTOM_DATA = "CORE";

    private static final String GATEWAY_CUSTOM_DATA = "GATEWAY";

    private static final String CIDR = "10.10.10.10/16";

    private static final int INSTANCE_STATE_RUNNING = 16;

    private static final String AUTOSCALING_GROUP_NAME = "autoscalingGroupName";

    private static final String INSTANCE_ID = "instanceId";

    private static final String VOLUME_ID = "New Volume Id";

    private static final int SIZE_DISK_1 = 1;

    private static final int SIZE_DISK_2 = 2;

    private static int volumeIndex = 1;

    private static boolean describeVolumeRequestFirstInvocation = true;

    @Inject
    private AwsResourceConnector awsResourceConnector;

    @Inject
    private PersistenceNotifier persistenceNotifier;

    @Inject
    private AmazonCloudFormationRetryClient amazonCloudFormationRetryClient;

    @Inject
    private EncryptedImageCopyService encryptedImageCopyService;

    @Inject
    private freemarker.template.Configuration configuration;

    @Inject
    private FreeMarkerTemplateUtils freeMarkerTemplateUtils;

    @Inject
    private AmazonEC2Client amazonEC2Client;

    @Inject
    private AwsCreateStackStatusCheckerTask awsCreateStackStatusCheckerTask;

    @Inject
    private AmazonAutoScalingRetryClient amazonAutoScalingRetryClient;

    @Test
    public void launchStack() throws Exception {
        setupFreemarkerTemplateProcessing();
        setupDescribeStacksResponses();
        setupDescribeImagesResponse();
        setupCreateStackStatusCheckerTask();
        setupDescribeStackResourceResponse();
        setupAutoscalingResponses();
        setupDescribeInstanceStatusResponse();
        setupCreateVolumeResponse();
        setupDescribeVolumeResponse();
        setupDescribeSubnetResponse();

        awsResourceConnector.launch(getAuthenticatedContext(), getStack(), persistenceNotifier, AdjustmentType.EXACT, Long.MAX_VALUE);

        // assert
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VPC.equals(cloudResource.getType())), any());
        verify(persistenceNotifier, times(2)).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_VOLUMESET.equals(cloudResource.getType())), any());
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.AWS_SUBNET.equals(cloudResource.getType())), any());
        verify(persistenceNotifier).notifyAllocation(argThat(cloudResource -> ResourceType.CLOUDFORMATION_STACK.equals(cloudResource.getType())), any());
        // resourceNotification calls: vpc, subnet

        InOrder inOrder = inOrder(amazonEC2Client, amazonCloudFormationRetryClient, awsCreateStackStatusCheckerTask);
        inOrder.verify(amazonEC2Client).describeImages(any());
        inOrder.verify(amazonCloudFormationRetryClient).createStack(any());
        inOrder.verify(awsCreateStackStatusCheckerTask).call();
        inOrder.verify(amazonEC2Client, times(2)).createVolume(any());
        inOrder.verify(amazonEC2Client, times(2)).attachVolume(any());
        // aws calls
        // computeResource calls
        // - createVolume
        // - attachVolume
        // - describeVolume
    }

    void setupFreemarkerTemplateProcessing() throws IOException, freemarker.template.TemplateException {
        when(freeMarkerTemplateUtils.processTemplateIntoString(any(), any())).thenReturn("processedTemplate");
    }

    void setupCreateVolumeResponse() {
        when(amazonEC2Client.createVolume(any())).thenReturn(
                new CreateVolumeResult().withVolume(
                        new com.amazonaws.services.ec2.model.Volume().withVolumeId(VOLUME_ID + getNextVolumeId())
                )
        );
    }

    private static int getNextVolumeId() {
        return volumeIndex++;
    }

    void setupDescribeSubnetResponse() {
        when(amazonEC2Client.describeSubnets(any())).thenAnswer(
                (Answer) invocation -> {
                    DescribeSubnetsRequest request = (DescribeSubnetsRequest) invocation.getArgument(0);
                    String subnetId = request.getSubnetIds().get(0);
                    DescribeSubnetsResult result = new DescribeSubnetsResult()
                            .withSubnets(new com.amazonaws.services.ec2.model.Subnet()
                                    .withSubnetId(subnetId)
                                    .withAvailabilityZone("eu-west-1c"));
                    return result;
                }
        );
    }

    void setupDescribeVolumeResponse() {
        when(amazonEC2Client.describeVolumes(any())).thenAnswer(
                (Answer) invocation -> {
                    DescribeVolumesResult describeVolumesResult = new DescribeVolumesResult();
                    Object[] args = invocation.getArguments();
                    DescribeVolumesRequest describeVolumesRequest = (DescribeVolumesRequest) args[0];
                    VolumeState currentVolumeState = getCurrentVolumeState();
                    describeVolumesRequest.getVolumeIds().forEach(
                            volume -> describeVolumesResult.withVolumes(
                                    new com.amazonaws.services.ec2.model.Volume().withState(currentVolumeState)
                            )
                    );
                    return describeVolumesResult;
                }
        );
    }

    private VolumeState getCurrentVolumeState() {
        VolumeState currentVolumeState = describeVolumeRequestFirstInvocation ? VolumeState.Available : VolumeState.InUse;
        describeVolumeRequestFirstInvocation = false;
        return currentVolumeState;
    }

    void setupDescribeInstanceStatusResponse() {
        when(amazonEC2Client.describeInstanceStatus(any())).thenReturn(
                new DescribeInstanceStatusResult().withInstanceStatuses(
                        new com.amazonaws.services.ec2.model.InstanceStatus().withInstanceState(new InstanceState().withCode(INSTANCE_STATE_RUNNING)))
        );
    }

    void setupDescribeStacksResponses() {
        when(amazonCloudFormationRetryClient.describeStacks(any()))
                .thenThrow(new AmazonServiceException("stack does not exist"))
                .thenReturn(getDescribeStacksResult())
                .thenReturn(getDescribeStacksResult())
                .thenReturn(getDescribeStacksResult());
    }

    void setupDescribeImagesResponse() {
        when(amazonEC2Client.describeImages(any())).thenReturn(
                new DescribeImagesResult()
                        .withImages(new com.amazonaws.services.ec2.model.Image().withRootDeviceName(""))
        );
    }

    void setupCreateStackStatusCheckerTask() {
        // TODO would be nice to call real method instead of mocking it
        when(awsCreateStackStatusCheckerTask.completed(anyBoolean())).thenReturn(true);
        when(awsCreateStackStatusCheckerTask.call()).thenReturn(true);
    }

    void setupDescribeStackResourceResponse() {
        StackResourceDetail stackResourceDetail = new StackResourceDetail().withPhysicalResourceId(AUTOSCALING_GROUP_NAME);
        DescribeStackResourceResult describeStackResourceResult = new DescribeStackResourceResult().withStackResourceDetail(stackResourceDetail);
        when(amazonCloudFormationRetryClient.describeStackResource(any())).thenReturn(describeStackResourceResult);
    }

    void setupAutoscalingResponses() {
        DescribeScalingActivitiesResult describeScalingActivitiesResult = new DescribeScalingActivitiesResult();
        when(amazonAutoScalingRetryClient.describeScalingActivities(any())).thenReturn(describeScalingActivitiesResult);

        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult()
                .withAutoScalingGroups(
                        new AutoScalingGroup()
                                .withInstances(new Instance().withLifecycleState(LifecycleState.InService).withInstanceId(INSTANCE_ID))
                                .withAutoScalingGroupName(AUTOSCALING_GROUP_NAME)
                );
        when(amazonAutoScalingRetryClient.describeAutoScalingGroups(any())).thenReturn(describeAutoScalingGroupsResult);
    }

    private AuthenticatedContext getAuthenticatedContext() {
        Location location = location(region("region"), availabilityZone("availabilityZone"));
        CloudContext cloudContext = new CloudContext(1L, "cloudContextName", AWS, "variant", location, "owner@company.com", 5L);
        CloudCredential cloudCredential = new CloudCredential(3L, "credentialName");
        return new AuthenticatedContext(cloudContext, cloudCredential);
    }

    private CloudStack getStack() throws IOException {
        InstanceAuthentication instanceAuthentication = new InstanceAuthentication(PUBLIC_KEY, "pubkeyid", LOGIN_USER_NAME);

        CloudInstance instance = getCloudInstance(instanceAuthentication);
        Security security = getSecurity();

        List<Group> groups = List.of(new Group("group1", InstanceGroupType.CORE, List.of(instance), security, null,
                instanceAuthentication, instanceAuthentication.getLoginUserName(), instanceAuthentication.getPublicKey(), ROOT_VOLUME_SIZE));
        Network network = new Network(new Subnet(CIDR));

        Map<InstanceGroupType, String> userData = ImmutableMap.of(
                InstanceGroupType.CORE, CORE_CUSTOM_DATA,
                InstanceGroupType.GATEWAY, GATEWAY_CUSTOM_DATA
        );
        Image image = new Image("cb-centos66-amb200-2015-05-25", userData, "redhat6", "redhat6", "", "default", "default-id", new HashMap<>());

        String template = configuration.getTemplate(LATEST_AWS_CLOUD_FORMATION_TEMPLATE_PATH, "UTF-8").toString();
        return new CloudStack(groups, network, image, Map.of(), Map.of(), template, instanceAuthentication, LOGIN_USER_NAME, PUBLIC_KEY, null);
    }

    private Security getSecurity() {
        List<SecurityRule> rules = Collections.singletonList(new SecurityRule("0.0.0.0/0",
                new PortDefinition[]{new PortDefinition("22", "22"), new PortDefinition("443", "443")}, "tcp"));
        return new Security(rules, emptyList());
    }

    private CloudInstance getCloudInstance(InstanceAuthentication instanceAuthentication) {
        List<Volume> volumes = Arrays.asList(
                new Volume("/hadoop/fs1", "HDD", SIZE_DISK_1),
                new Volume("/hadoop/fs2", "HDD", SIZE_DISK_2)
        );
        InstanceTemplate instanceTemplate = new InstanceTemplate("m1.medium", "master", 0L, volumes, InstanceStatus.CREATE_REQUESTED,
                new HashMap<>(), 0L, "cb-centos66-amb200-2015-05-25");
        Map<String, Object> params = new HashMap<>();
        return new CloudInstance(null, instanceTemplate, instanceAuthentication, params);
    }

    private DescribeStacksResult getDescribeStacksResult() {
        return new DescribeStacksResult().withStacks(
                new Stack().withOutputs(
                        new Output().withOutputKey("CreatedVpc").withOutputValue("vpc-id"),
                        new Output().withOutputKey("CreatedSubnet").withOutputValue("subnet-id"),
                        new Output().withOutputKey("EIPAllocationIDmaster1").withOutputValue("eipalloc-id")
                ));
    }
}
