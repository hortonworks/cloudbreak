package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.amazonaws.services.autoscaling.model.Activity;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesRequest;
import com.amazonaws.services.autoscaling.model.DescribeScalingActivitiesResult;
import com.amazonaws.waiters.Waiter;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;
import com.sequenceiq.cloudbreak.cloud.aws.scheduler.CustomAmazonWaiterProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@ExtendWith(MockitoExtension.class)
public class AwsAutoScalingServiceTest {

    @InjectMocks
    private AwsAutoScalingService underTest;

    @Mock
    private CustomAmazonWaiterProvider customAmazonWaiterProvider;

    @Mock
    private AmazonAutoScalingClient amazonAutoScalingClient;

    @Mock
    private Waiter<DescribeScalingActivitiesRequest> describeScalingActivitiesRequestWaiter;

    @Test
    public void testCheckLastScalingActivityWhenActivitiesFailed() {
        DescribeScalingActivitiesResult result = new DescribeScalingActivitiesResult();
        Activity activity1 = new Activity();
        activity1.setStatusMessage("Status");
        activity1.setDescription("Description");
        activity1.setCause("Cause");
        activity1.setStatusCode("FAILED");
        result.setActivities(List.of(activity1));
        Group group = createGroup("master", InstanceGroupType.GATEWAY, List.of(new CloudInstance("anId", null, null, "subnet-1", "az1")));
        when(amazonAutoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(result);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any(), any())).thenReturn(describeScalingActivitiesRequestWaiter);
        mockDescribeAutoscalingGroup();

        Date date = new Date();
        AmazonAutoscalingFailed expected = Assertions.assertThrows(AmazonAutoscalingFailed.class, () ->
                underTest.checkLastScalingActivity(amazonAutoScalingClient, "asGroup", date, group));

        Assertions.assertEquals(expected.getMessage(), "Description Cause");
    }

    @Test
    public void testCheckLastScalingActivityWhenActivitiesFailedWithInsufficientInstanceCapacity() throws AmazonAutoscalingFailed {
        DescribeScalingActivitiesResult result = new DescribeScalingActivitiesResult();
        Activity activity1 = new Activity();
        activity1.setStatusMessage("Status InsufficientInstanceCapacity blahblah");
        activity1.setDescription("Description");
        activity1.setCause("Cause");
        activity1.setStatusCode("FAILED");
        result.setActivities(List.of(activity1));
        Group group = createGroup("master", InstanceGroupType.GATEWAY, List.of(new CloudInstance("anId", null, null, "subnet-1", "az1")));
        when(amazonAutoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(result);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any(), any())).thenReturn(describeScalingActivitiesRequestWaiter);
        mockDescribeAutoscalingGroup();

        Date date = new Date();
        underTest.checkLastScalingActivity(amazonAutoScalingClient, "asGroup", date, group);
    }

    @Test
    public void testCheckLastScalingActivityWhenActivitiesSuccessThenNoException() throws AmazonAutoscalingFailed {
        DescribeScalingActivitiesResult result = new DescribeScalingActivitiesResult();
        Activity activity1 = new Activity();
        activity1.setStatusMessage("Status");
        activity1.setDescription("Description");
        activity1.setCause("Cause");
        activity1.setStatusCode("success");
        result.setActivities(List.of(activity1));
        Group group = createGroup("master", InstanceGroupType.GATEWAY, List.of(new CloudInstance("anId", null, null, "subnet-1", "az1")));
        when(amazonAutoScalingClient.describeScalingActivities(any(DescribeScalingActivitiesRequest.class))).thenReturn(result);
        when(customAmazonWaiterProvider.getAutoscalingActivitiesWaiter(any(), any())).thenReturn(describeScalingActivitiesRequestWaiter);
        mockDescribeAutoscalingGroup();

        Date date = new Date();
        underTest.checkLastScalingActivity(amazonAutoScalingClient, "asGroup", date, group);

    }

    @Test
    public void testCheckLastScalingActivityShouldNotCreateWaiterWhenGroupDoesNotHaveAnyInstance() throws AmazonAutoscalingFailed {
        DescribeScalingActivitiesResult result = new DescribeScalingActivitiesResult();
        Activity activity1 = new Activity();
        activity1.setStatusMessage("Status");
        activity1.setDescription("Description");
        activity1.setCause("Cause");
        activity1.setStatusCode("success");
        result.setActivities(List.of(activity1));
        Group group = createGroup("gateway", InstanceGroupType.CORE, List.of());

        Date date = new Date();
        underTest.checkLastScalingActivity(amazonAutoScalingClient, "asGroup", date, group);

        verify(amazonAutoScalingClient, times(0)).describeScalingActivities(any(DescribeScalingActivitiesRequest.class));
        verify(customAmazonWaiterProvider, times(0)).getAutoscalingActivitiesWaiter(any(), any());
        verifyNoMoreInteractions(amazonAutoScalingClient);
        verifyNoMoreInteractions(customAmazonWaiterProvider);

    }

    private void mockDescribeAutoscalingGroup() {
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = new DescribeAutoScalingGroupsResult();
        ArrayList<AutoScalingGroup> autoScalingGroups = new ArrayList<>();
        AutoScalingGroup autoScalingGroup = new AutoScalingGroup();
        autoScalingGroup.setAutoScalingGroupName("asGroup");
        autoScalingGroups.add(autoScalingGroup);
        describeAutoScalingGroupsResult.setAutoScalingGroups(autoScalingGroups);
        when(amazonAutoScalingClient.describeAutoScalingGroups(any()))
                .thenReturn(describeAutoScalingGroupsResult);
    }

    private Group createGroup(String groupName, InstanceGroupType groupType, List<CloudInstance> instances) {
        Group group = new Group(groupName, groupType, instances, null, null, null, null, null, 0, null, createGroupNetwork());
        return group;
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }
}
