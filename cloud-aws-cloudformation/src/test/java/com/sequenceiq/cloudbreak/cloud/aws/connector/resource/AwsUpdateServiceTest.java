package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.AutoScalingGroupHandler;
import com.sequenceiq.cloudbreak.cloud.aws.AwsCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.AwsImageUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchConfigurationUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchTemplateUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationStackUtil;
import com.sequenceiq.cloudbreak.cloud.aws.LaunchTemplateField;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonCloudFormationClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.InstanceAuthentication;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.model.catalog.ImagePackageVersion;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.ec2.model.HttpTokensState;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateBlockDeviceMapping;
import software.amazon.awssdk.services.ec2.model.LaunchTemplateEbsBlockDevice;
import software.amazon.awssdk.services.ec2.model.VolumeType;

@ExtendWith(MockitoExtension.class)
class AwsUpdateServiceTest {

    @Mock
    private AwsImageUpdateService awsImageUpdateService;

    @Mock
    private CloudStack stack;

    @Mock
    private AuthenticatedContext ac;

    @Mock
    private AwsLaunchTemplateUpdateService awsLaunchTemplateUpdateService;

    @Mock
    private AwsLaunchConfigurationUpdateService launchConfigurationUpdateService;

    @Mock
    private AutoScalingGroupHandler autoScalingGroupHandler;

    @Mock
    private AwsCloudFormationClient awsClient;

    @Mock
    private CloudFormationStackUtil cfStackUtil;

    @InjectMocks
    private AwsUpdateService underTest;

    @Test
    void updateCloudFormationTemplateResourceWithImageParameter() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, Collections.singletonList(cloudResource),
                UpdateType.IMAGE_UPDATE, Optional.empty());

        verify(awsImageUpdateService, times(1)).updateImage(ac, stack, cloudResource);
        assertEquals(1, statuses.size());
        assertEquals(ResourceStatus.UPDATED, statuses.get(0).getStatus());
        assertEquals(cloudResource, statuses.get(0).getCloudResource());
    }

    @Test
    void updateCloudFormationTemplateResourceWithoutImageParameter() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .build();
        CloudResource launch = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .build();

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, List.of(cloudResource, launch),
                UpdateType.IMAGE_UPDATE, Optional.empty());

        verify(awsImageUpdateService, times(0)).updateImage(ac, stack, cloudResource);
        assertEquals(0, statuses.size());
    }

    @Test
    void updateRandomResource() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();
        CloudResource cf = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, List.of(cloudResource, cf),
                UpdateType.IMAGE_UPDATE, Optional.empty());

        verify(awsImageUpdateService, times(0)).updateImage(ac, stack, cloudResource);
        assertEquals(1, statuses.size());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    void testUpdateUserDataWhenUserdataIsNullOrEmptyForGroup(String userdata) {
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");
        Group gatewayGroup = mock(Group.class);
        when(gatewayGroup.getType()).thenReturn(InstanceGroupType.GATEWAY);
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        Map<InstanceGroupType, String> userDataMap = new HashMap<>();
        userDataMap.put(InstanceGroupType.GATEWAY, userdata);

        CloudResource cf = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();

        assertThrows(IllegalStateException.class, () -> underTest.updateUserData(ac, stack, List.of(cf), userDataMap));
    }

    @Test
    void updateUserData() {
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");
        Group coreGroup = mock(Group.class);
        when(coreGroup.getType()).thenReturn(InstanceGroupType.CORE);
        Group gatewayGroup = mock(Group.class);
        when(gatewayGroup.getType()).thenReturn(InstanceGroupType.GATEWAY);
        when(stack.getGroups()).thenReturn(List.of(coreGroup, gatewayGroup));

        Map<InstanceGroupType, String> userData = Map.of(
                InstanceGroupType.CORE, "core",
                InstanceGroupType.GATEWAY, "gateway"
        );

        CloudResource cf = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();

        underTest.updateUserData(ac, stack, List.of(cf), userData);

        String encodedCoreUserData = Base64Util.encode(userData.get(InstanceGroupType.CORE));
        Map<LaunchTemplateField, String> coreFields = Map.of(LaunchTemplateField.USER_DATA, encodedCoreUserData);
        verify(awsLaunchTemplateUpdateService).updateLaunchTemplate(coreFields, ac, cf.getName(), coreGroup, stack, Boolean.FALSE);
        String encodedGatewayUserData = Base64Util.encode(userData.get(InstanceGroupType.GATEWAY));
        Map<LaunchTemplateField, String> gatewayFields = Map.of(LaunchTemplateField.USER_DATA, encodedGatewayUserData);
        verify(awsLaunchTemplateUpdateService).updateLaunchTemplate(gatewayFields, ac, cf.getName(), gatewayGroup, stack, Boolean.FALSE);
    }

    @Test
    void verticalScaleLaunchTemplate() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();
        Group gatewayGroup = mock(Group.class);
        when(gatewayGroup.getName()).thenReturn("gwGroup");
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getFlavor()).thenReturn("m42.xxxl");
        when(gatewayGroup.getRootVolumeSize()).thenReturn(100);
        when(gatewayGroup.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");


        underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.VERTICAL_SCALE, Optional.of("gwGroup"));

        Map<LaunchTemplateField, String> updatableFields = Map.of(
                LaunchTemplateField.INSTANCE_TYPE,
                "m42.xxxl",
                LaunchTemplateField.ROOT_DISK_SIZE,
                "100");
        verify(awsLaunchTemplateUpdateService, times(1)).updateLaunchTemplate(eq(updatableFields), any(), eq("cf"), eq(gatewayGroup),
                eq(stack), eq(Boolean.TRUE));
    }

    @Test
    void instanceMetadataUpdateLaunchTemplate() {
        CloudResource cloudResource = CloudResource.builder().withName("cf").withType(ResourceType.CLOUDFORMATION_STACK).build();
        Group gatewayGroup = mock(Group.class);
        when(gatewayGroup.getName()).thenReturn("gwGroup");
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");
        Image image = mock(Image.class);
        when(image.getImageId()).thenReturn("image");
        when(image.getPackageVersions()).thenReturn(Map.of(ImagePackageVersion.IMDS_VERSION.getKey(), "v2"));
        when(stack.getImage()).thenReturn(image);

        underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, Optional.empty());

        Map<LaunchTemplateField, String> updatableFields = Map.of(LaunchTemplateField.HTTP_METADATA_OPTIONS, HttpTokensState.REQUIRED.toString());
        verify(awsLaunchTemplateUpdateService, times(1)).updateLaunchTemplate(eq(updatableFields), any(), eq("cf"), any(),
                eq(stack), eq(Boolean.TRUE));
    }

    @Test
    void instanceMetadataUpdateLaunchConfig() {
        CloudResource cloudResource = CloudResource.builder().withName("cf").withType(ResourceType.CLOUDFORMATION_STACK).build();
        Group gatewayGroup = mock(Group.class);
        when(gatewayGroup.getName()).thenReturn("gwGroup");
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchConfiguration");
        Image image = mock(Image.class);
        when(image.getImageId()).thenReturn("image");
        when(image.getPackageVersions()).thenReturn(Map.of(ImagePackageVersion.IMDS_VERSION.getKey(), "v2"));
        when(stack.getImage()).thenReturn(image);

        underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.INSTANCE_METADATA_UPDATE_TOKEN_REQUIRED, Optional.empty());

        Map<LaunchTemplateField, String> updatableFields = Map.of(LaunchTemplateField.HTTP_METADATA_OPTIONS, HttpTokensState.REQUIRED.toString());
        verify(launchConfigurationUpdateService, times(1)).updateLaunchConfigurations(any(), any(), any(), eq(updatableFields),
                any(), eq(Boolean.TRUE));
    }

    @Test
    void verticalScaleWithoutInstancesLaunchTemplate() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();
        Group gatewayGroup = mock(Group.class);
        when(gatewayGroup.getName()).thenReturn("gwGroup");
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getFlavor()).thenReturn("m42.xxxl");
        when(gatewayGroup.getRootVolumeSize()).thenReturn(100);
        when(gatewayGroup.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");


        underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.VERTICAL_SCALE_WITHOUT_INSTANCES, Optional.of("gwGroup"));

        Map<LaunchTemplateField, String> updatableFields = Map.of(
                LaunchTemplateField.INSTANCE_TYPE,
                "m42.xxxl",
                LaunchTemplateField.ROOT_DISK_SIZE,
                "100");
        verify(awsLaunchTemplateUpdateService, times(1)).updateLaunchTemplate(eq(updatableFields), any(), eq("cf"), eq(gatewayGroup),
                eq(stack), eq(Boolean.FALSE));
    }

    @Test
    void verticalScaleLaunchTemplateWhenGroupNotPresentedShouldNotCallModificationService() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();
        Group gatewayGroup = mock(Group.class);
        when(gatewayGroup.getName()).thenReturn("gwGroup");
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");


        underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.VERTICAL_SCALE, Optional.of("gwGroup1"));

        Map<LaunchTemplateField, String> updatableFields = Map.of(
                LaunchTemplateField.INSTANCE_TYPE,
                "m42.xxxl",
                LaunchTemplateField.ROOT_DISK_SIZE,
                "100");
        verify(awsLaunchTemplateUpdateService, times(0)).updateLaunchTemplate(eq(updatableFields), any(), eq("cf"), eq(gatewayGroup),
                eq(stack), eq(Boolean.TRUE));
        verify(launchConfigurationUpdateService, times(0)).updateLaunchConfigurations(any(), any(), any(), any());
    }

    @Test
    void verticalScaleLaunchConfig() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParameters(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();
        Group gatewayGroup = mock(Group.class);
        when(gatewayGroup.getName()).thenReturn("gwGroup");
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getFlavor()).thenReturn("m42.xxxl");
        when(gatewayGroup.getRootVolumeSize()).thenReturn(100);
        when(gatewayGroup.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchConfiguration");


        underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.VERTICAL_SCALE, Optional.of("gwGroup"));

        Map<LaunchTemplateField, String> updatableFields = Map.of(
                LaunchTemplateField.INSTANCE_TYPE,
                "m42.xxxl",
                LaunchTemplateField.ROOT_DISK_SIZE,
                "100");
        verify(launchConfigurationUpdateService, times(1))
                .updateLaunchConfigurations(any(), eq(stack), eq(cloudResource), eq(updatableFields), eq(gatewayGroup), eq(Boolean.TRUE));
    }

    @Test
    void updateLaunchTemplateForGroup() {
        AutoScalingGroup asg1 = newAutoScalingGroup("masterASG", List.of("i-master1", "i-master2"));
        AutoScalingGroup asg2 = newAutoScalingGroup("workerASG", List.of("i-worker1", "i-worker2", "i-worker3"));
        AmazonAutoScalingClient amazonAutoScalingClient = mock(AmazonAutoScalingClient.class);
        AmazonCloudFormationClient amazonCloudFormationClient = mock(AmazonCloudFormationClient.class);
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);

        Map<String, AutoScalingGroup> autoScalingGroupMap = Map.of("masterASG", asg1, "workerASG", asg2);

        when(awsClient.createAutoScalingClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonAutoScalingClient);
        when(awsClient.createCloudFormationClient(any(AwsCredentialView.class), anyString())).thenReturn(amazonCloudFormationClient);
        when(awsClient.createEc2Client(any(), any())).thenReturn(ec2Client);

        CloudContext cloudContext = CloudContext.Builder.builder()
                .withId(1L)
                .withName("teststack")
                .withCrn("crn")
                .withPlatform("AWS")
                .withVariant("AWS")
                .withLocation(Location.location(Region.region("eu-west-1"), AvailabilityZone.availabilityZone("eu-west-1a")))
                .withAccountId("1")
                .build();
        AuthenticatedContext authenticatedContext = new AuthenticatedContext(cloudContext, new CloudCredential());

        List<CloudResource> allInstances = new ArrayList<>();
        allInstances.add(newInstanceResource("worker1", "worker", "i-worker1"));
        allInstances.add(newInstanceResource("worker2", "worker", "i-worker2"));
        allInstances.add(newInstanceResource("worker3", "worker", "i-worker3"));
        CloudResource workerInstance4 = newInstanceResource("worker4", "worker", "i-worker4");
        allInstances.add(workerInstance4);
        CloudResource workerInstance5 = newInstanceResource("worker5", "worker", "i-worker5");
        allInstances.add(workerInstance5);

        InstanceAuthentication instanceAuthentication = new InstanceAuthentication("sshkey", "", "cloudbreak");
        List<Group> groups = new ArrayList<>();

        Group masterGroup = getMasterGroup(instanceAuthentication);
        masterGroup.setRootVolumeType(null);
        masterGroup.setRootVolumeSize(20);
        groups.add(masterGroup);

        Group worker = getWorkerGroup(instanceAuthentication);
        groups.add(worker);

        Map<String, String> tags = new HashMap<>();
        tags.put("owner", "cbuser");
        tags.put("created", "yesterday");
        CloudStack cloudStack = CloudStack.builder()
                .groups(groups)
                .network(getNetwork())
                .tags(tags)
                .instanceAuthentication(instanceAuthentication)
                .build();

        when(autoScalingGroupHandler.autoScalingGroupByName(any(), any(), any())).thenReturn(autoScalingGroupMap);
        LaunchTemplateBlockDeviceMapping masterBlockDeviceMapping = LaunchTemplateBlockDeviceMapping.builder().ebs(LaunchTemplateEbsBlockDevice.builder()
                .volumeSize(20).volumeType(VolumeType.GP2).build()).build();
        when(awsLaunchTemplateUpdateService.getBlockDeviceMappingFromAutoScalingGroup(authenticatedContext, asg2))
                .thenReturn(List.of(masterBlockDeviceMapping));
        when(awsLaunchTemplateUpdateService.getBlockDeviceMappingFromAutoScalingGroup(authenticatedContext, asg1))
                .thenReturn(List.of(masterBlockDeviceMapping));
        Map<LaunchTemplateField, String> updatableFields = Map.of(LaunchTemplateField.ROOT_DISK_SIZE, "50",
                LaunchTemplateField.ROOT_VOLUME_TYPE, "gp3");

        List<CloudResourceStatus> statuses = underTest.update(authenticatedContext, cloudStack, List.of(),
                UpdateType.PROVIDER_TEMPLATE_UPDATE, Optional.empty());

        verify(awsLaunchTemplateUpdateService).updateLaunchTemplate(updatableFields, false, amazonAutoScalingClient, ec2Client, asg2, cloudStack);
    }

    private AutoScalingGroup newAutoScalingGroup(String groupName, List<String> instances) {
        return AutoScalingGroup.builder()
                .autoScalingGroupName(groupName)
                .instances(instances.stream().map(instance -> Instance.builder().instanceId(instance).build()).collect(Collectors.toList()))
                .build();
    }

    private CloudResource newInstanceResource(String name, String group, String instanceId) {
        return CloudResource.builder().withType(ResourceType.AWS_INSTANCE).withStatus(CommonStatus.CREATED)
                .withName(name).withGroup(group).withInstanceId(instanceId).build();
    }

    private Group getMasterGroup(InstanceAuthentication instanceAuthentication) {
        List<CloudInstance> masterInstances = new ArrayList<>();
        CloudInstance masterInstance1 = new CloudInstance("i-master1", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        CloudInstance masterInstance2 = new CloudInstance("i-master2", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        masterInstances.add(masterInstance1);
        masterInstances.add(masterInstance2);
        return Group.builder()
                .withName("master")
                .withType(InstanceGroupType.GATEWAY)
                .withInstances(masterInstances)
                .withInstanceAuthentication(instanceAuthentication)
                .withLoginUserName(instanceAuthentication.getLoginUserName())
                .withPublicKey(instanceAuthentication.getPublicKey())
                .withRootVolumeSize(50)
                .withRootVolumeType("GP3")
                .build();
    }

    private Group getWorkerGroup(InstanceAuthentication instanceAuthentication) {
        List<CloudInstance> cloudInstances = new ArrayList<>();
        CloudInstance workerInstance1 = new CloudInstance("i-worker1", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance2 = new CloudInstance("i-worker2", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance3 = new CloudInstance("i-worker3", mock(InstanceTemplate.class), instanceAuthentication, "subnet-1", "az1");
        InstanceTemplate newInstanceTemplate = mock(InstanceTemplate.class);
        lenient().when(newInstanceTemplate.getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        CloudInstance workerInstance4 = new CloudInstance(null, newInstanceTemplate, instanceAuthentication, "subnet-1", "az1");
        CloudInstance workerInstance5 = new CloudInstance(null, newInstanceTemplate, instanceAuthentication, "subnet-1", "az1");
        cloudInstances.add(workerInstance1);
        cloudInstances.add(workerInstance2);
        cloudInstances.add(workerInstance3);
        cloudInstances.add(workerInstance4);
        cloudInstances.add(workerInstance5);
        return Group.builder()
                .withName("worker")
                .withType(InstanceGroupType.CORE)
                .withInstances(cloudInstances)
                .withInstanceAuthentication(instanceAuthentication)
                .withLoginUserName(instanceAuthentication.getLoginUserName())
                .withPublicKey(instanceAuthentication.getPublicKey())
                .withRootVolumeSize(50)
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build();
    }

    private Network getNetwork() {
        Map<String, Object> networkParameters = new HashMap<>();
        networkParameters.put("vpcId", "vpc-12345678");
        networkParameters.put("internetGatewayId", "igw-12345678");
        return new Network(new Subnet(null), networkParameters);
    }
}
