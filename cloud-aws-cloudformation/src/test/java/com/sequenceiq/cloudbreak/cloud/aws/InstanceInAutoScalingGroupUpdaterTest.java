package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;

class InstanceInAutoScalingGroupUpdaterTest {

    private static final String DESIRED_FLAVOR = "desiredFlavor";

    private InstanceInAutoScalingGroupUpdater underTest = new InstanceInAutoScalingGroupUpdater();

    @Test
    void testUpdateInstanceInAutoscalingGroup() {
        AmazonEc2Client ec2Client = mock(AmazonEc2Client.class);
        AutoScalingGroup autoScalingGroup = mock(AutoScalingGroup.class);
        Instance instance1 = mock(Instance.class);
        when(instance1.instanceType()).thenReturn(DESIRED_FLAVOR);
        Instance instance2 = mock(Instance.class);
        when(instance2.instanceType()).thenReturn("dummy");
        when(instance2.instanceId()).thenReturn("instanceId2");
        when(autoScalingGroup.instances()).thenReturn(List.of(instance1, instance2));
        Group group = mock(Group.class);
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getFlavor()).thenReturn(DESIRED_FLAVOR);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);

        underTest.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group);

        ArgumentCaptor<ModifyInstanceAttributeRequest> captor = ArgumentCaptor.forClass(ModifyInstanceAttributeRequest.class);
        verify(ec2Client).modifyInstanceAttribute(captor.capture());
        ModifyInstanceAttributeRequest request = captor.getValue();
        assertEquals(request.instanceId(), "instanceId2");
        assertEquals(request.instanceType().value(), DESIRED_FLAVOR);
    }
}