package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsImageUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchConfigurationUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchTemplateUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.LaunchTemplateField;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

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

        Assertions.assertThrows(IllegalStateException.class, () -> underTest.updateUserData(ac, stack, List.of(cf), userDataMap));
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

        String encodedCoreUserData = Base64.getEncoder().encodeToString(userData.get(InstanceGroupType.CORE).getBytes());
        Map<LaunchTemplateField, String> coreFields = Map.of(LaunchTemplateField.USER_DATA, encodedCoreUserData);
        verify(awsLaunchTemplateUpdateService).updateLaunchTemplate(coreFields, ac, cf.getName(), coreGroup, stack);
        String encodedGatewayUserData = Base64.getEncoder().encodeToString(userData.get(InstanceGroupType.GATEWAY).getBytes());
        Map<LaunchTemplateField, String> gatewayFields = Map.of(LaunchTemplateField.USER_DATA, encodedGatewayUserData);
        verify(awsLaunchTemplateUpdateService).updateLaunchTemplate(gatewayFields, ac, cf.getName(), gatewayGroup, stack);
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
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
        when(gatewayGroup.getReferenceInstanceConfiguration()).thenReturn(cloudInstance);
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchTemplate");


        underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.VERTICAL_SCALE, Optional.of("gwGroup"));

        Map<LaunchTemplateField, String> updatableFields = Map.of(
                LaunchTemplateField.INSTANCE_TYPE,
                "m42.xxxl",
                LaunchTemplateField.ROOT_DISK,
                "100");
        verify(awsLaunchTemplateUpdateService, times(1)).updateLaunchTemplate(eq(updatableFields), any(), eq("cf"), eq(gatewayGroup), eq(stack));
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
                LaunchTemplateField.ROOT_DISK,
                "100");
        verify(awsLaunchTemplateUpdateService, times(0)).updateLaunchTemplate(eq(updatableFields), any(), eq("cf"), eq(gatewayGroup), eq(stack));
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
        CloudInstance cloudInstance = mock(CloudInstance.class);
        when(cloudInstance.getTemplate()).thenReturn(instanceTemplate);
        when(gatewayGroup.getReferenceInstanceConfiguration()).thenReturn(cloudInstance);
        when(stack.getGroups()).thenReturn(List.of(gatewayGroup));
        when(stack.getTemplate()).thenReturn("AWS::EC2::LaunchConfiguration");


        underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.VERTICAL_SCALE, Optional.of("gwGroup"));

        Map<LaunchTemplateField, String> updatableFields = Map.of(
                LaunchTemplateField.INSTANCE_TYPE,
                "m42.xxxl",
                LaunchTemplateField.ROOT_DISK,
                "100");
        verify(launchConfigurationUpdateService, times(1))
                .updateLaunchConfigurations(any(), eq(stack), eq(cloudResource), eq(updatableFields), eq(gatewayGroup));
    }
}
