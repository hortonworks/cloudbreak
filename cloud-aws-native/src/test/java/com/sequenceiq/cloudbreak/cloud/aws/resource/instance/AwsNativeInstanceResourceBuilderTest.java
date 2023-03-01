package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsStackNameCommonUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.ec2.model.BlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.Reservation;
import software.amazon.awssdk.services.ec2.model.RunInstancesRequest;
import software.amazon.awssdk.services.ec2.model.RunInstancesResponse;
import software.amazon.awssdk.services.ec2.model.TagSpecification;

@ExtendWith(MockitoExtension.class)
class AwsNativeInstanceResourceBuilderTest {

    @InjectMocks
    private AwsNativeInstanceResourceBuilder underTest;

    @Mock
    private AwsContext awsContext;

    @Mock
    private AwsMethodExecutor awsMethodExecutor;

    @Mock
    private AwsTaggingService awsTaggingService;

    @Mock
    private SecurityGroupBuilderUtil securityGroupBuilderUtil;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private Network network;

    @Mock
    private Security security;

    @Mock
    private AmazonEc2Client amazonEc2Client;

    @Mock
    private Group group;

    @Mock
    private CloudInstance cloudInstance;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private Supplier<Object> supplier;

    @Mock
    private VolumeBuilderUtil volumeBuilderUtil;

    @Mock
    private InstanceTemplate instanceTemplate;

    @Mock
    private AwsStackNameCommonUtil awsStackNameCommonUtil;

    @Test
    void testBuildWhenBuildableResorucesAreEmpty() {
        long privateId = 0;
        CloudConnectorException actual = Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.emptyList(), cloudStack));
        Assertions.assertEquals("Buildable resources cannot be empty!", actual.getMessage());
    }

    @Test
    void testBuildWhenInstanceNoExist() throws Exception {
        Instance instance = Instance.builder().instanceId("instanceId").build();
        RunInstancesResponse runInstancesResponse = RunInstancesResponse.builder().instances(instance).build();
        InstanceAuthentication authentication = mock(InstanceAuthentication.class);
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("groupName")
                .withParameters(emptyMap())
                .build();

        Image image = mock(Image.class);

        long privateId = 0;
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.empty());
        when(awsTaggingService.prepareEc2TagSpecification(cloudStack.getTags(), software.amazon.awssdk.services.ec2.model.ResourceType.INSTANCE))
                .thenReturn(TagSpecification.builder().build());
        when(amazonEc2Client.createInstance(any())).thenReturn(runInstancesResponse);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(group.getName()).thenReturn("groupName");
        when(cloudStack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn("img-name");
        when(cloudStack.getInstanceAuthentication()).thenReturn(authentication);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(securityGroupBuilderUtil.getSecurityGroupIds(awsContext, group)).thenReturn(List.of("sg-id"));
        when(awsStackNameCommonUtil.getInstanceName(ac, "groupName", privateId)).thenReturn("stackname");

        ArgumentCaptor<RunInstancesRequest> runInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(RunInstancesRequest.class);
        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        verify(amazonEc2Client).createInstance(runInstancesRequestArgumentCaptor.capture());
        RunInstancesRequest runInstancesRequest = runInstancesRequestArgumentCaptor.getValue();
        Assertions.assertEquals(actual.get(0).getInstanceId(), "instanceId");
        Assertions.assertEquals("sg-id", runInstancesRequest.securityGroupIds().get(0));
    }

    @Test
    void testBuildWhenInstanceExistAndRunning() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();
        long privateId = 0;
        Instance instance = Instance.builder()
                .instanceId("instanceId")
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.of(instance));

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        Assertions.assertEquals(actual.get(0).getInstanceId(), "instanceId");
        verify(amazonEc2Client, times(0)).startInstances(any());
    }

    @Test
    void testBuildWhenInstanceExistAndNotRunningButNotTerminated() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();
        long privateId = 0;
        Instance instance = Instance.builder()
                .instanceId("instanceId")
                .state(InstanceState.builder().code(0).build())
                .build();
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.of(instance));
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        Assertions.assertEquals(actual.get(0).getInstanceId(), "instanceId");
        verify(amazonEc2Client, times(1)).startInstances(any());
    }

    @Test
    void testBuildWhenInstanceExistButTerminated() throws Exception {
        Instance instance = Instance.builder().instanceId("instanceId").build();
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withGroup("groupName")
                .withParameters(emptyMap())
                .build();
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        InstanceAuthentication authentication = mock(InstanceAuthentication.class);
        Instance terminatedInstance = Instance.builder()
                .instanceId("terminatedInstanceId")
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE).build())
                .build();
        RunInstancesResponse runInstancesResponse = RunInstancesResponse.builder().instances(instance).build();

        Image image = mock(Image.class);

        long privateId = 0;
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.of(terminatedInstance));
        when(awsTaggingService.prepareEc2TagSpecification(cloudStack.getTags(), software.amazon.awssdk.services.ec2.model.ResourceType.INSTANCE))
                .thenReturn(TagSpecification.builder().build());
        when(amazonEc2Client.createInstance(any())).thenReturn(runInstancesResponse);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(cloudStack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn("img-name");
        when(cloudStack.getInstanceAuthentication()).thenReturn(authentication);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(group.getName()).thenReturn("groupName");
        when(awsStackNameCommonUtil.getInstanceName(ac, "groupName", privateId)).thenReturn("stackname");

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        Assertions.assertEquals(actual.get(0).getInstanceId(), "instanceId");
    }

    @Test
    void testIsFinishedWhenCreationAllMatchRunning() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance1 = Instance.builder()
                .instanceId("instanceId1")
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        Instance instance2 = Instance.builder()
                .instanceId("instanceId1")
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance1, instance2).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertTrue(actual);
    }

    @Test
    void testIsFinishedWhenCreationOneRunningAndOneOther() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance1 = Instance.builder()
                .instanceId("instanceId1")
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE).build())
                .build();
        Instance instance2 = Instance.builder()
                .instanceId("instanceId1")
                .state(InstanceState.builder().code(0).build())
                .build();
        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance1, instance2).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertFalse(actual);
    }

    @Test
    void testIsFinishedWhenCreationAndNotFoundException() {
        CloudResource cloudResource = createInstanceResource();
        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("message")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NotFound").build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        Ec2Exception actual = Assertions.assertThrows(Ec2Exception.class, () -> underTest.isFinished(awsContext, ac, cloudResource));
        Assertions.assertEquals("NotFound", actual.awsErrorDetails().errorCode());
    }

    @Test
    void testIsFinishedWhenTerminationAllMatchTerminated() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance1 = Instance.builder()
                .instanceId("instanceId1")
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE).build())
                .build();
        Instance instance2 = Instance.builder()
                .instanceId("instanceId1")
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE).build())
                .build();

        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance1, instance2).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertTrue(actual);
    }

    @Test
    void testIsFinishedWhenTerminationAndOneTerminatedAndOneOther() {
        CloudResource cloudResource = createInstanceResource();
        Instance instance1 = Instance.builder()
                .instanceId("instanceId1")
                .state(InstanceState.builder().code(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE).build())
                .build();
        Instance instance2 = Instance.builder()
                .instanceId("instanceId1")
                .state(InstanceState.builder().code(0).build())
                .build();

        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResponse describeInstanceResponse = DescribeInstancesResponse.builder()
                .reservations(Reservation.builder().instances(instance1, instance2).build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResponse);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertFalse(actual);
    }

    @Test
    void testIsFinishedWhenTerminationAndNotFoundException() {
        CloudResource cloudResource = createInstanceResource();
        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("message")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("NotFound").build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertTrue(actual);
    }

    @Test
    void testIsFinishedWhenTerminationAndOtherException() {
        CloudResource cloudResource = createInstanceResource();
        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        Ec2Exception amazonEC2Exception = (Ec2Exception) Ec2Exception.builder()
                .message("message")
                .awsErrorDetails(AwsErrorDetails.builder().errorCode("AnyOther").build())
                .build();
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        Ec2Exception actual = Assertions.assertThrows(Ec2Exception.class, () -> underTest.isFinished(awsContext, ac, cloudResource));
        Assertions.assertEquals("AnyOther", actual.awsErrorDetails().errorCode());
    }

    @Test
    void testIsFinishedWhenTheInstanceIdIsEmptyOnTheResourceShouldReturnAsFinished() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .build();

        when(awsContext.isBuild()).thenReturn(false);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        verify(amazonEc2Client, times(0)).describeInstances(any());
        Assertions.assertTrue(actual);
    }

    @Test
    void testBlocksWhenEphemeralNull() {
        BlockDeviceMapping root = BlockDeviceMapping.builder().build();
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(volumeBuilderUtil.getEphemeral(any())).thenReturn(null);
        when(volumeBuilderUtil.getRootVolume(any(AwsInstanceView.class), eq(group), eq(cloudStack), eq(ac))).thenReturn(root);
        Collection<BlockDeviceMapping> actual = underTest.blocks(group, cloudStack, ac);
        Assertions.assertEquals(1, actual.size());
    }

    @Test
    void testBlocksWhenEphemeralBlockDeviceMappingsListIsEmpty() {
        BlockDeviceMapping root = BlockDeviceMapping.builder().build();
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(volumeBuilderUtil.getEphemeral(any())).thenReturn(new ArrayList<>());
        when(volumeBuilderUtil.getRootVolume(any(AwsInstanceView.class), eq(group), eq(cloudStack), eq(ac))).thenReturn(root);
        Collection<BlockDeviceMapping> actual = underTest.blocks(group, cloudStack, ac);
        Assertions.assertEquals(1, actual.size());
    }

    @Test
    void testBlocksWhenEphemeralBlockDeviceMappingsListIsNotEmpty() {
        BlockDeviceMapping root = BlockDeviceMapping.builder().build();
        BlockDeviceMapping ephemeralBlockDevice1 = BlockDeviceMapping.builder()
                .deviceName("/dev/xvdb")
                .virtualName("ephemeral0")
                .build();
        BlockDeviceMapping ephemeralBlockDevice2 = BlockDeviceMapping.builder()
                .deviceName("/dev/xvdc")
                .virtualName("ephemeral1")
                .build();
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(volumeBuilderUtil.getEphemeral(any())).thenReturn(List.of(ephemeralBlockDevice1, ephemeralBlockDevice2));
        when(volumeBuilderUtil.getRootVolume(any(AwsInstanceView.class), eq(group), eq(cloudStack), eq(ac))).thenReturn(root);
        Collection<BlockDeviceMapping> actual = underTest.blocks(group, cloudStack, ac);
        Assertions.assertEquals(3, actual.size());
    }

    private CloudResource createInstanceResource() {
        return CloudResource.builder()
                .withName("name")
                .withType(ResourceType.AWS_INSTANCE)
                .withStatus(CommonStatus.CREATED)
                .withParameters(emptyMap())
                .withInstanceId("i-aninstanceid")
                .build();
    }
}
