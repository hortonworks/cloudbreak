package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.UpdateType;
import com.sequenceiq.cloudbreak.cloud.aws.AwsImageUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.AwsLaunchTemplateUpdateService;
import com.sequenceiq.cloudbreak.cloud.aws.LaunchTemplateField;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
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

    @InjectMocks
    private AwsUpdateService underTest;

    @Test
    void updateCloudFormationTemplateResourceWithImageParameter() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParams(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, Collections.singletonList(cloudResource), UpdateType.IMAGE_UPDATE);

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

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, List.of(cloudResource, launch), UpdateType.IMAGE_UPDATE);

        verify(awsImageUpdateService, times(0)).updateImage(ac, stack, cloudResource);
        assertEquals(0, statuses.size());
    }

    @Test
    void updateRandomResource() {
        CloudResource cloudResource = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.AWS_LAUNCHCONFIGURATION)
                .withParams(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();
        CloudResource cf = CloudResource.builder()
                .withName("cf")
                .withType(ResourceType.CLOUDFORMATION_STACK)
                .withParams(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();

        List<CloudResourceStatus> statuses = underTest.update(ac, stack, List.of(cloudResource, cf), UpdateType.IMAGE_UPDATE);

        verify(awsImageUpdateService, times(0)).updateImage(ac, stack, cloudResource);
        assertEquals(1, statuses.size());
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
                .withParams(Collections.singletonMap(CloudResource.IMAGE, "dummy"))
                .build();

        underTest.updateUserData(ac, stack, List.of(cf), userData);

        String encodedCoreUserData = Base64.getEncoder().encodeToString(userData.get(InstanceGroupType.CORE).getBytes());
        Map<LaunchTemplateField, String> coreFields = Map.of(LaunchTemplateField.USER_DATA, encodedCoreUserData);
        verify(awsLaunchTemplateUpdateService).updateLaunchTemplate(coreFields, ac, cf.getName(), coreGroup);
        String encodedGatewayUserData = Base64.getEncoder().encodeToString(userData.get(InstanceGroupType.GATEWAY).getBytes());
        Map<LaunchTemplateField, String> gatewayFields = Map.of(LaunchTemplateField.USER_DATA, encodedGatewayUserData);
        verify(awsLaunchTemplateUpdateService).updateLaunchTemplate(gatewayFields, ac, cf.getName(), gatewayGroup);
    }
}
