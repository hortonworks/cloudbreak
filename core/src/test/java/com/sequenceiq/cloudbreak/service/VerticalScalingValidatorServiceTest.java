package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToExtendedCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.verticalscale.VerticalScaleInstanceProvider;

@ExtendWith(MockitoExtension.class)
public class VerticalScalingValidatorServiceTest {

    @Mock
    private CloudParameterService cloudParameterService;

    @Mock
    private CredentialToExtendedCloudCredentialConverter credentialToExtendedCloudCredentialConverter;

    @Mock
    private CredentialClientService credentialClientService;

    @Mock
    private VerticalScaleInstanceProvider verticalScaleInstanceProvider;

    @InjectMocks
    private VerticalScalingValidatorService underTest;

    @Mock
    private Stack stack;

    @Test
    public void testRequestValidateInstanceTypeForDeleteVolumesSuccess() {
        String instanceGroupNameInStack = "master1";
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(1);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack, template)));

        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup(instanceGroupNameInRequest);

        underTest.validateInstanceTypeForDeletingDisks(stack, stackDeleteVolumesRequest);
    }

    @Test
    public void testInstanceTypeForDeleteVolumesBadRequestNoEphemeralVolume() {
        String instanceGroupNameInStack = "master1";
        String instanceGroupNameInRequest = "master1";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack, template)));

        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateInstanceTypeForDeletingDisks(stack, stackDeleteVolumesRequest));
        assertEquals("Deleting disks is only supported on instances with instance storage", badRequestException.getMessage());
    }

    @Test
    public void testInstanceTypeForDeleteVolumesBadRequestInvalidInstanceGroup() {
        String instanceGroupNameInStack = "master1";
        String instanceGroupNameInRequest = "compute";
        String instanceTypeNameInStack = "m3.xlarge";
        String instanceTypeNameInRequest = "m3.xlarge";

        Template template = new Template();
        template.setInstanceStorageCount(0);

        when(stack.getInstanceGroups()).thenReturn(Set.of(instanceGroup(instanceGroupNameInStack, instanceTypeNameInStack, template)));

        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup(instanceGroupNameInRequest);

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateInstanceTypeForDeletingDisks(stack, stackDeleteVolumesRequest));
        assertEquals("Define a group which exists in Cluster. It can be [master1].", badRequestException.getMessage());
    }

    private InstanceGroup instanceGroup(String name, String instanceType, Template template) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(name);
        template.setInstanceType(instanceType);
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }
}
