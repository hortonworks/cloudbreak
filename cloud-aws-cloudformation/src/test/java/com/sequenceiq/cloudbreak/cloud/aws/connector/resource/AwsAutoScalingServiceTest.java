package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.core.waiters.Waiter;
import software.amazon.awssdk.services.autoscaling.model.Activity;
import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeScalingActivitiesResponse;

@ExtendWith(MockitoExtension.class)
public class AwsAutoScalingServiceTest {

    @InjectMocks
    private AwsAutoScalingService underTest;

    @Mock
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @Mock
    private AmazonAutoScalingClient amazonAutoScalingClient;

    @Mock
    private Waiter<DescribeScalingActivitiesResponse> describeScalingActivitiesRequestWaiter;

    @Test
    public void testCheckLastScalingActivityWhenActivitiesFailed() {
        DescribeScalingActivitiesResponse response = DescribeScalingActivitiesResponse.builder()
                .activities(Activity.builder()
                        .statusMessage("Status")
                        .description("Description")
                        .cause("Cause")
                        .statusCode("FAILED")
                        .build())
                .build();
        Group group = createGroup("master", InstanceGroupType.GATEWAY, List.of(new CloudInstance("anId", null, null, "subnet-1", "az1")));
        when(amazonAutoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(response);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any())).thenReturn(describeScalingActivitiesRequestWaiter);
        mockDescribeAutoscalingGroup();

        Date date = new Date();
        AmazonAutoscalingFailedException expected = assertThrows(AmazonAutoscalingFailedException.class, () ->
                underTest.checkLastScalingActivity(amazonAutoScalingClient, "asGroup", date, group));

        assertEquals(expected.getMessage(), "Description Cause");
    }

    @Test
    public void testCheckLastScalingActivityWhenActivitiesFailedWithInsufficientInstanceCapacity() throws AmazonAutoscalingFailedException {
        DescribeScalingActivitiesResponse response = DescribeScalingActivitiesResponse.builder()
                .activities(Activity.builder()
                        .statusMessage("Status InsufficientInstanceCapacity blahblah")
                        .description("Description")
                        .cause("Cause")
                        .statusCode("FAILED")
                        .build())
                .build();
        Group group = createGroup("master", InstanceGroupType.GATEWAY, List.of(new CloudInstance("anId", null, null, "subnet-1", "az1")));
        when(amazonAutoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(response);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any())).thenReturn(describeScalingActivitiesRequestWaiter);
        mockDescribeAutoscalingGroup();

        Date date = new Date();
        underTest.checkLastScalingActivity(amazonAutoScalingClient, "asGroup", date, group);
    }

    @Test
    public void testCheckLastScalingActivityWhenActivitiesSuccessThenNoException() throws AmazonAutoscalingFailedException {
        DescribeScalingActivitiesResponse response = DescribeScalingActivitiesResponse.builder()
                .activities(Activity.builder()
                        .statusMessage("Status")
                        .description("Description")
                        .cause("Cause")
                        .statusCode("success")
                        .build())
                .build();
        Group group = createGroup("master", InstanceGroupType.GATEWAY, List.of(new CloudInstance("anId", null, null, "subnet-1", "az1")));
        when(amazonAutoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(response);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any())).thenReturn(describeScalingActivitiesRequestWaiter);
        mockDescribeAutoscalingGroup();

        Date date = new Date();
        underTest.checkLastScalingActivity(amazonAutoScalingClient, "asGroup", date, group);

    }

    @Test
    public void testCheckLastScalingActivityShouldNotCreateWaiterWhenGroupDoesNotHaveAnyInstance() throws AmazonAutoscalingFailedException {
        Group group = createGroup("gateway", InstanceGroupType.CORE, List.of());

        Date date = new Date();
        underTest.checkLastScalingActivity(amazonAutoScalingClient, "asGroup", date, group);

        verify(amazonAutoScalingClient, times(0)).describeScalingActivities(any(DescribeScalingActivitiesRequest.class));
        verify(customAmazonWaiterProvider, times(0)).getAutoscalingActivitiesWaiter(any());
        verifyNoMoreInteractions(amazonAutoScalingClient);
        verifyNoMoreInteractions(customAmazonWaiterProvider);

    }

    private void mockDescribeAutoscalingGroup() {
        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResult = DescribeAutoScalingGroupsResponse.builder()
                .autoScalingGroups(AutoScalingGroup.builder().autoScalingGroupName("asGroup").build()).build();
        when(amazonAutoScalingClient.describeAutoScalingGroups(any()))
                .thenReturn(describeAutoScalingGroupsResult);
    }

    private Group createGroup(String groupName, InstanceGroupType groupType, List<CloudInstance> instances) {
        return Group.builder()
                .withName(groupName)
                .withType(groupType)
                .withInstances(instances)
                .withNetwork(createGroupNetwork())
                .withRootVolumeType(AwsDiskType.Gp3.value())
                .build();
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }
}
