package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptState;
import com.sequenceiq.cloudbreak.cloud.aws.client.AmazonAutoScalingClient;

import software.amazon.awssdk.services.autoscaling.model.AutoScalingGroup;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import software.amazon.awssdk.services.autoscaling.model.DescribeAutoScalingGroupsResponse;
import software.amazon.awssdk.services.autoscaling.model.Instance;

@ExtendWith(MockitoExtension.class)
class AsgInstanceDetachWaiterTest {

    @Mock
    private AmazonAutoScalingClient amazonASClient;

    @Test
    public void testProcessFinish() throws Exception {
        String asGroupName = "master";
        List<String> instanceIdsToDetach = List.of("i1", "i2", "i3", "i4");
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequestArgumentCaptor =
                ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResponse = mock(DescribeAutoScalingGroupsResponse.class);
        when(amazonASClient.describeAutoScalingGroups(describeAutoScalingGroupsRequestArgumentCaptor.capture())).thenReturn(describeAutoScalingGroupsResponse);

        AsgInstanceDetachWaiter asgInstanceDetachWaiter = new AsgInstanceDetachWaiter(amazonASClient, asGroupName, instanceIdsToDetach);
        AttemptResult<Boolean> result = asgInstanceDetachWaiter.process();

        assertEquals(AttemptState.FINISH, result.getState());

        DescribeAutoScalingGroupsRequest request = describeAutoScalingGroupsRequestArgumentCaptor.getValue();
        assertThat(request.autoScalingGroupNames()).containsOnly("master");
    }

    @Test
    public void testProcessJustContinue() throws Exception {
        String asGroupName = "master";
        List<String> instanceIdsToDetach = List.of("i1", "i2", "i3", "i4");
        ArgumentCaptor<DescribeAutoScalingGroupsRequest> describeAutoScalingGroupsRequestArgumentCaptor =
                ArgumentCaptor.forClass(DescribeAutoScalingGroupsRequest.class);
        DescribeAutoScalingGroupsResponse describeAutoScalingGroupsResponse = mock(DescribeAutoScalingGroupsResponse.class);
        Instance instance1 = Instance.builder().instanceId("i1").build();
        Instance instance2 = Instance.builder().instanceId("i2").build();
        AutoScalingGroup asg = AutoScalingGroup.builder().instances(List.of(instance1, instance2)).build();
        when(describeAutoScalingGroupsResponse.autoScalingGroups()).thenReturn(List.of(asg));
        when(amazonASClient.describeAutoScalingGroups(describeAutoScalingGroupsRequestArgumentCaptor.capture())).thenReturn(describeAutoScalingGroupsResponse);

        AsgInstanceDetachWaiter asgInstanceDetachWaiter = new AsgInstanceDetachWaiter(amazonASClient, asGroupName, instanceIdsToDetach);
        AttemptResult<Boolean> result = asgInstanceDetachWaiter.process();
        assertEquals(AttemptState.CONTINUE, result.getState());

        describeAutoScalingGroupsResponse = mock(DescribeAutoScalingGroupsResponse.class);
        asg = AutoScalingGroup.builder().instances(List.of()).build();
        when(describeAutoScalingGroupsResponse.autoScalingGroups()).thenReturn(List.of(asg));
        when(amazonASClient.describeAutoScalingGroups(describeAutoScalingGroupsRequestArgumentCaptor.capture())).thenReturn(describeAutoScalingGroupsResponse);

        result = asgInstanceDetachWaiter.process();
        assertEquals(AttemptState.FINISH, result.getState());

        DescribeAutoScalingGroupsRequest request = describeAutoScalingGroupsRequestArgumentCaptor.getValue();
        assertThat(request.autoScalingGroupNames()).containsOnly("master");
    }
}