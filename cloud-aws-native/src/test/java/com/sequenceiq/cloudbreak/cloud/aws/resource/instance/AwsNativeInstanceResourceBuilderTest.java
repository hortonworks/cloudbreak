package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.sequenceiq.cloudbreak.cloud.aws.common.util.AwsMethodExecutor;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsTaggingService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.VolumeBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsInstanceView;
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

@ExtendWith(MockitoExtension.class)
public class AwsNativeInstanceResourceBuilderTest {

    @InjectMocks
    private AwsNativeInstanceResourceBuilder underTest;

    @Mock
    private AwsContext awsContext;

    @Mock
    private AwsMethodExecutor awsMethodExecutor;

    @Mock
    private AwsTaggingService awsTaggingService;

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

    @Test
    public void testBuildWhenBuildableResorucesAreEmpty() throws Exception {
        long privateId = 0;
        CloudConnectorException actual = Assertions.assertThrows(CloudConnectorException.class,
                () -> underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.emptyList(), cloudStack));
        Assertions.assertEquals("Buildable resources cannot be empty!", actual.getMessage());
    }

    @Test
    public void testBuildWhenInstanceNoExist() throws Exception {
        RunInstancesResult runInstancesResult = mock(RunInstancesResult.class);
        InstanceAuthentication authentication = mock(InstanceAuthentication.class);
        Instance instance = new Instance().withInstanceId("instanceId");
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .group("groupName")
                .params(emptyMap())
                .build();

        CloudResource securityGroupCloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_SECURITY_GROUP)
                .status(CommonStatus.CREATED)
                .reference("sg-id")
                .group("groupName")
                .params(emptyMap())
                .build();

        Image image = mock(Image.class);

        long privateId = 0;
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.empty());
        when(awsTaggingService.prepareEc2TagSpecification(cloudStack.getTags(), com.amazonaws.services.ec2.model.ResourceType.Instance))
                .thenReturn(new TagSpecification());
        when(amazonEc2Client.createInstance(any())).thenReturn(runInstancesResult);
        when(runInstancesResult.getReservation()).thenReturn(new Reservation().withInstances(instance));
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(group.getName()).thenReturn("groupName");
        when(cloudStack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn("img-name");
        when(cloudStack.getInstanceAuthentication()).thenReturn(authentication);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(awsContext.getGroupResources("groupName")).thenReturn(List.of(securityGroupCloudResource));

        ArgumentCaptor<RunInstancesRequest> runInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(RunInstancesRequest.class);
        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        verify(amazonEc2Client).createInstance(runInstancesRequestArgumentCaptor.capture());
        RunInstancesRequest runInstancesRequest = runInstancesRequestArgumentCaptor.getValue();
        Assertions.assertEquals(actual.get(0).getInstanceId(), "instanceId");
        Assertions.assertEquals("sg-id", runInstancesRequest.getSecurityGroupIds().get(0));
    }

    @Test
    public void testBuildWhenInstanceExistAndRunning() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();
        long privateId = 0;
        Instance instance = new Instance().withInstanceId("instanceId")
                .withState(new InstanceState().withCode(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE));
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.of(instance));

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        Assertions.assertEquals(actual.get(0).getInstanceId(), "instanceId");
        verify(amazonEc2Client, times(0)).startInstances(any());
    }

    @Test
    public void testBuildWhenInstanceExistAndNotRunningButNotTerminated() throws Exception {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();
        long privateId = 0;
        Instance instance = new Instance().withInstanceId("instanceId")
                .withState(new InstanceState().withCode(0));
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.of(instance));
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        Assertions.assertEquals(actual.get(0).getInstanceId(), "instanceId");
        verify(amazonEc2Client, times(1)).startInstances(any());
    }

    @Test
    public void testBuildWhenInstanceExistButTerminated() throws Exception {
        RunInstancesResult runInstancesResult = mock(RunInstancesResult.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        InstanceAuthentication authentication = mock(InstanceAuthentication.class);
        Instance terminatedInstance = new Instance().withInstanceId("terminatedInstanceId")
                .withState(new InstanceState().withCode(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE));
        Instance instance = new Instance().withInstanceId("instanceId");
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .group("groupName")
                .params(emptyMap())
                .build();

        CloudResource securityGroupCloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_SECURITY_GROUP)
                .status(CommonStatus.CREATED)
                .reference("sg-id")
                .group("groupName")
                .params(emptyMap())
                .build();

        Image image = mock(Image.class);

        long privateId = 0;
        when(awsMethodExecutor.execute(any(), eq(Optional.empty()))).thenReturn(Optional.of(terminatedInstance));
        when(awsTaggingService.prepareEc2TagSpecification(cloudStack.getTags(), com.amazonaws.services.ec2.model.ResourceType.Instance))
                .thenReturn(new TagSpecification());
        when(amazonEc2Client.createInstance(any())).thenReturn(runInstancesResult);
        when(runInstancesResult.getReservation()).thenReturn(new Reservation().withInstances(instance));
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(cloudStack.getImage()).thenReturn(image);
        when(image.getImageName()).thenReturn("img-name");
        when(cloudStack.getInstanceAuthentication()).thenReturn(authentication);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        when(group.getName()).thenReturn("groupName");
        when(awsContext.getGroupResources("groupName")).thenReturn(List.of(securityGroupCloudResource));

        List<CloudResource> actual = underTest.build(awsContext, cloudInstance, privateId, ac, group, Collections.singletonList(cloudResource), cloudStack);
        Assertions.assertEquals(actual.get(0).getInstanceId(), "instanceId");
    }

    @Test
    public void testIsFinishedWhenCreationAllMatchRunning() {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();
        Instance instance1 = new Instance().withInstanceId("instanceId1")
                .withState(new InstanceState().withCode(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE));
        Instance instance2 = new Instance().withInstanceId("instanceId2")
                .withState(new InstanceState().withCode(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE));

        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResult describeInstanceResult = new DescribeInstancesResult().withReservations(new Reservation().withInstances(instance1, instance2));
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResult);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertTrue(actual);
    }

    @Test
    public void testIsFinishedWhenCreationOneRunningAndOneOther() {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();
        Instance instance1 = new Instance().withInstanceId("instanceId1")
                .withState(new InstanceState().withCode(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_RUNNING_CODE));
        Instance instance2 = new Instance().withInstanceId("instanceId2")
                .withState(new InstanceState().withCode(0));

        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResult describeInstanceResult = new DescribeInstancesResult().withReservations(new Reservation().withInstances(instance1, instance2));
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResult);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertFalse(actual);
    }

    @Test
    public void testIsFinishedWhenCreationAndNotFoundException() {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();

        when(awsContext.isBuild()).thenReturn(true);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("message");
        amazonEC2Exception.setErrorCode("NotFound");
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        AmazonEC2Exception actual = Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.isFinished(awsContext, ac, cloudResource));
        Assertions.assertEquals("NotFound", actual.getErrorCode());
    }

    @Test
    public void testIsFinishedWhenTerminationAllMatchTerminated() {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();
        Instance instance1 = new Instance().withInstanceId("instanceId1")
                .withState(new InstanceState().withCode(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE));
        Instance instance2 = new Instance().withInstanceId("instanceId2")
                .withState(new InstanceState().withCode(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE));

        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResult describeInstanceResult = new DescribeInstancesResult().withReservations(new Reservation().withInstances(instance1, instance2));
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResult);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertTrue(actual);
    }

    @Test
    public void testIsFinishedWhenTerminationAndOneTerminatedAndOneOther() {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();
        Instance instance1 = new Instance().withInstanceId("instanceId1")
                .withState(new InstanceState().withCode(AwsNativeInstanceResourceBuilder.AWS_INSTANCE_TERMINATED_CODE));
        Instance instance2 = new Instance().withInstanceId("instanceId2")
                .withState(new InstanceState().withCode(0));

        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        DescribeInstancesResult describeInstanceResult = new DescribeInstancesResult().withReservations(new Reservation().withInstances(instance1, instance2));
        when(amazonEc2Client.describeInstances(any())).thenReturn(describeInstanceResult);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertFalse(actual);
    }

    @Test
    public void testIsFinishedWhenTerminationAndNotFoundException() {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();

        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("message");
        amazonEC2Exception.setErrorCode("NotFound");
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        boolean actual = underTest.isFinished(awsContext, ac, cloudResource);
        Assertions.assertTrue(actual);
    }

    @Test
    public void testIsFinishedWhenTerminationAndOtherException() {
        CloudResource cloudResource = CloudResource.builder()
                .name("name")
                .type(ResourceType.AWS_INSTANCE)
                .status(CommonStatus.CREATED)
                .params(emptyMap())
                .build();

        when(awsContext.isBuild()).thenReturn(false);
        when(awsContext.getAmazonEc2Client()).thenReturn(amazonEc2Client);
        AmazonEC2Exception amazonEC2Exception = new AmazonEC2Exception("message");
        amazonEC2Exception.setErrorCode("AnyOther");
        when(amazonEc2Client.describeInstances(any())).thenThrow(amazonEC2Exception);
        AmazonEC2Exception actual = Assertions.assertThrows(AmazonEC2Exception.class, () -> underTest.isFinished(awsContext, ac, cloudResource));
        Assertions.assertEquals("AnyOther", actual.getErrorCode());
    }

    @Test
    public void testBlocksWhenEphemeralNull() {
        BlockDeviceMapping root = new BlockDeviceMapping();
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(volumeBuilderUtil.getEphemeral(any())).thenReturn(null);
        when(volumeBuilderUtil.getRootVolume(any(AwsInstanceView.class), eq(group), eq(cloudStack), eq(ac))).thenReturn(root);
        Collection<BlockDeviceMapping> actual = underTest.blocks(group, cloudStack, ac);
        Assertions.assertEquals(1, actual.size());
    }
}
