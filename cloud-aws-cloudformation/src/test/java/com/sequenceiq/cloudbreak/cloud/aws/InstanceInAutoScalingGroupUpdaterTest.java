package com.sequenceiq.cloudbreak.cloud.aws;

import static com.sequenceiq.cloudbreak.cloud.aws.common.AwsSdkErrorCodes.INSTANCE_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.Instance;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Ec2Exception;
import software.amazon.awssdk.services.ec2.model.HttpTokensState;
import software.amazon.awssdk.services.ec2.model.InstanceMetadataOptionsResponse;
import software.amazon.awssdk.services.ec2.model.ModifyInstanceAttributeRequest;
import software.amazon.awssdk.services.ec2.model.Reservation;

@ExtendWith(MockitoExtension.class)
class InstanceInAutoScalingGroupUpdaterTest {

    private static final String DESIRED_FLAVOR = "desiredFlavor";

    @Mock
    private AutoScalingGroup autoScalingGroup;

    @Mock
    private AmazonEc2Client ec2Client;

    @Mock
    private Group group;

    @InjectMocks
    private InstanceInAutoScalingGroupUpdater underTest;

    @BeforeEach
    void setUp() {
        InstanceTemplate instanceTemplate = mock(InstanceTemplate.class);
        when(instanceTemplate.getFlavor()).thenReturn(DESIRED_FLAVOR);
        when(group.getReferenceInstanceTemplate()).thenReturn(instanceTemplate);
    }

    @Test
    void testUpdateInstanceInAutoscalingGroupWhenTheASGIsEmpty() {
        when(autoScalingGroup.instances()).thenReturn(List.of());

        assertDoesNotThrow(() -> underTest.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group, Optional.empty()));

        verifyNoInteractions(ec2Client);
    }

    @Test
    void testUpdateInstanceInAutoscalingGroupWhenTheASGContainsInstancesButDescribeInstancesCouldNotFoundIt() {
        when(autoScalingGroup.instances()).thenReturn(List.of());
        when(autoScalingGroup.autoScalingGroupName()).thenReturn("asg-name");
        Instance instance1 = mock(Instance.class);
        when(instance1.instanceId()).thenReturn("instanceId1");
        Instance instance2 = mock(Instance.class);
        when(instance2.instanceId()).thenReturn("instanceId2");
        when(autoScalingGroup.instances()).thenReturn(List.of(instance1, instance2));
        AwsServiceException ec2Exception = Ec2Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder().errorCode(INSTANCE_NOT_FOUND).build())
                .build();
        when(ec2Client.describeInstances(any())).thenThrow(ec2Exception);

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group, Optional.empty()),
                "Some of the instances in the auto scaling group('asg-name') does not exist on EC2: ");

        verify(ec2Client, times(1)).describeInstances(any());
    }

    @Test
void testUpdateInstanceInAutoscalingGroupWhenTheASGContainsInstancesButDescribeInstancesFailsWithSdkClientException() {
        when(autoScalingGroup.instances()).thenReturn(List.of());
        when(autoScalingGroup.autoScalingGroupName()).thenReturn("asg-name");
        Instance instance1 = mock(Instance.class);
        when(instance1.instanceId()).thenReturn("instanceId1");
        Instance instance2 = mock(Instance.class);
        when(instance2.instanceId()).thenReturn("instanceId2");
        when(autoScalingGroup.instances()).thenReturn(List.of(instance1, instance2));
        when(ec2Client.describeInstances(any())).thenThrow(SdkClientException.builder().build());

        assertThrows(CloudbreakServiceException.class,
                () -> underTest.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group, Optional.empty()),
                "AWS EC2 could not be contacted for a response during describing instances for auto scaling group('asg-name')");

        verify(ec2Client, times(1)).describeInstances(any());
    }

    @Test
    void testUpdateInstanceInAutoscalingGroupWhenTheASGAndDescribeInstancesContainsTheSameInstancesAndTypeUpgradeIsNecessary() {
        Instance instance1 = mock(Instance.class);
        when(instance1.instanceId()).thenReturn("instanceId1");
        Instance instance2 = mock(Instance.class);
        when(instance2.instanceId()).thenReturn("instanceId2");
        when(autoScalingGroup.instances()).thenReturn(List.of(instance1, instance2));
        when(ec2Client.describeInstances(any())).thenReturn(DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(List.of(software.amazon.awssdk.services.ec2.model.Instance.builder().instanceType(DESIRED_FLAVOR).build(),
                                software.amazon.awssdk.services.ec2.model.Instance.builder().instanceType("dummy").instanceId("instanceId2").build()))
                        .build())
                .build());

        assertDoesNotThrow(() -> underTest.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group, Optional.empty()));

        verify(ec2Client, times(1)).describeInstances(any());
        ArgumentCaptor<ModifyInstanceAttributeRequest> captor = ArgumentCaptor.forClass(ModifyInstanceAttributeRequest.class);
        verify(ec2Client, times(1)).modifyInstanceAttribute(captor.capture());
        verify(ec2Client, times(0)).modifyInstanceMetadataOptions(any());
        ModifyInstanceAttributeRequest request = captor.getValue();
        assertEquals(request.instanceId(), "instanceId2");
        assertEquals(request.instanceType().value(), DESIRED_FLAVOR);
    }

    @Test
    void testUpdateInstanceInAutoscalingGroupWhenTheASGAndDescribeInstancesContainsTheSameInstancesWithTheDesiredInstanceType() {
        Instance instance1 = mock(Instance.class);
        when(instance1.instanceId()).thenReturn("instanceId1");
        Instance instance2 = mock(Instance.class);
        when(instance2.instanceId()).thenReturn("instanceId2");
        when(autoScalingGroup.instances()).thenReturn(List.of(instance1, instance2));
        when(ec2Client.describeInstances(any())).thenReturn(DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(List.of(software.amazon.awssdk.services.ec2.model.Instance.builder().instanceType(DESIRED_FLAVOR).build(),
                                software.amazon.awssdk.services.ec2.model.Instance.builder().instanceType(DESIRED_FLAVOR).build()))
                        .build())
                .build());

        assertDoesNotThrow(() -> underTest.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group, Optional.empty()));

        verify(ec2Client, times(1)).describeInstances(any());
        verify(ec2Client, times(0)).modifyInstanceMetadataOptions(any());
        verifyNoMoreInteractions(ec2Client);
    }

    @Test
    void testUpdateInstanceWhenImdsOptionIsTheSame() {
        setupImdsMock();

        assertDoesNotThrow(() -> underTest.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group, Optional.of(HttpTokensState.REQUIRED)));

        verify(ec2Client, times(1)).describeInstances(any());
        verifyNoMoreInteractions(ec2Client);
    }

    @Test
    void testUpdateInstanceWhenImdsOptionIsUpdated() {
        setupImdsMock();

        assertDoesNotThrow(() -> underTest.updateInstanceInAutoscalingGroup(ec2Client, autoScalingGroup, group, Optional.of(HttpTokensState.OPTIONAL)));

        verify(ec2Client, times(1)).describeInstances(any());
        verify(ec2Client, times(2)).modifyInstanceMetadataOptions(any());
        verifyNoMoreInteractions(ec2Client);
    }

    private void setupImdsMock() {
        Instance instance1 = mock(Instance.class);
        when(instance1.instanceId()).thenReturn("instanceId1");
        Instance instance2 = mock(Instance.class);
        when(instance2.instanceId()).thenReturn("instanceId2");
        when(autoScalingGroup.instances()).thenReturn(List.of(instance1, instance2));
        when(ec2Client.describeInstances(any())).thenReturn(DescribeInstancesResponse.builder()
                .reservations(Reservation.builder()
                        .instances(List.of(software.amazon.awssdk.services.ec2.model.Instance.builder().instanceType(DESIRED_FLAVOR)
                                        .metadataOptions(InstanceMetadataOptionsResponse.builder().httpTokens(HttpTokensState.REQUIRED).build()).build(),
                                software.amazon.awssdk.services.ec2.model.Instance.builder().instanceType(DESIRED_FLAVOR)
                                        .metadataOptions(InstanceMetadataOptionsResponse.builder().httpTokens(HttpTokensState.REQUIRED).build()).build()))
                        .build())
                .build());
    }
}